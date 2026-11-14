package com.example.data

import com.example.domain.ApplianceEntry
import com.example.domain.AuthOtpEngine
import com.example.domain.ChannelId
import com.example.domain.ConnectionSpec
import com.example.domain.OwnersSpec
import com.example.domain.OtpChallenge
import com.example.domain.OtpVerifyResult
import com.example.domain.PlanId
import com.example.domain.ProfileDraft
import com.example.domain.RoofSpec
import com.example.domain.Sector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class EcoRepository(
    private val preferences: EcoPreferences,
    private val db: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val challenges = mutableMapOf<String, OtpChallenge>()
    private val lockedUntilMap = mutableMapOf<String, Long>()

    val ecoState: Flow<EcoState> = preferences.stateFlow
    val billRevisions: Flow<List<BillRevisionEntity>> = db.billRevisionDao().observeAll()
    val consents: Flow<List<ConsentEntity>> = db.consentDao().observeAll()
    val auditEvents: Flow<List<AuditEventEntity>> = db.auditEventDao().observeRecent(10)
    val assistantMessages: Flow<List<AssistantMessageEntity>> = db.assistantMessageDao().observeAll()
    val plantReports: Flow<List<PlantReportEntity>> = db.plantReportDao().observeAll()

    fun observeProfileDraft(phone: String): Flow<ProfileDraft> =
        db.profileDraftDao().observeByPhone(phone).map { entity ->
            if (entity == null) {
                ProfileDraft()
            } else {
                val appliances = runCatching {
                    json.decodeFromString<List<ApplianceEntry>>(entity.appliancesJson)
                }.getOrDefault(emptyList())
                ProfileDraft(
                    step = entity.step,
                    sector = Sector.fromRaw(entity.sectorRaw),
                    city = entity.city,
                    connection = ConnectionSpec(
                        meterSerial = entity.meterSerial,
                        tariffRaw = entity.tariffRaw,
                        demandKw = entity.demandKw,
                        bidirectional = entity.bidirectional
                    ),
                    appliances = appliances,
                    roof = RoofSpec(
                        area = entity.roofArea,
                        tilt = entity.roofTilt,
                        azimuth = entity.roofAzimuth,
                        shadowPct = entity.roofShadowPct
                    ),
                    sharedOwnership = entity.sharedOwnership,
                    owners = OwnersSpec(entity.ownersTotal, entity.ownersGranted),
                    updatedAt = entity.updatedAt,
                    completedAt = entity.completedAt
                )
            }
        }

    suspend fun requestOtp(rawPhone: String): Result<Pair<String, Int>> {
        val phone = AuthOtpEngine.normalizePhone(rawPhone)
        if (!AuthOtpEngine.isValidIranMobile(phone)) {
            return Result.failure(IllegalArgumentException("شماره موبایل باید ۱۱ رقم و با ۰۹ شروع شود"))
        }
        val now = System.currentTimeMillis()
        val lockedUntil = lockedUntilMap[phone]
        if (lockedUntil != null && lockedUntil > now) {
            return Result.failure(IllegalStateException("تلاش‌های ناموفق زیاد بود؛ چند دقیقه بعد دوباره تلاش کنید"))
        }
        val existing = challenges[phone]
        val wait = AuthOtpEngine.resendWaitSeconds(existing, now)
        if (wait > 0 && existing?.consumedAt == null) {
            return Result.failure(IllegalStateException("برای ارسال مجدد $wait ثانیه صبر کنید"))
        }
        val code = AuthOtpEngine.generateOtp()
        val salt = AuthOtpEngine.randomSalt()
        val challenge = OtpChallenge(
            phone = phone,
            codeHash = AuthOtpEngine.hashCode(code, salt),
            salt = salt,
            createdAt = now,
            expiresAt = now + AuthOtpEngine.OTP_TTL_SECONDS * 1000L,
            attempts = 0,
            consumedAt = null
        )
        challenges[phone] = challenge
        preferences.update { it.copy(phone = phone) }
        recordAudit(phone, "OtpRequested")
        return Result.success(code to AuthOtpEngine.RESEND_COOLDOWN_SECONDS)
    }

    suspend fun verifyOtp(rawPhone: String, code: String, acceptedTerms: Boolean): OtpVerifyResult {
        val phone = AuthOtpEngine.normalizePhone(rawPhone)
        if (!AuthOtpEngine.isValidIranMobile(phone)) {
            return OtpVerifyResult.Failure("شماره موبایل معتبر نیست")
        }
        if (!acceptedTerms) {
            return OtpVerifyResult.Failure("پذیرش شرایط استفاده الزامی است")
        }
        val now = System.currentTimeMillis()
        val challenge = challenges[phone]
        val result = AuthOtpEngine.verifyChallenge(challenge, code, now, lockedUntilMap[phone])
        when (result) {
            is OtpVerifyResult.Success -> {
                if (challenge != null) {
                    challenges[phone] = challenge.copy(consumedAt = now)
                }
                preferences.update { it.copy(loggedIn = true, phone = phone) }
                recordAudit(phone, "TermsAccepted")
                recordAudit(phone, "SessionStarted")
            }
            is OtpVerifyResult.Failure -> {
                if (challenge != null) {
                    val nextAttempts = challenge.attempts + 1
                    challenges[phone] = challenge.copy(attempts = nextAttempts)
                    if (nextAttempts >= AuthOtpEngine.MAX_VERIFY_ATTEMPTS) {
                        lockedUntilMap[phone] = now + AuthOtpEngine.LOCK_SECONDS * 1000L
                    }
                }
                recordAudit(phone, "OtpFailed")
            }
        }
        return result
    }

    suspend fun logout(phone: String) {
        recordAudit(phone, "SessionEnded")
        preferences.reset()
    }

    suspend fun updateEcoState(transform: (EcoState) -> EcoState) {
        preferences.update(transform)
    }

    suspend fun confirmBillAndAppendRevision(
        phone: String,
        bill: BillData,
        source: String,
        confidence: String,
        issuesText: String,
        dirty: Boolean
    ): Int {
        val revisionNum = db.billRevisionDao().insert(
            BillRevisionEntity(
                phone = phone,
                period = bill.period,
                peakKwh = bill.peak,
                midKwh = bill.mid,
                lowKwh = bill.low,
                amountRial = bill.total,
                source = source,
                confidence = confidence,
                issuesText = issuesText,
                confirmed = true,
                createdAt = System.currentTimeMillis()
            )
        ).toInt()
        preferences.update { s ->
            s.copy(
                bill = bill,
                billRevision = if (dirty) revisionNum else s.billRevision.coerceAtLeast(revisionNum),
                lowConfidenceReviewed = true
            )
        }
        return revisionNum
    }

    suspend fun appendBillRevision(
        phone: String,
        bill: BillData,
        source: String = "manual"
    ): Int {
        return db.billRevisionDao().insert(
            BillRevisionEntity(
                phone = phone,
                period = bill.period,
                peakKwh = bill.peak,
                midKwh = bill.mid,
                lowKwh = bill.low,
                amountRial = bill.total,
                source = source,
                confidence = "high",
                issuesText = "",
                confirmed = true,
                createdAt = System.currentTimeMillis()
            )
        ).toInt()
    }

    suspend fun applyRevisionAsBasis(revision: BillRevisionEntity, currentBillId: String) {
        preferences.update { s ->
            s.copy(
                bill = BillData(
                    id = currentBillId,
                    period = revision.period,
                    peak = revision.peakKwh,
                    mid = revision.midKwh,
                    low = revision.lowKwh,
                    total = revision.amountRial
                ),
                billRevision = revision.version
            )
        }
    }

    suspend fun saveProfileDraft(phone: String, draft: ProfileDraft, completedAt: Long? = null): Long {
        val now = System.currentTimeMillis()
        db.profileDraftDao().upsert(
            ProfileDraftEntity(
                phone = phone,
                step = draft.step,
                sectorRaw = draft.sector?.raw,
                city = draft.city,
                meterSerial = draft.connection.meterSerial,
                tariffRaw = draft.connection.tariffRaw,
                demandKw = draft.connection.demandKw,
                bidirectional = draft.connection.bidirectional,
                appliancesJson = json.encodeToString(draft.appliances),
                roofArea = draft.roof.area,
                roofTilt = draft.roof.tilt,
                roofAzimuth = draft.roof.azimuth,
                roofShadowPct = draft.roof.shadowPct,
                sharedOwnership = draft.sharedOwnership,
                ownersTotal = draft.owners.total,
                ownersGranted = draft.owners.granted,
                updatedAt = now,
                completedAt = completedAt ?: draft.completedAt
            )
        )
        return now
    }

    suspend fun grantConsent(phone: String) {
        val now = System.currentTimeMillis()
        db.consentDao().upsert(
            ConsentEntity(
                id = UUID.randomUUID().toString(),
                phone = phone,
                partner = "شرکت توزیع برق",
                scope = "bill_data",
                documentVersion = "1.2",
                grantedAt = now,
                revokedAt = null
            )
        )
        recordAudit(phone, "ConsentGranted")
    }

    suspend fun revokeConsent(phone: String, consentId: String) {
        db.consentDao().revoke(consentId, System.currentTimeMillis())
        recordAudit(phone, "ConsentRevoked")
    }

    suspend fun recordAudit(phone: String, action: String) {
        db.auditEventDao().insert(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                phone = phone,
                action = action,
                at = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveAssistantMessage(id: String, role: String, content: String, at: Long = System.currentTimeMillis()) {
        db.assistantMessageDao().upsert(AssistantMessageEntity(id, role, content, at))
    }

    suspend fun clearAssistantMessages() {
        db.assistantMessageDao().clearAll()
    }

    suspend fun submitPlantReport(monthKey: String, text: String): String {
        val id = "MR-${System.currentTimeMillis().toString().takeLast(6)}"
        db.plantReportDao().insert(
            PlantReportEntity(
                id = id,
                monthKey = monthKey,
                text = text.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
        return id
    }
}
