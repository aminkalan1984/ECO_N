package com.example.domain

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class Sector(val raw: String, val label: String) {
    HOME("home", "خانگی"),
    COMMERCIAL("commercial", "تجاری"),
    OFFICE("office", "اداری"),
    INDUSTRIAL("industrial", "صنعتی");

    companion object {
        fun fromRaw(raw: String?): Sector? = entries.firstOrNull { it.raw == raw }
    }
}

enum class Tariff(val raw: String, val label: String) {
    RESIDENTIAL("residential", "خانگی"),
    COMMERCIAL("commercial", "تجاری"),
    INDUSTRIAL("industrial", "صنعتی"),
    AGRICULTURAL("agricultural", "کشاورزی");

    companion object {
        fun fromRaw(raw: String): Tariff = entries.firstOrNull { it.raw == raw } ?: RESIDENTIAL
    }
}

@Serializable
data class ApplianceEntry(
    val id: String,
    val kind: String,
    val label: String,
    val ratedKw: Double,
    val count: Int,
    val hoursPerDay: Double
)

@Serializable
data class ConnectionSpec(
    val meterSerial: String = "",
    val tariffRaw: String = Tariff.RESIDENTIAL.raw,
    val demandKw: Double = 0.0,
    val bidirectional: Boolean = false
)

@Serializable
data class RoofSpec(
    val area: Double = 0.0,
    val tilt: Int = 30,
    val azimuth: String = "جنوب",
    val shadowPct: Int = 0
)

@Serializable
data class OwnersSpec(
    val total: Int = 1,
    val granted: Int = 0
)

data class ProfileDraft(
    val step: Int = 0,
    val sector: Sector? = null,
    val city: String = "",
    val connection: ConnectionSpec = ConnectionSpec(),
    val appliances: List<ApplianceEntry> = emptyList(),
    val roof: RoofSpec = RoofSpec(),
    val sharedOwnership: Boolean = false,
    val owners: OwnersSpec = OwnersSpec(),
    val updatedAt: Long = 0L,
    val completedAt: Long? = null
)

enum class CapacityLimit { LOAD, ROOF, CAP }

data class LoadProfile(
    val dailyKwh: Double,
    val peakKw: Double,
    val annualConsumptionKwh: Int,
    val suggestedKwp: Double,
    val capacityByLoadKwp: Double,
    val capacityByRoofKwp: Double,
    val limitedBy: CapacityLimit,
    val expectedAnnualKwh: Int,
    val yieldPerKwp: Int
)

object LoadProfileCalc {
    val WIZARD_STEPS = listOf(
        "بخش و موقعیت",
        "انشعاب برق",
        "لوازم برقی",
        "مشخصات بام",
        "پروفایل بار",
        "مرور و ثبت"
    )

    val AZIMUTHS = listOf("جنوب", "جنوب شرقی", "جنوب غربی")

    private fun roundDigits(value: Double, digits: Int = 1): Double {
        var factor = 1.0
        repeat(digits) { factor *= 10.0 }
        return round(value * factor) / factor
    }

    fun dailyConsumptionKwh(appliances: List<ApplianceEntry>): Double =
        roundDigits(appliances.sumOf { it.ratedKw * it.count * it.hoursPerDay })

    fun coincidentPeakKw(appliances: List<ApplianceEntry>, coincidenceFactor: Double): Double =
        roundDigits(appliances.sumOf { it.ratedKw * it.count } * coincidenceFactor, 2)

    fun annualYieldPerKwp(table: RateTable = RateTables.ACTIVE): Double {
        val annual = table.scenario.monthlyProductionKwh.sum().toDouble()
        return annual / table.scenario.pilotCapacityKwp
    }

    fun buildLoadProfile(draft: ProfileDraft, table: RateTable = RateTables.ACTIVE): LoadProfile {
        val dailyKwh = dailyConsumptionKwh(draft.appliances)
        val peakKw = coincidentPeakKw(draft.appliances, table.scenario.coincidenceFactor)
        val annualConsumptionKwh = kotlin.math.round(dailyKwh * 365.0).toInt()
        val yieldPerKwp = annualYieldPerKwp(table)

        val capacityByLoadKwp = roundDigits(annualConsumptionKwh.toDouble() / yieldPerKwp, 1)
        val capacityByRoofKwp = roundDigits(draft.roof.area / table.scenario.roofAreaPerKwp, 1)
        val cap = table.loan.maxCapacityKwp

        val raw = max(0.0, min(capacityByLoadKwp, min(capacityByRoofKwp, cap)))
        val suggestedKwp = if (raw > 0.0) max(1.0, round(raw * 2.0) / 2.0) else 0.0
        val roundedYield = kotlin.math.round(yieldPerKwp).toInt()

        val limitedBy = when {
            cap <= capacityByLoadKwp && cap <= capacityByRoofKwp -> CapacityLimit.CAP
            capacityByRoofKwp <= capacityByLoadKwp -> CapacityLimit.ROOF
            else -> CapacityLimit.LOAD
        }

        return LoadProfile(
            dailyKwh = dailyKwh,
            peakKw = peakKw,
            annualConsumptionKwh = annualConsumptionKwh,
            suggestedKwp = suggestedKwp,
            capacityByLoadKwp = capacityByLoadKwp,
            capacityByRoofKwp = capacityByRoofKwp,
            limitedBy = limitedBy,
            expectedAnnualKwh = kotlin.math.round(suggestedKwp * roundedYield).toInt(),
            yieldPerKwp = roundedYield
        )
    }

    fun ownersReady(draft: ProfileDraft): Boolean =
        !draft.sharedOwnership || (draft.owners.total > 0 && draft.owners.granted >= draft.owners.total)

    fun ownerConsentPct(draft: ProfileDraft): Int =
        if (draft.owners.total <= 0) 0 else ((draft.owners.granted.toDouble() / draft.owners.total.toDouble()) * 100.0).toInt()

    fun validateStep(step: Int, draft: ProfileDraft): String? = when (step) {
        0 -> when {
            draft.sector == null -> "بخش مصرف را انتخاب کنید"
            draft.city.trim().length < 2 -> "نام شهر را وارد کنید"
            else -> null
        }
        1 -> when {
            draft.connection.meterSerial.trim().length < 6 -> "شماره کنتور را کامل وارد کنید"
            draft.connection.demandKw <= 0.0 -> "توان قراردادی انشعاب را وارد کنید"
            else -> null
        }
        2 -> if (draft.appliances.isEmpty()) "حداقل یک لوازم برقی اضافه کنید" else null
        3 -> if (draft.roof.area <= 0.0) "مساحت بام را وارد کنید" else null
        4 -> if (ownersReady(draft)) null else "رضایت همه مالکان مشاع لازم است"
        else -> null
    }
}
