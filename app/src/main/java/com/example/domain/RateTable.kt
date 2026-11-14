package com.example.domain

enum class ChannelId(val rawId: String) {
    FIT_BATTERY("fit-battery"),
    FIT_BASE("fit-base"),
    SELF("self");

    companion object {
        fun fromRaw(raw: String): ChannelId =
            entries.firstOrNull { it.rawId == raw } ?: FIT_BATTERY
    }
}

enum class PlanId(val rawId: String) {
    ECONOMY("economy"),
    BALANCED("balanced"),
    COMFORT("comfort");

    companion object {
        fun fromRaw(raw: String): PlanId =
            entries.firstOrNull { it.rawId == raw } ?: BALANCED
    }
}

data class RateChannel(
    val id: ChannelId,
    val name: String,
    val rateRialPerKwh: Long,
    val contractYears: Int,
    val paymentDelay: String?,
    val activation: String,
    val requiresBattery: Boolean,
    val isFeedInTariff: Boolean
)

data class LoanTerms(
    val amountRial: Long,
    val annualRatePct: Double,
    val months: Int,
    val maxAmountRial: Long,
    val maxCapacityKwp: Double,
    val bank: String
)

data class OptimizationPlanRow(
    val id: PlanId,
    val name: String,
    val desc: String,
    val savingPct: Int,
    val preCoolTemp: Int,
    val peakTemp: Int
)

data class ApplianceSpec(
    val kind: String,
    val label: String,
    val ratedKw: Double
)

data class ScenarioAssumptions(
    val pilotCapacityKwp: Double,
    val monthlyProductionKwh: List<Int>,
    val dailyVariation: List<Double>,
    val panelWatt: Int,
    val inverterKw: Int,
    val roofAreaPerKwp: Double,
    val roofAreaAvailable: Int,
    val shadowLossPct: Int,
    val systemLossPct: Int,
    val performanceRatio: Double,
    val peakSunHours: Double,
    val tiltDeg: Int,
    val azimuth: String,
    val temperatureSensitivity: Double,
    val mainApplianceKw: Double,
    val coincidenceFactor: Double,
    val appliances: List<ApplianceSpec>,
    val optimizationPlans: List<OptimizationPlanRow>,
    val fitMonthlyIncomeRial: Long
)

data class RateTable(
    val version: String,
    val label: String,
    val channels: List<RateChannel>,
    val loan: LoanTerms,
    val batteryMinKwhPerKwp: Double,
    val meterTolerancePct: Double,
    val annualRateAdjustment: Double,
    val scenario: ScenarioAssumptions
)

object RateTables {
    val ACTIVE: RateTable = RateTable(
        version = "1405.03",
        label = "آبان ۱۴۰۵، نسخه ۳",
        channels = listOf(
            RateChannel(
                id = ChannelId.FIT_BATTERY,
                name = "خرید تضمینی باتری‌دار",
                rateRialPerKwh = 91_169L,
                contractYears = 20,
                paymentDelay = "۲ تا ۳ ماه",
                activation = "باتری حداقل ۷,۵ kWh",
                requiresBattery = true,
                isFeedInTariff = true
            ),
            RateChannel(
                id = ChannelId.FIT_BASE,
                name = "خرید تضمینی پایه",
                rateRialPerKwh = 45_853L,
                contractYears = 20,
                paymentDelay = "۲ تا ۳ ماه",
                activation = "کنتور دوطرفه",
                requiresBattery = false,
                isFeedInTariff = true
            ),
            RateChannel(
                id = ChannelId.SELF,
                name = "خودتأمین",
                rateRialPerKwh = 49_873L,
                contractYears = 1,
                paymentDelay = null,
                activation = "مصرف هم‌زمان در محل",
                requiresBattery = false,
                isFeedInTariff = false
            )
        ),
        loan = LoanTerms(
            amountRial = 3_000_000_000L,
            annualRatePct = 13.0,
            months = 40,
            maxAmountRial = 4_000_000_000L,
            maxCapacityKwp = 10.0,
            bank = "بانک ملت"
        ),
        batteryMinKwhPerKwp = 1.5,
        meterTolerancePct = 3.0,
        annualRateAdjustment = 0.5,
        scenario = ScenarioAssumptions(
            pilotCapacityKwp = 5.0,
            monthlyProductionKwh = listOf(700, 760, 800, 820, 840, 790, 730, 650, 560, 480, 410, 344),
            dailyVariation = listOf(1.02, 1.08, 0.96, 1.13, 1.04, 1.0, 0.99),
            panelWatt = 550,
            inverterKw = 5,
            roofAreaPerKwp = 5.6,
            roofAreaAvailable = 64,
            shadowLossPct = 6,
            systemLossPct = 14,
            performanceRatio = 0.82,
            peakSunHours = 4.3,
            tiltDeg = 30,
            azimuth = "جنوب",
            temperatureSensitivity = 0.07,
            mainApplianceKw = 2.2,
            coincidenceFactor = 0.6,
            appliances = listOf(
                ApplianceSpec("ac", "کولر گازی", 2.2),
                ApplianceSpec("fridge", "یخچال", 0.15),
                ApplianceSpec("washer", "ماشین لباسشویی", 0.5),
                ApplianceSpec("dishwasher", "ماشین ظرفشویی", 0.6),
                ApplianceSpec("lighting", "روشنایی", 0.2),
                ApplianceSpec("water-heater", "آبگرمکن برقی", 2.0),
                ApplianceSpec("ev", "شارژر خودرو برقی", 7.4)
            ),
            optimizationPlans = listOf(
                OptimizationPlanRow(
                    id = PlanId.ECONOMY,
                    name = "اقتصادی",
                    desc = "بیشترین صرفه‌جویی، دمای اوج ۲۸ درجه",
                    savingPct = 34,
                    preCoolTemp = 23,
                    peakTemp = 28
                ),
                OptimizationPlanRow(
                    id = PlanId.BALANCED,
                    name = "متعادل",
                    desc = "پیشنهاد هوش مصنوعی برای خانه شما",
                    savingPct = 28,
                    preCoolTemp = 24,
                    peakTemp = 27
                ),
                OptimizationPlanRow(
                    id = PlanId.COMFORT,
                    name = "راحتی",
                    desc = "تغییر دمای کمتر، صرفه‌جویی کمتر",
                    savingPct = 17,
                    preCoolTemp = 24,
                    peakTemp = 26
                )
            ),
            fitMonthlyIncomeRial = 38_460_000L
        )
    )

    fun minBatteryKwhFor(table: RateTable, capacityKwp: Double): Double =
        kotlin.math.round(capacityKwp * table.batteryMinKwhPerKwp * 10.0) / 10.0
}
