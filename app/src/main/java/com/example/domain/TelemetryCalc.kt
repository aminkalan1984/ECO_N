package com.example.domain

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

enum class TelemetryState(val label: String) {
    LIVE("زنده"),
    STALE("داده کهنه"),
    OFFLINE("آفلاین")
}

data class TelemetryPoint(
    val label: String,
    val value: Double
)

data class TelemetrySnapshot(
    val isScenario: Boolean,
    val state: TelemetryState,
    val sourceLabel: String,
    val ageMinutes: Int,
    val capacityKwp: Double,
    val pLiveKw: Double,
    val todayKwh: Double,
    val weekKwh: Double,
    val monthKwh: Int,
    val weekSeries: List<TelemetryPoint>,
    val monthSeries: List<TelemetryPoint>,
    val performanceRatioPct: Int,
    val rateTableVersion: String
)

object TelemetryCalc {
    private val WEEK_LABELS = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    private const val DAYS_PER_MONTH = 30.0

    private fun round1(v: Double): Double = round(v * 10.0) / 10.0

    /** Approximates Shamsi month (1..12) in Tehran timezone for scenario telemetry selection. */
    fun currentShamsiMonth(): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            days -= 1
            jy += days / 365
            days %= 365
        }
        return if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
    }

    fun scenarioTelemetry(
        capacityKwp: Double? = null,
        table: RateTable = RateTables.ACTIVE
    ): TelemetrySnapshot {
        val scenario = table.scenario
        val cap = capacityKwp ?: scenario.pilotCapacityKwp
        val scale = cap / scenario.pilotCapacityKwp
        val monthIdx = (currentShamsiMonth() - 1).coerceIn(0, 11)
        val monthlyKwh = scenario.monthlyProductionKwh[monthIdx] * scale
        val monthKwh = monthlyKwh.roundToInt()
        val dailyKwh = monthlyKwh / DAYS_PER_MONTH

        val weekSeries = WEEK_LABELS.mapIndexed { i, lbl ->
            TelemetryPoint(lbl, round1(dailyKwh * scenario.dailyVariation.getOrElse(i) { 1.0 }))
        }
        val monthSeries = scenario.monthlyProductionKwh.mapIndexed { i, v ->
            TelemetryPoint(PersianFormat.faDigits(i + 1), (v * scale).roundToInt().toDouble())
        }

        return TelemetrySnapshot(
            isScenario = true,
            state = TelemetryState.LIVE,
            sourceLabel = "شبیه‌ساز",
            ageMinutes = 2,
            capacityKwp = cap,
            pLiveKw = round1(cap * scenario.performanceRatio * (scenario.peakSunHours / 5.0)),
            todayKwh = round1(dailyKwh),
            weekKwh = round1(weekSeries.sumOf { it.value }),
            monthKwh = monthKwh,
            weekSeries = weekSeries,
            monthSeries = monthSeries,
            performanceRatioPct = (scenario.performanceRatio * 100.0).roundToInt(),
            rateTableVersion = table.version
        )
    }

    fun productionAt(hour: Double, peak: Double = 3.6): Double {
        if (hour <= 5.6 || hour >= 18.4) return 0.0
        return peak * exp(-(hour - 12.0).pow(2) / (2.0 * 2.35 * 2.35))
    }

    fun outsideTempAt(hour: Double, shift: Double = 0.0): Double {
        val keys = listOf(
            0.0 to 28.0,
            6.0 to 25.5,
            10.0 to 31.0,
            15.0 to 37.5,
            20.0 to 33.0,
            24.0 to 28.5
        )
        for (i in 0 until keys.size - 1) {
            val (h0, t0) = keys[i]
            val (h1, t1) = keys[i + 1]
            if (hour in h0..h1) {
                val p = (hour - h0) / (h1 - h0)
                val s = (1.0 - cos(PI * p)) / 2.0
                return t0 + (t1 - t0) * s + shift
            }
        }
        return 28.0 + shift
    }

    fun buildHourlyPowerSeries(
        peakKw: Double = 3.6,
        nowHour: Double = 14.5
    ): List<HourlyPowerPoint> {
        val hours = listOf(6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0)
        return hours.map { h ->
            val clearSky = round1(productionAt(h, peakKw))
            val actual = if (h <= nowHour) clearSky else null
            val forecast = if (h >= nowHour - 0.5) round1(clearSky * 0.94) else null
            HourlyPowerPoint(
                hour = h,
                label = "${PersianFormat.fa(h.toInt())}:۰۰",
                actualKw = actual,
                forecastKw = forecast,
                optimalKw = clearSky
            )
        }
    }
}

data class HourlyPowerPoint(
    val hour: Double,
    val label: String,
    val actualKw: Double?,
    val forecastKw: Double?,
    val optimalKw: Double
)
