package com.example.domain

import kotlin.math.abs
import kotlin.math.roundToInt

enum class Confidence(val raw: String, val weight: Double) {
    HIGH("high", 1.0),
    MEDIUM("medium", 0.75),
    LOW("low", 0.4);

    companion object {
        fun fromRaw(raw: String?): Confidence =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: LOW
    }
}

enum class BillField(val raw: String, val label: String) {
    BILL_ID("billId", "شناسه قبض"),
    PERIOD("period", "دوره صورتحساب"),
    PEAK_KWH("peakKwh", "مصرف اوج"),
    MID_KWH("midKwh", "مصرف میان‌باری"),
    LOW_KWH("lowKwh", "مصرف کم‌باری"),
    TOTAL_KWH("totalKwh", "مصرف کل"),
    AMOUNT_RIAL("amountRial", "مبلغ کل قبض"),
    GENERAL("general", "کلی");

    companion object {
        fun fromRaw(raw: String?): BillField =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: GENERAL
    }
}

data class BillIssue(
    val field: BillField,
    val message: String,
    val isRuleSource: Boolean
)

data class ExtractedBill(
    val isElectricityBill: Boolean,
    val billId: String?,
    val period: String?,
    val periodDays: Int?,
    val peakKwh: Int?,
    val midKwh: Int?,
    val lowKwh: Int?,
    val totalKwh: Int?,
    val amountRial: Long?,
    val confidence: Map<BillField, Confidence>,
    val aiIssues: List<Pair<BillField, String>>,
    val summary: String?
)

data class BillUsageAnalysis(
    val totalKwh: Int,
    val peakSharePct: Int,
    val midSharePct: Int,
    val lowSharePct: Int,
    val pricePerKwh: Long?
)

object BillChecks {
    const val TIER_SUM_TOLERANCE_PCT = 2.0
    const val MIN_PRICE_PER_KWH = 300.0
    const val MAX_PRICE_PER_KWH = 2_500_000.0
    const val MIN_PERIOD_DAYS = 25
    const val MAX_PERIOD_DAYS = 70
    const val BILL_ID_LENGTH = 13

    fun checkBillConsistency(b: ExtractedBill): List<BillIssue> {
        val issues = mutableListOf<BillIssue>()
        fun add(field: BillField, message: String) {
            issues.add(BillIssue(field, message, isRuleSource = true))
        }

        if (!b.isElectricityBill) {
            add(BillField.GENERAL, "تصویر شبیه قبض برق نیست؛ لطفاً عکس واضح قبض برق را بارگذاری کنید.")
            return issues
        }

        if (b.billId.isNullOrBlank()) add(BillField.BILL_ID, "${BillField.BILL_ID.label} در تصویر خوانده نشد.")
        if (b.period.isNullOrBlank()) add(BillField.PERIOD, "${BillField.PERIOD.label} در تصویر خوانده نشد.")
        if (b.peakKwh == null) add(BillField.PEAK_KWH, "${BillField.PEAK_KWH.label} در تصویر خوانده نشد.")
        if (b.midKwh == null) add(BillField.MID_KWH, "${BillField.MID_KWH.label} در تصویر خوانده نشد.")
        if (b.lowKwh == null) add(BillField.LOW_KWH, "${BillField.LOW_KWH.label} در تصویر خوانده نشد.")
        if (b.amountRial == null) add(BillField.AMOUNT_RIAL, "${BillField.AMOUNT_RIAL.label} در تصویر خوانده نشد.")

        val digitsOnly = b.billId?.filter { it.isDigit() }
        if (digitsOnly != null && digitsOnly.length != BILL_ID_LENGTH) {
            add(BillField.BILL_ID, "شناسه قبض باید $BILL_ID_LENGTH رقم باشد.")
        }

        if ((b.peakKwh ?: 0) < 0) add(BillField.PEAK_KWH, "${BillField.PEAK_KWH.label} نمی‌تواند منفی باشد.")
        if ((b.midKwh ?: 0) < 0) add(BillField.MID_KWH, "${BillField.MID_KWH.label} نمی‌تواند منفی باشد.")
        if ((b.lowKwh ?: 0) < 0) add(BillField.LOW_KWH, "${BillField.LOW_KWH.label} نمی‌تواند منفی باشد.")
        if ((b.totalKwh ?: 0) < 0) add(BillField.TOTAL_KWH, "${BillField.TOTAL_KWH.label} نمی‌تواند منفی باشد.")
        if ((b.amountRial ?: 0L) < 0L) add(BillField.AMOUNT_RIAL, "${BillField.AMOUNT_RIAL.label} نمی‌تواند منفی باشد.")

        val tierSum = if (b.peakKwh != null && b.midKwh != null && b.lowKwh != null) {
            b.peakKwh + b.midKwh + b.lowKwh
        } else null

        if (tierSum != null && b.totalKwh != null && b.totalKwh > 0) {
            val gapPct = (abs(tierSum - b.totalKwh).toDouble() / b.totalKwh.toDouble()) * 100.0
            if (gapPct > TIER_SUM_TOLERANCE_PCT) {
                add(
                    BillField.TOTAL_KWH,
                    "جمع مصرف اوج، میان‌باری و کم‌باری ($tierSum) با مصرف کل (${b.totalKwh}) همخوانی ندارد."
                )
            }
        }

        if (b.peakKwh != null && tierSum != null && tierSum > 0 && b.peakKwh > tierSum * 0.6) {
            add(BillField.PEAK_KWH, "سهم مصرف اوج بیش از ۶۰٪ کل مصرف است؛ مقدار را بررسی کنید.")
        }

        val energy = b.totalKwh ?: tierSum
        if (energy != null && energy > 0 && b.amountRial != null && b.amountRial > 0L) {
            val perKwh = b.amountRial.toDouble() / energy.toDouble()
            if (perKwh < MIN_PRICE_PER_KWH || perKwh > MAX_PRICE_PER_KWH) {
                add(BillField.AMOUNT_RIAL, "نسبت مبلغ قبض به مصرف غیرعادی است؛ مبلغ یا مصرف را بررسی کنید.")
            }
        }

        if (b.periodDays != null && (b.periodDays < MIN_PERIOD_DAYS || b.periodDays > MAX_PERIOD_DAYS)) {
            add(BillField.PERIOD, "طول دوره صورتحساب (${b.periodDays} روز) غیرمعمول است.")
        }

        return issues
    }

    fun mergeIssues(b: ExtractedBill): List<BillIssue> {
        val rules = checkBillConsistency(b)
        val ruleFields = rules.map { it.field }.toSet()
        val ai = b.aiIssues
            .filter { (field, msg) -> msg.trim().isNotEmpty() && !ruleFields.contains(field) }
            .map { (field, msg) -> BillIssue(field, msg.trim(), isRuleSource = false) }
        return rules + ai
    }

    fun analyzeBillUsage(peakKwh: Int, midKwh: Int, lowKwh: Int, amountRial: Long): BillUsageAnalysis? {
        val total = peakKwh + midKwh + lowKwh
        if (total <= 0) return null
        fun share(v: Int): Int = ((v.toDouble() / total.toDouble()) * 100.0).roundToInt()
        return BillUsageAnalysis(
            totalKwh = total,
            peakSharePct = share(peakKwh),
            midSharePct = share(midKwh),
            lowSharePct = share(lowKwh),
            pricePerKwh = if (amountRial > 0L) (amountRial.toDouble() / total.toDouble()).roundToInt().toLong() else null
        )
    }

    fun overallConfidence(conf: Map<BillField, Confidence>): Int {
        if (conf.isEmpty()) return 0
        val avg = conf.values.sumOf { it.weight } / conf.size.toDouble()
        return (avg * 100.0).roundToInt()
    }
}
