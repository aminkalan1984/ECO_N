package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.ChannelId
import com.example.domain.PlanId
import com.example.domain.RateTables
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ecoDataStore by preferencesDataStore(name = "ecosolar_prefs")

data class BillData(
    val id: String = "7810332190365",
    val period: String = "مرداد تا مهر ۱۴۰۵",
    val peak: Int = 312,
    val mid: Int = 638,
    val low: Int = 297,
    val total: Long = 1_803_500_000L
) {
    val totalKwh: Int get() = peak + mid + low
}

data class LoanSpec(
    val amount: Long = RateTables.ACTIVE.loan.amountRial,
    val rate: Double = RateTables.ACTIVE.loan.annualRatePct,
    val months: Int = RateTables.ACTIVE.loan.months
)

data class EcoState(
    val phone: String = "09133457781",
    val loggedIn: Boolean = false,
    val bill: BillData = BillData(),
    val billRevision: Int = 1,
    val lowConfidenceReviewed: Boolean = false,
    val plan: PlanId = PlanId.BALANCED,
    val planAccepted: Boolean = false,
    val battery: Boolean = false,
    val channel: ChannelId = ChannelId.FIT_BATTERY,
    val channelConfirmed: Boolean = false,
    val loan: LoanSpec = LoanSpec(),
    val strategy: Int? = null,
    val quoteId: String? = "a",
    val orderPlaced: Boolean = false,
    val visitConfirmed: Boolean = false,
    val paidMonths: Set<String> = emptySet()
)

class EcoPreferences(private val context: Context) {
    private object Keys {
        val PHONE = stringPreferencesKey("phone")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val BILL_ID = stringPreferencesKey("bill_id")
        val BILL_PERIOD = stringPreferencesKey("bill_period")
        val BILL_PEAK = intPreferencesKey("bill_peak")
        val BILL_MID = intPreferencesKey("bill_mid")
        val BILL_LOW = intPreferencesKey("bill_low")
        val BILL_TOTAL = longPreferencesKey("bill_total")
        val BILL_REV = intPreferencesKey("bill_revision")
        val LOW_CONF_REVIEWED = booleanPreferencesKey("low_conf_reviewed")
        val PLAN = stringPreferencesKey("plan")
        val PLAN_ACCEPTED = booleanPreferencesKey("plan_accepted")
        val BATTERY = booleanPreferencesKey("battery")
        val CHANNEL = stringPreferencesKey("channel")
        val CHANNEL_CONFIRMED = booleanPreferencesKey("channel_confirmed")
        val LOAN_AMOUNT = longPreferencesKey("loan_amount")
        val LOAN_RATE = doublePreferencesKey("loan_rate")
        val LOAN_MONTHS = intPreferencesKey("loan_months")
        val STRATEGY = intPreferencesKey("strategy")
        val QUOTE_ID = stringPreferencesKey("quote_id")
        val ORDER_PLACED = booleanPreferencesKey("order_placed")
        val VISIT_CONFIRMED = booleanPreferencesKey("visit_confirmed")
        val PAID_MONTHS = stringSetPreferencesKey("paid_months")
    }

    val stateFlow: Flow<EcoState> = context.ecoDataStore.data.map { prefs ->
        val def = EcoState()
        EcoState(
            phone = prefs[Keys.PHONE] ?: def.phone,
            loggedIn = prefs[Keys.LOGGED_IN] ?: def.loggedIn,
            bill = BillData(
                id = prefs[Keys.BILL_ID] ?: def.bill.id,
                period = prefs[Keys.BILL_PERIOD] ?: def.bill.period,
                peak = prefs[Keys.BILL_PEAK] ?: def.bill.peak,
                mid = prefs[Keys.BILL_MID] ?: def.bill.mid,
                low = prefs[Keys.BILL_LOW] ?: def.bill.low,
                total = prefs[Keys.BILL_TOTAL] ?: def.bill.total
            ),
            billRevision = prefs[Keys.BILL_REV] ?: def.billRevision,
            lowConfidenceReviewed = prefs[Keys.LOW_CONF_REVIEWED] ?: def.lowConfidenceReviewed,
            plan = PlanId.fromRaw(prefs[Keys.PLAN] ?: def.plan.rawId),
            planAccepted = prefs[Keys.PLAN_ACCEPTED] ?: def.planAccepted,
            battery = prefs[Keys.BATTERY] ?: def.battery,
            channel = ChannelId.fromRaw(prefs[Keys.CHANNEL] ?: def.channel.rawId),
            channelConfirmed = prefs[Keys.CHANNEL_CONFIRMED] ?: def.channelConfirmed,
            loan = LoanSpec(
                amount = prefs[Keys.LOAN_AMOUNT] ?: def.loan.amount,
                rate = prefs[Keys.LOAN_RATE] ?: def.loan.rate,
                months = prefs[Keys.LOAN_MONTHS] ?: def.loan.months
            ),
            strategy = prefs[Keys.STRATEGY]?.takeIf { it in 1..3 },
            quoteId = prefs[Keys.QUOTE_ID] ?: def.quoteId,
            orderPlaced = prefs[Keys.ORDER_PLACED] ?: def.orderPlaced,
            visitConfirmed = prefs[Keys.VISIT_CONFIRMED] ?: def.visitConfirmed,
            paidMonths = prefs[Keys.PAID_MONTHS] ?: def.paidMonths
        )
    }

    suspend fun update(transform: (EcoState) -> EcoState) {
        context.ecoDataStore.edit { prefs ->
            val current = EcoState(
                phone = prefs[Keys.PHONE] ?: "09133457781",
                loggedIn = prefs[Keys.LOGGED_IN] ?: false,
                bill = BillData(
                    id = prefs[Keys.BILL_ID] ?: "7810332190365",
                    period = prefs[Keys.BILL_PERIOD] ?: "مرداد تا مهر ۱۴۰۵",
                    peak = prefs[Keys.BILL_PEAK] ?: 312,
                    mid = prefs[Keys.BILL_MID] ?: 638,
                    low = prefs[Keys.BILL_LOW] ?: 297,
                    total = prefs[Keys.BILL_TOTAL] ?: 1_803_500_000L
                ),
                billRevision = prefs[Keys.BILL_REV] ?: 1,
                lowConfidenceReviewed = prefs[Keys.LOW_CONF_REVIEWED] ?: false,
                plan = PlanId.fromRaw(prefs[Keys.PLAN] ?: PlanId.BALANCED.rawId),
                planAccepted = prefs[Keys.PLAN_ACCEPTED] ?: false,
                battery = prefs[Keys.BATTERY] ?: false,
                channel = ChannelId.fromRaw(prefs[Keys.CHANNEL] ?: ChannelId.FIT_BATTERY.rawId),
                channelConfirmed = prefs[Keys.CHANNEL_CONFIRMED] ?: false,
                loan = LoanSpec(
                    amount = prefs[Keys.LOAN_AMOUNT] ?: RateTables.ACTIVE.loan.amountRial,
                    rate = prefs[Keys.LOAN_RATE] ?: RateTables.ACTIVE.loan.annualRatePct,
                    months = prefs[Keys.LOAN_MONTHS] ?: RateTables.ACTIVE.loan.months
                ),
                strategy = prefs[Keys.STRATEGY]?.takeIf { it in 1..3 },
                quoteId = prefs[Keys.QUOTE_ID] ?: "a",
                orderPlaced = prefs[Keys.ORDER_PLACED] ?: false,
                visitConfirmed = prefs[Keys.VISIT_CONFIRMED] ?: false,
                paidMonths = prefs[Keys.PAID_MONTHS] ?: emptySet()
            )
            val next = transform(current)
            prefs[Keys.PHONE] = next.phone
            prefs[Keys.LOGGED_IN] = next.loggedIn
            prefs[Keys.BILL_ID] = next.bill.id
            prefs[Keys.BILL_PERIOD] = next.bill.period
            prefs[Keys.BILL_PEAK] = next.bill.peak
            prefs[Keys.BILL_MID] = next.bill.mid
            prefs[Keys.BILL_LOW] = next.bill.low
            prefs[Keys.BILL_TOTAL] = next.bill.total
            prefs[Keys.BILL_REV] = next.billRevision
            prefs[Keys.LOW_CONF_REVIEWED] = next.lowConfidenceReviewed
            prefs[Keys.PLAN] = next.plan.rawId
            prefs[Keys.PLAN_ACCEPTED] = next.planAccepted
            prefs[Keys.BATTERY] = next.battery
            prefs[Keys.CHANNEL] = next.channel.rawId
            prefs[Keys.CHANNEL_CONFIRMED] = next.channelConfirmed
            prefs[Keys.LOAN_AMOUNT] = next.loan.amount
            prefs[Keys.LOAN_RATE] = next.loan.rate
            prefs[Keys.LOAN_MONTHS] = next.loan.months
            if (next.strategy != null) prefs[Keys.STRATEGY] = next.strategy else prefs.remove(Keys.STRATEGY)
            if (next.quoteId != null) prefs[Keys.QUOTE_ID] = next.quoteId else prefs.remove(Keys.QUOTE_ID)
            prefs[Keys.ORDER_PLACED] = next.orderPlaced
            prefs[Keys.VISIT_CONFIRMED] = next.visitConfirmed
            prefs[Keys.PAID_MONTHS] = next.paidMonths
        }
    }

    suspend fun reset() {
        context.ecoDataStore.edit { it.clear() }
    }
}
