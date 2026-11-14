package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.BillField
import com.example.domain.Confidence
import com.example.domain.ExtractedBill
import com.example.domain.LoadProfileCalc
import com.example.domain.PersianFormat
import com.example.domain.ProfileDraft
import com.example.domain.RateTables
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiRepository"

sealed interface BillExtractionUiState {
    data object Idle : BillExtractionUiState
    data object Loading : BillExtractionUiState
    data class Success(val bill: ExtractedBill) : BillExtractionUiState
    data class Error(val message: String) : BillExtractionUiState
}

sealed interface AssistantStreamEvent {
    data class Chunk(val accumulatedText: String) : AssistantStreamEvent
    data class Completed(val finalText: String) : AssistantStreamEvent
    data class Error(val message: String) : AssistantStreamEvent
}

class AiRepository {
    private val billSchema = Schema.obj(
        properties = mapOf(
            "isElectricityBill" to Schema.boolean(description = "True if image is an Iranian electricity bill"),
            "billId" to Schema.string(description = "13-digit شناسه قبض digits only", nullable = true),
            "period" to Schema.string(description = "Persian billing period e.g. مرداد تا مهر ۱۴۰۵", nullable = true),
            "periodDays" to Schema.integer(description = "Billing period length in days", nullable = true),
            "peakKwh" to Schema.integer(description = "Peak consumption (اوج بار) in kWh", nullable = true),
            "midKwh" to Schema.integer(description = "Mid-load consumption (میان باری) in kWh", nullable = true),
            "lowKwh" to Schema.integer(description = "Low-load consumption (کم باری) in kWh", nullable = true),
            "totalKwh" to Schema.integer(description = "Printed total consumption in kWh", nullable = true),
            "amountRial" to Schema.double(description = "Payable amount in RIAL (multiply Toman by 10)", nullable = true),
            "confidence" to Schema.obj(
                properties = mapOf(
                    "billId" to Schema.enumeration(listOf("high", "medium", "low")),
                    "period" to Schema.enumeration(listOf("high", "medium", "low")),
                    "peakKwh" to Schema.enumeration(listOf("high", "medium", "low")),
                    "midKwh" to Schema.enumeration(listOf("high", "medium", "low")),
                    "lowKwh" to Schema.enumeration(listOf("high", "medium", "low")),
                    "totalKwh" to Schema.enumeration(listOf("high", "medium", "low")),
                    "amountRial" to Schema.enumeration(listOf("high", "medium", "low"))
                )
            ),
            "issues" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "field" to Schema.enumeration(
                            listOf("billId", "period", "peakKwh", "midKwh", "lowKwh", "totalKwh", "amountRial", "general")
                        ),
                        "message" to Schema.string()
                    )
                )
            ),
            "summary" to Schema.string(description = "One short Persian sentence summarizing consumption and cost")
        ),
        optionalProperties = listOf("summary")
    )

    private val billModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = billSchema
            },
            systemInstruction = content {
                text(
                    """
                    You read photos of Iranian electricity bills (قبض برق) for a solar-plant feasibility app.
                    Extract only what is printed on the bill. Never guess a value you cannot read; use null instead.
                    - billId: the 13-digit «شناسه قبض», digits only (convert Persian/Arabic digits to ASCII).
                    - period: the billing period in Persian, e.g. «مرداد تا مهر ۱۴۰۵». periodDays: number of days if printed or computable, else null.
                    - peakKwh / midKwh / lowKwh: consumption for «اوج بار», «میان باری», «کم باری» in kWh.
                    - totalKwh: total consumption printed on the bill (kWh), else null.
                    - amountRial: total payable amount in RIAL (if printed in toman, multiply by 10).
                    - confidence: per field, "low" when blurry, partially hidden, handwritten or ambiguous.
                    - issues: short Persian sentences (max 6) describing anything inconsistent or suspicious. Empty array if none.
                    - summary: one short Persian sentence (max 25 words) about the consumption and cost shown on the bill.
                    - isElectricityBill: false if the image is not an electricity bill.
                    Do not include names, addresses or other personal data anywhere in the output.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun extractBillFromBitmap(bitmap: Bitmap): Result<ExtractedBill> {
        return try {
            val scaled = downscaleBitmap(bitmap, maxSide = 1400)
            val response = withTimeout(60_000L) {
                billModel.generateContent(
                    content {
                        image(scaled)
                        text("Extract the bill data from this photo, list any inconsistencies, then write the one-line summary.")
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("پاسخ هوش مصنوعی خالی بود.")
            val root = JSONObject(jsonText)

            val confJson = root.optJSONObject("confidence")
            val confMap = mapOf(
                BillField.BILL_ID to Confidence.fromRaw(confJson?.optString("billId")),
                BillField.PERIOD to Confidence.fromRaw(confJson?.optString("period")),
                BillField.PEAK_KWH to Confidence.fromRaw(confJson?.optString("peakKwh")),
                BillField.MID_KWH to Confidence.fromRaw(confJson?.optString("midKwh")),
                BillField.LOW_KWH to Confidence.fromRaw(confJson?.optString("lowKwh")),
                BillField.TOTAL_KWH to Confidence.fromRaw(confJson?.optString("totalKwh")),
                BillField.AMOUNT_RIAL to Confidence.fromRaw(confJson?.optString("amountRial"))
            )

            val issuesArray = root.optJSONArray("issues")
            val aiIssues = buildList {
                if (issuesArray != null) {
                    for (i in 0 until issuesArray.length()) {
                        val item = issuesArray.optJSONObject(i) ?: continue
                        val field = BillField.fromRaw(item.optString("field"))
                        val msg = item.optString("message").trim()
                        if (msg.isNotEmpty()) add(field to msg)
                    }
                }
            }

            fun optIntOrNull(key: String): Int? =
                if (root.isNull(key)) null else root.optInt(key)

            fun optLongOrNull(key: String): Long? =
                if (root.isNull(key)) null else root.optDouble(key).toLong()

            fun optStringOrNull(key: String): String? =
                if (root.isNull(key)) null else root.optString(key).takeIf { it.isNotBlank() }

            val extracted = ExtractedBill(
                isElectricityBill = root.optBoolean("isElectricityBill", false),
                billId = optStringOrNull("billId")?.let { PersianFormat.toLatinDigits(it).filter { ch -> ch.isDigit() } },
                period = optStringOrNull("period"),
                periodDays = optIntOrNull("periodDays"),
                peakKwh = optIntOrNull("peakKwh"),
                midKwh = optIntOrNull("midKwh"),
                lowKwh = optIntOrNull("lowKwh"),
                totalKwh = optIntOrNull("totalKwh"),
                amountRial = optLongOrNull("amountRial"),
                confidence = confMap,
                aiIssues = aiIssues,
                summary = optStringOrNull("summary")
            )
            Result.success(extracted)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Bill extraction timed out", e)
            Result.failure(Exception("خواندن قبض بیش از ۶۰ ثانیه طول کشید؛ لطفاً دوباره تلاش کنید."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Bill extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "خواندن قبض با هوش مصنوعی ناموفق بود."))
        }
    }

    fun streamAssistantReply(
        history: List<Pair<String, String>>,
        userMessage: String,
        ecoState: EcoState,
        latestRevision: BillRevisionEntity?,
        profileDraft: ProfileDraft?
    ): Flow<AssistantStreamEvent> = flow {
        val systemPrompt = buildAssistantSystemPrompt(ecoState, latestRevision, profileDraft)
        val chatModel = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemPrompt) }
        )
        val chatHistory = history.takeLast(12).map { (role, text) ->
            content(role = if (role == "user") "user" else "model") { text(text) }
        }
        val chat = chatModel.startChat(history = chatHistory)
        val accumulated = StringBuilder()

        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(AssistantStreamEvent.Chunk(accumulated.toString()))
                    }
                }
            }
            val finalReply = accumulated.toString().trim()
            if (finalReply.isEmpty()) {
                emit(AssistantStreamEvent.Error("پاسخ خالی از سرویس دستیار دریافت شد."))
            } else {
                emit(AssistantStreamEvent.Completed(finalReply))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Assistant stream timed out", e)
            emit(AssistantStreamEvent.Error("پاسخ دستیار دیر کرد؛ دوباره بپرسید."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Assistant stream failed", e)
            emit(AssistantStreamEvent.Error(e.localizedMessage ?: "ارتباط با سرویس دستیار برقرار نشد."))
        }
    }

    private fun buildAssistantSystemPrompt(
        ecoState: EcoState,
        latestRevision: BillRevisionEntity?,
        profileDraft: ProfileDraft?
    ): String {
        val table = RateTables.ACTIVE
        val channels = table.channels.joinToString("\n") { ch ->
            val bits = mutableListOf(
                "${ch.name}: ${ch.rateRialPerKwh} ریال بر کیلووات‌ساعت",
                "قرارداد ${ch.contractYears} ساله"
            )
            if (ch.paymentDelay != null) bits.add("تسویه ${ch.paymentDelay}")
            if (ch.requiresBattery) bits.add("نیازمند باتری حداقل ${table.batteryMinKwhPerKwp} کیلووات‌ساعت بر هر کیلووات")
            bits.add("شرط فعال‌سازی: ${ch.activation}")
            "- ${bits.joinToString("، ")}"
        }
        val annualProd = table.scenario.monthlyProductionKwh.sum()
        val plans = table.scenario.optimizationPlans.joinToString("، ") { p ->
            "${p.name} ${p.savingPct}٪ (پیش‌سرمایش ${p.preCoolTemp}°، اوج ${p.peakTemp}°)"
        }
        val channelLabel = table.channels.firstOrNull { it.id == ecoState.channel }?.name ?: ecoState.channel.rawId
        val billKwh = ecoState.bill.totalKwh

        val contextLines = buildList {
            add("کانال انتخابی: $channelLabel")
            add("نسخه باتری‌دار: ${if (ecoState.battery) "بله" else "خیر"}")
            add("برنامه بهینه‌سازی: ${ecoState.plan.rawId}")
            add("مصرف قبض فعلی: $billKwh کیلووات‌ساعت")
            if (latestRevision != null) {
                val revKwh = latestRevision.peakKwh + latestRevision.midKwh + latestRevision.lowKwh
                add("آخرین قبض تأییدشده در دفتر: نسخه ${latestRevision.version}، دوره ${latestRevision.period}، $revKwh کیلووات‌ساعت، مبلغ ${latestRevision.amountRial} ریال")
            }
            if (profileDraft != null && profileDraft.completedAt != null) {
                val lp = LoadProfileCalc.buildLoadProfile(profileDraft, table)
                add("پروفایل بار: شهر ${profileDraft.city}، مصرف روزانه ${lp.dailyKwh} kWh، مساحت بام ${profileDraft.roof.area} مترمربع، ظرفیت پیشنهادی ${lp.suggestedKwp} کیلووات")
            }
        }.joinToString("\n")

        return """
            تو «دستیار اکوسولار» هستی؛ دستیار فارسی‌زبان پلتفرم نیروگاه خورشیدی خانگی اکوسولار.
            وظیفه‌ات پاسخ به پرسش‌های کاربر درباره همین پلتفرم است: قبض برق، اسکن قبض، مصرف و لوازم برقی، طراحی نیروگاه، باتری، کانال درآمد، خرید تضمینی، خودتأمین، وام، پرونده مهرسان، پایش تولید، تطبیق کنتور و مراحل اپلیکیشن.

            قواعد قطعی:
            ۱. فقط در همین حوزه پاسخ بده. اگر پرسش بی‌ربط بود، یک جمله بگو که فقط درباره اکوسولار و انرژی خورشیدی کمک می‌کنی و یک موضوع مرتبط پیشنهاد بده.
            ۲. کوتاه و دقیق بنویس: حداکثر ۵ جمله یا ۱۲۰ کلمه، مگر کاربر صریحاً توضیح بلندتر بخواهد. فهرست حداکثر ۴ مورد.
            ۳. هیچ نرخ، عدد یا قانونی از خودت نساز. فقط از «واقعیت‌های پلتفرم» پایین یا اعدادی که خود کاربر داده استفاده کن.
            ۴. هیچ تضمین سود یا بازدهی قطعی نده؛ بگو ارقام برآوردی‌اند.
            ۵. مبنای مالی صورتحساب، کنتور رسمی توزیع است؛ قرائت اینورتر فقط برای پایش است. اختلاف بیش از ${table.meterTolerancePct}٪ بین کنتور و اینورتر باید بررسی شود.
            ۶. هرگز کد یکبارمصرف، رمز، کد ملی، شماره کارت یا شبای کامل را درخواست یا تکرار نکن.
            ۷. مشاوره حقوقی نده؛ برای تصمیم قطعی به کارشناس یا ساتبا/شرکت توزیع ارجاع بده.
            ۸. فارسی روان و محترمانه بنویس، بدون Markdown سنگین. حداکثر یک ایموجی.

            واقعیت‌های پلتفرم (نسخه جدول نرخ: ${table.version} — ${table.label}):
            کانال‌های درآمد:
            $channels
            - خودتأمین و خرید تضمینی روی یک انشعاب هم‌زمان ممکن نیست.
            - وام مرجع: ${table.loan.amountRial} ریال با نرخ ${table.loan.annualRatePct}٪ و ${table.loan.months} قسط؛ سقف ${table.loan.maxAmountRial} ریال تا ${table.loan.maxCapacityKwp} کیلووات، بانک عامل ${table.loan.bank}.
            - نیروگاه نمونه ${table.scenario.pilotCapacityKwp} کیلوواتی: تولید سالانه $annualProd کیلووات‌ساعت؛ ضریب عملکرد ${(table.scenario.performanceRatio * 100).toInt()}٪، سایه‌اندازی ${table.scenario.shadowLossPct}٪، تلفات سیستم ${table.scenario.systemLossPct}٪.
            - برنامه‌های بهینه‌سازی مصرف: $plans.
            - برای بام مشاع، رضایت ۱۰۰٪ مالکان پیش از شروع پروژه لازم است.
            - حداقل باتری ${table.batteryMinKwhPerKwp} کیلووات‌ساعت به ازای هر کیلووات نصب‌شده است.

            وضعیت فعلی کاربر:
            $contextLines
        """.trimIndent()
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest.toFloat()
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
