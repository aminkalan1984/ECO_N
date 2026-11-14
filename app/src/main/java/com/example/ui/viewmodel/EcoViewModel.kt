package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuditEventEntity
import com.example.data.BillData
import com.example.data.BillRevisionEntity
import com.example.data.ConsentEntity
import com.example.data.EcoRepository
import com.example.data.EcoState
import com.example.data.LoanSpec
import com.example.data.PlantReportEntity
import com.example.domain.AuthOtpEngine
import com.example.domain.ChannelId
import com.example.domain.OtpVerifyResult
import com.example.domain.PlanId
import com.example.domain.ProfileDraft
import com.example.domain.RateTables
import com.example.domain.TelemetryCalc
import com.example.domain.TelemetrySnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OtpUiState(
    val phoneInput: String = "09133457781",
    val codeInput: String = "",
    val stepIsCode: Boolean = false,
    val acceptedTerms: Boolean = true,
    val resendCountdown: Int = 0,
    val devPreviewCode: String? = null,
    val busy: Boolean = false,
    val error: String? = null
)

class EcoViewModel(
    private val repository: EcoRepository
) : ViewModel() {

    val ecoState: StateFlow<EcoState> = repository.ecoState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EcoState()
    )

    val billRevisions: StateFlow<List<BillRevisionEntity>> = repository.billRevisions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val consents: StateFlow<List<ConsentEntity>> = repository.consents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val auditEvents: StateFlow<List<AuditEventEntity>> = repository.auditEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val plantReports: StateFlow<List<PlantReportEntity>> = repository.plantReports.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val persistedProfileDraft: StateFlow<ProfileDraft> = ecoState
        .map { it.phone }
        .distinctUntilChanged()
        .flatMapLatest { phone -> repository.observeProfileDraft(phone) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileDraft()
        )

    private val _wizardDraft = MutableStateFlow(ProfileDraft())
    val wizardDraft: StateFlow<ProfileDraft> = _wizardDraft.asStateFlow()
    private var wizardHydratedForPhone: String? = null
    private var wizardSaveJob: Job? = null

    private val _wizardSaveStatus = MutableStateFlow<String?>(null)
    val wizardSaveStatus: StateFlow<String?> = _wizardSaveStatus.asStateFlow()

    private val _otpUi = MutableStateFlow(OtpUiState())
    val otpUi: StateFlow<OtpUiState> = _otpUi.asStateFlow()
    private var countdownJob: Job? = null

    private val _telemetry = MutableStateFlow(TelemetryCalc.scenarioTelemetry())
    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            persistedProfileDraft.collect { saved ->
                val phone = ecoState.value.phone
                if (wizardHydratedForPhone != phone || (_wizardDraft.value.updatedAt == 0L && saved.updatedAt > 0L)) {
                    wizardHydratedForPhone = phone
                    _wizardDraft.value = saved
                }
            }
        }
    }

    fun refreshTelemetry() {
        _telemetry.value = TelemetryCalc.scenarioTelemetry(
            capacityKwp = RateTables.ACTIVE.scenario.pilotCapacityKwp,
            table = RateTables.ACTIVE
        )
    }

    fun emitToast(message: String) {
        _toastEvents.tryEmit(message)
    }

    // --- OTP Auth ---

    fun onPhoneChanged(input: String) {
        _otpUi.update { it.copy(phoneInput = input, error = null) }
    }

    fun onCodeChanged(input: String) {
        val cleaned = com.example.domain.PersianFormat.toLatinDigits(input)
            .filter { it.isDigit() }
            .take(AuthOtpEngine.OTP_LENGTH)
        _otpUi.update { it.copy(codeInput = cleaned, error = null) }
    }

    fun onAcceptedTermsChanged(accepted: Boolean) {
        _otpUi.update { it.copy(acceptedTerms = accepted, error = null) }
    }

    fun editPhoneNumber() {
        countdownJob?.cancel()
        _otpUi.update {
            it.copy(stepIsCode = false, codeInput = "", devPreviewCode = null, error = null, resendCountdown = 0)
        }
    }

    fun requestOtp() {
        val current = _otpUi.value
        if (current.busy) return
        viewModelScope.launch {
            _otpUi.update { it.copy(busy = true, error = null) }
            val result = repository.requestOtp(current.phoneInput)
            result.onSuccess { (code, waitSeconds) ->
                _otpUi.update {
                    it.copy(
                        busy = false,
                        stepIsCode = true,
                        codeInput = "",
                        devPreviewCode = code,
                        resendCountdown = waitSeconds,
                        error = null
                    )
                }
                startCountdown(waitSeconds)
            }.onFailure { err ->
                _otpUi.update { it.copy(busy = false, error = err.message ?: "خطا در ارسال کد") }
            }
        }
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        val current = _otpUi.value
        if (current.busy) return
        viewModelScope.launch {
            _otpUi.update { it.copy(busy = true, error = null) }
            when (val res = repository.verifyOtp(current.phoneInput, current.codeInput, current.acceptedTerms)) {
                is OtpVerifyResult.Success -> {
                    countdownJob?.cancel()
                    _otpUi.update {
                        it.copy(busy = false, stepIsCode = false, codeInput = "", devPreviewCode = null, error = null)
                    }
                    emitToast("با موفقیت وارد حساب شدید")
                    onSuccess()
                }
                is OtpVerifyResult.Failure -> {
                    _otpUi.update { it.copy(busy = false, error = res.reason) }
                }
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000L)
                remaining--
                _otpUi.update { it.copy(resendCountdown = remaining) }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        val phone = ecoState.value.phone
        viewModelScope.launch {
            repository.logout(phone)
            wizardHydratedForPhone = null
            _wizardDraft.value = ProfileDraft()
            _otpUi.value = OtpUiState(phoneInput = phone)
            emitToast("از حساب خارج شدید")
            onComplete()
        }
    }

    // --- Bill & Revisions ---

    fun confirmBill(
        bill: BillData,
        source: String,
        confidence: String,
        issuesText: String,
        dirty: Boolean,
        onDone: (Int) -> Unit
    ) {
        val phone = ecoState.value.phone
        viewModelScope.launch {
            val rev = repository.confirmBillAndAppendRevision(
                phone = phone,
                bill = bill,
                source = source,
                confidence = confidence,
                issuesText = issuesText,
                dirty = dirty
            )
            emitToast(
                if (dirty) "تغییرات با شماره بازبینی ${com.example.domain.PersianFormat.faDigits(rev)} ذخیره شد"
                else "اطلاعات قبض تأیید شد"
            )
            onDone(rev)
        }
    }

    fun appendCurrentBillAsRevision() {
        val state = ecoState.value
        viewModelScope.launch {
            val rev = repository.appendBillRevision(state.phone, state.bill, source = "manual")
            repository.applyRevisionAsBasis(
                BillRevisionEntity(
                    version = rev,
                    phone = state.phone,
                    period = state.bill.period,
                    peakKwh = state.bill.peak,
                    midKwh = state.bill.mid,
                    lowKwh = state.bill.low,
                    amountRial = state.bill.total,
                    source = "manual",
                    confidence = "high",
                    issuesText = "",
                    confirmed = true,
                    createdAt = System.currentTimeMillis()
                ),
                currentBillId = state.bill.id
            )
            emitToast("نسخه ${com.example.domain.PersianFormat.faDigits(rev)} ثبت و مبنای تحلیل شد")
        }
    }

    fun selectRevisionAsBasis(revision: BillRevisionEntity) {
        val currentId = ecoState.value.bill.id
        viewModelScope.launch {
            repository.applyRevisionAsBasis(revision, currentId)
            emitToast("نسخه ${com.example.domain.PersianFormat.faDigits(revision.version)} مبنای تحلیل شد")
        }
    }

    // --- Plan, Design, Channel, Loan, Quotes, Case, Income ---

    fun acceptPlan(plan: PlanId) {
        viewModelScope.launch {
            repository.updateEcoState { it.copy(plan = plan, planAccepted = true) }
        }
    }

    fun setBattery(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateEcoState { s ->
                val nextChannel = when {
                    enabled -> ChannelId.FIT_BATTERY
                    s.channel == ChannelId.FIT_BATTERY -> ChannelId.FIT_BASE
                    else -> s.channel
                }
                s.copy(battery = enabled, channel = nextChannel)
            }
        }
    }

    fun chooseChannel(channel: ChannelId) {
        viewModelScope.launch {
            val requiresBat = RateTables.ACTIVE.channels.firstOrNull { it.id == channel }?.requiresBattery == true
            repository.updateEcoState { s ->
                s.copy(
                    channel = channel,
                    channelConfirmed = true,
                    battery = if (requiresBat) true else s.battery
                )
            }
        }
    }

    fun updateLoan(loan: LoanSpec) {
        viewModelScope.launch {
            repository.updateEcoState { it.copy(loan = loan) }
        }
    }

    fun chooseStrategy(strategy: Int) {
        viewModelScope.launch {
            repository.updateEcoState { it.copy(strategy = strategy) }
        }
    }

    fun placeQuoteOrder(quoteId: String) {
        viewModelScope.launch {
            repository.updateEcoState { it.copy(quoteId = quoteId, orderPlaced = true) }
        }
    }

    fun setVisitConfirmed(confirmed: Boolean) {
        viewModelScope.launch {
            repository.updateEcoState { it.copy(visitConfirmed = confirmed) }
        }
    }

    fun markMonthPaid(monthKey: String) {
        viewModelScope.launch {
            repository.updateEcoState { s -> s.copy(paidMonths = s.paidMonths + monthKey) }
        }
    }

    fun submitPlantReport(monthKey: String, text: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val id = repository.submitPlantReport(monthKey, text)
            emitToast("گزارش با کد ${com.example.domain.PersianFormat.faDigits(id)} ثبت شد")
            onDone(id)
        }
    }

    // --- Consents ---

    fun grantConsent() {
        val state = ecoState.value
        viewModelScope.launch {
            repository.grantConsent(state.phone)
            emitToast("رضایت ثبت و در Audit Log ذخیره شد")
        }
    }

    fun revokeConsent(consentId: String) {
        val state = ecoState.value
        viewModelScope.launch {
            repository.revokeConsent(state.phone, consentId)
            emitToast("رضایت لغو شد")
        }
    }

    // --- Wizard ---

    fun updateWizardDraft(transform: (ProfileDraft) -> ProfileDraft) {
        val next = transform(_wizardDraft.value).copy(updatedAt = System.currentTimeMillis())
        _wizardDraft.value = next
        _wizardSaveStatus.value = "در حال ذخیره…"
        wizardSaveJob?.cancel()
        wizardSaveJob = viewModelScope.launch {
            delay(400L)
            repository.saveProfileDraft(ecoState.value.phone, next)
            _wizardSaveStatus.value = "ذخیره خودکار شد"
        }
    }

    fun completeWizard(onCompleted: () -> Unit) {
        val current = _wizardDraft.value
        val now = System.currentTimeMillis()
        val doneDraft = current.copy(updatedAt = now, completedAt = now)
        _wizardDraft.value = doneDraft
        wizardSaveJob?.cancel()
        viewModelScope.launch {
            repository.saveProfileDraft(ecoState.value.phone, doneDraft, completedAt = now)
            _wizardSaveStatus.value = "پروفایل ثبت نهایی شد"
            emitToast("پروفایل بار ثبت نهایی شد")
            onCompleted()
        }
    }

    companion object {
        fun factory(repository: EcoRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EcoViewModel(repository) as T
            }
    }
}
