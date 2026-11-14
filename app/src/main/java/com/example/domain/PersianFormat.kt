package com.example.domain

import java.util.Locale
import kotlin.math.abs

object PersianFormat {
    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private const val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"

    val SHAMSI_MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    fun faDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(FA_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun faDigits(value: Int): String = faDigits(value.toString())
    fun faDigits(value: Long): String = faDigits(value.toString())

    fun toLatinDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val faIdx = FA_DIGITS.indexOf(ch)
            if (faIdx >= 0) {
                sb.append(('0'.code + faIdx).toChar())
            } else {
                val arIdx = AR_DIGITS.indexOf(ch)
                if (arIdx >= 0) {
                    sb.append(('0'.code + arIdx).toChar())
                } else {
                    sb.append(ch)
                }
            }
        }
        return sb.toString()
    }

    fun fa(value: Double, fractionDigits: Int = 0, groupThousands: Boolean = true): String {
        val negative = value < 0.0
        val fixed = String.format(Locale.US, "%.${fractionDigits}f", abs(value))
        val parts = fixed.split('.')
        val intPart = parts[0]
        val decPart = parts.getOrNull(1)
        val grouped = if (groupThousands) groupThousands(intPart) else intPart
        val cleanDec = decPart?.trimEnd('0')
        val out = if (!cleanDec.isNullOrEmpty()) "$grouped,$cleanDec" else grouped
        return (if (negative) "-" else "") + faDigits(out)
    }

    fun fa(value: Int, groupThousands: Boolean = true): String =
        fa(value.toDouble(), 0, groupThousands)

    fun fa(value: Long, groupThousands: Boolean = true): String =
        fa(value.toDouble(), 0, groupThousands)

    fun faFixed(value: Double, fractionDigits: Int): String {
        val fixed = String.format(Locale.US, "%.${fractionDigits}f", abs(value))
        val parts = fixed.split('.')
        val intPart = groupThousands(parts[0])
        val decPart = parts.getOrNull(1)
        val out = if (!decPart.isNullOrEmpty()) "$intPart,$decPart" else intPart
        return (if (value < 0.0) "-" else "") + faDigits(out)
    }

    private fun groupThousands(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        var count = 0
        for (i in digits.length - 1 downTo 0) {
            sb.append(digits[i])
            count++
            if (count % 3 == 0 && i > 0) sb.append('٬')
        }
        return sb.reverse().toString()
    }

    fun faTimer(totalSeconds: Int, withHours: Boolean = false): String {
        val s = totalSeconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        val text = if (withHours) {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
        } else {
            String.format(Locale.US, "%02d:%02d", m, sec)
        }
        return faDigits(text)
    }

    fun formatPhone(phone: String): String {
        val d = toLatinDigits(phone).filter { it.isDigit() }
        val parts = listOfNotNull(
            d.take(4).takeIf { it.isNotEmpty() },
            d.drop(4).take(3).takeIf { it.isNotEmpty() },
            d.drop(7).takeIf { it.isNotEmpty() }
        )
        return faDigits(parts.joinToString(" "))
    }

    fun maskPhone(phone: String): String {
        val p = AuthOtpEngine.normalizePhone(phone)
        return if (p.length == 11) "${p.take(4)}***${p.takeLast(2)}" else "***"
    }

    fun tehranClock(epochMillis: Long): String {
        val shiftedSeconds = ((epochMillis / 1000L) + 3 * 3600 + 1800) % 86400
        val h = (shiftedSeconds / 3600).toInt()
        val m = ((shiftedSeconds % 3600) / 60).toInt()
        return faDigits(String.format(Locale.US, "%02d:%02d", h, m))
    }
}
