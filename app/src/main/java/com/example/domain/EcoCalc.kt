package com.example.domain

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

data class ChannelSummary(
    val channel: RateChannel,
    val annualMillion: Double
)

object EcoCalc {
    val TABLE: RateTable = RateTables.ACTIVE
    val RATE_TABLE_VERSION: String = TABLE.label
    val MONTHLY_PRODUCTION_KWH: List<Int> = TABLE.scenario.monthlyProductionKwh
    val ANNUAL_PRODUCTION_KWH: Int = MONTHLY_PRODUCTION_KWH.sum()

    fun channelRow(id: ChannelId): RateChannel =
        TABLE.channels.first { it.id == id }

    val BATTERY_FIT_RATE: Long = channelRow(ChannelId.FIT_BATTERY).rateRialPerKwh
    val BASE_FIT_RATE: Long = channelRow(ChannelId.FIT_BASE).rateRialPerKwh
    val SELF_SUPPLY_RATE: Long = channelRow(ChannelId.SELF).rateRialPerKwh
    val MIN_BATTERY_KWH: Double = RateTables.minBatteryKwhFor(TABLE, TABLE.scenario.pilotCapacityKwp)
    val ANNUAL_RATE_ADJUSTMENT: Double = TABLE.annualRateAdjustment
    val MONTHLY_FIT_INCOME: Long = TABLE.scenario.fitMonthlyIncomeRial
    val METER_TOLERANCE_PCT: Double = TABLE.meterTolerancePct

    val CHANNELS: List<ChannelSummary> = TABLE.channels.map { ch ->
        ChannelSummary(
            channel = ch,
            annualMillion = (ANNUAL_PRODUCTION_KWH.toDouble() * ch.rateRialPerKwh.toDouble()) / 1_000_000.0
        )
    }

    fun channelRate(id: ChannelId): Long = channelRow(id).rateRialPerKwh

    fun channelsConflict(a: ChannelId, b: ChannelId): Boolean =
        channelRow(a).isFeedInTariff != channelRow(b).isFeedInTariff

    fun monthlyInstallment(principal: Double, annualRatePct: Double, months: Int): Double {
        if (months <= 0) return 0.0
        val r = annualRatePct / 1200.0
        if (r == 0.0) return principal / months
        return (principal * r) / (1.0 - (1.0 + r).pow(-months))
    }

    fun maxCoverableLoan(monthlyIncome: Double, annualRatePct: Double, months: Int): Double {
        val r = annualRatePct / 1200.0
        if (r == 0.0) return monthlyIncome * months
        return (monthlyIncome * (1.0 - (1.0 + r).pow(-months))) / r
    }

    fun coverageRatio(monthlyIncome: Double, installment: Double): Double {
        if (installment <= 0.0) return 1.0
        return min(1.0, monthlyIncome / installment)
    }

    fun coverageByYear(
        firstYear: Double,
        years: Int = 3,
        growth: Double = ANNUAL_RATE_ADJUSTMENT
    ): List<Double> =
        (0 until years).map { i -> min(1.0, firstYear * (1.0 + growth).pow(i)) }

    fun meterDiscrepancyPct(inverterKwh: Int, meterKwh: Int): Double {
        if (inverterKwh <= 0) return 0.0
        return ((inverterKwh - meterKwh).toDouble() / inverterKwh.toDouble()) * 100.0
    }

    fun canLockEnergy(inverterKwh: Int, meterKwh: Int): Boolean =
        abs(meterDiscrepancyPct(inverterKwh, meterKwh)) <= METER_TOLERANCE_PCT

    fun invoiceGross(meterKwh: Int, lockedRate: Long): Long =
        (meterKwh.toDouble() * lockedRate.toDouble()).roundToLong()
}
