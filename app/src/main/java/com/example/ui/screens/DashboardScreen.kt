package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.EcoCalc
import com.example.domain.PersianFormat
import com.example.domain.RateTables
import com.example.ui.components.ActionCard
import com.example.ui.components.BarDatum
import com.example.ui.components.DailyGenerationTrendChart
import com.example.ui.components.DataFreshness
import com.example.ui.components.DropdownPill
import com.example.ui.components.EcoBarChart
import com.example.ui.components.EcoBottomNav
import com.example.ui.components.EcoNavTab
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.HeroStat
import com.example.ui.components.InfoRow
import com.example.ui.components.PanelTitle
import com.example.ui.components.PillTone
import com.example.ui.components.ScenarioLabel
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToLong

private enum class TelemetryRange(val heroLabel: String, val pillLabel: String) {
    DAY("تولید امروز", "روز"),
    WEEK("تولید این هفته", "هفته"),
    MONTH("تولید این ماه", "ماه")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    ecoViewModel: EcoViewModel,
    onNavigate: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val telemetry by ecoViewModel.telemetry.collectAsStateWithLifecycle()
    val revisions by ecoViewModel.billRevisions.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var range by remember { mutableStateOf(TelemetryRange.DAY) }
    var hideMoney by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(false) }
    var selectedBarIdx by remember { mutableStateOf<Int?>(null) }

    val produced = when (range) {
        TelemetryRange.DAY -> telemetry.todayKwh
        TelemetryRange.WEEK -> telemetry.weekKwh
        TelemetryRange.MONTH -> telemetry.monthKwh.toDouble()
    }

    val latestConfirmedRev = revisions.lastOrNull { it.confirmed }
    val activeRate = EcoCalc.channelRate(ecoState.channel)
    val ledgerKwh = latestConfirmedRev?.let { it.peakKwh + it.midKwh + it.lowKwh } ?: 0
    val ledgerAmountRial = (ledgerKwh.toDouble() * activeRate.toDouble()).roundToLong()
    val rangeIncomeRial = ((produced / max(1, telemetry.monthKwh).toDouble()) * ledgerAmountRial).roundToLong()

    val chartData = when (range) {
        TelemetryRange.WEEK -> telemetry.weekSeries.map { BarDatum(it.label, it.value) }
        else -> telemetry.monthSeries.map { BarDatum(it.label, it.value) }
    }
    val chartStep = if (range == TelemetryRange.WEEK) 5 else 100
    val maxVal = chartData.maxOfOrNull { it.value } ?: chartStep.toDouble()
    val chartMax = max(chartStep, (ceil(maxVal / chartStep).toInt() * chartStep))

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "نیروگاه من",
                subtitle = "${PersianFormat.fa(telemetry.capacityKwp)} کیلووات | نجف‌آباد",
                useUserAvatar = true,
                onStartClick = { onNavigate("profile") },
                onOpenNotifications = onOpenNotifications,
                onOpenMenu = onOpenMenu
            )

            HeroStat(
                label = range.heroLabel,
                value = PersianFormat.fa(produced, 1),
                unit = "kWh",
                aside = {
                    DropdownPill(
                        value = range,
                        options = TelemetryRange.entries.map { it to it.pillLabel },
                        onChange = {
                            range = it
                            selectedBarIdx = null
                        },
                        modifier = Modifier.testTag("dashboard_range_dropdown")
                    )
                }
            )

            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataFreshness(
                    state = telemetry.state,
                    ageMinutes = telemetry.ageMinutes,
                    source = telemetry.sourceLabel
                )
                if (telemetry.isScenario) {
                    ScenarioLabel(text = "تلمتری شبیه‌سازی‌شده · نسخه ${telemetry.rateTableVersion}")
                }
            }

            if (range == TelemetryRange.DAY) {
                Spacer(Modifier.height(14.dp))
                DailyGenerationTrendChart(
                    peakKw = telemetry.pLiveKw,
                    todayKwh = telemetry.todayKwh,
                    weekSeries = telemetry.weekSeries,
                    performanceRatioPct = telemetry.performanceRatioPct
                )
            } else {
                EcoBarChart(
                    data = chartData,
                    max = chartMax.toDouble(),
                    ticks = listOf(0, chartMax / 2, chartMax),
                    selectedIndex = selectedBarIdx,
                    onSelect = { selectedBarIdx = it },
                    tooltipText = { _, datum -> "${PersianFormat.fa(datum.value, 1)} kWh" },
                    chartHeightDp = 170
                )
            }
        },
        sunContent = {
            PanelTitle(
                title = "جریان انرژی امروز",
                aside = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { collapsed = !collapsed },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dashboard_collapse_toggle")
                            ) {
                                Icon(
                                    imageVector = if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                    contentDescription = if (collapsed) "باز کردن" else "جمع کردن"
                                )
                            }
                            IconButton(
                                onClick = { hideMoney = !hideMoney },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dashboard_hide_money_toggle")
                            ) {
                                Icon(
                                    imageVector = if (hideMoney) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (hideMoney) "نمایش مبالغ" else "پنهان کردن مبالغ"
                                )
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = "داده اینورتر برای پایش است؛ مبنای صورتحساب کنتور رسمی توزیع است",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )

            if (!collapsed) {
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "تولید اینورتر",
                    value = "${PersianFormat.fa(produced, 1)} kWh"
                )
                InfoRow(
                    label = "درآمد برآوردی این بازه",
                    value = if (hideMoney) "••••••" else "${PersianFormat.fa(rangeIncomeRial, false)} ریال",
                    badge = { EcoPill(text = "برآورد", tone = PillTone.WHITE, small = true) }
                )
                InfoRow(
                    label = "بازده عملکرد PR",
                    value = "${PersianFormat.fa(telemetry.performanceRatioPct)}٪",
                    showDivider = false
                )
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "خلاصه درآمد از دفتر",
                            style = MaterialTheme.typography.titleSmall
                        )
                        EcoPill(
                            text = if (latestConfirmedRev != null) "نسخه ${RateTables.ACTIVE.version}" else "بدون قبض",
                            tone = PillTone.WHITE,
                            small = true
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    if (latestConfirmedRev != null) {
                        InfoRow(
                            label = "مبنای تحلیل",
                            value = "نسخه ${PersianFormat.fa(latestConfirmedRev.version)} · ${PersianFormat.fa(ledgerKwh)} kWh"
                        )
                        InfoRow(
                            label = "برآورد درآمد دوره",
                            value = if (hideMoney) "••••••" else "${PersianFormat.fa(ledgerAmountRial, false)} ریال"
                        )
                        InfoRow(
                            label = "نرخ کانال فعال",
                            value = "${PersianFormat.fa(activeRate, false)} ریال",
                            showDivider = false
                        )
                        Text(
                            text = "محاسبه: مصرف آخرین قبض تأییدشده × نرخ کانال فعال",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText
                        )
                    } else {
                        Text(
                            text = "هنوز قبض تأییدشده‌ای در دفتر نیست؛ پس از تأیید قبض، درآمد برآوردی از دفتر خوانده می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                        ActionCard(
                            icon = Icons.Filled.History,
                            title = "ثبت نسخه قبض",
                            subtitle = "تاریخچه نسخه‌ها و انتخاب مبنای تحلیل",
                            onClick = { onNavigate("bills") },
                            modifier = Modifier.testTag("dashboard_register_bill_card")
                        )
                    }
                }
            }

            ActionCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "اسکن دوربین و تحلیل قبض برق",
                subtitle = "عکسبرداری از قبض برای استخراج مصرف و برآورد صرفه‌جویی خورشیدی",
                onClick = { onNavigate("bill") },
                modifier = Modifier.testTag("dashboard_bill_card")
            )

            ActionCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "قرائت کنتور مهر رسید",
                subtitle = "تطبیق سه‌طرفه و صورتحساب را ببینید",
                onClick = { onNavigate("income") },
                modifier = Modifier.testTag("dashboard_income_card")
            )

            ActionCard(
                icon = Icons.Filled.AutoAwesome,
                title = "دستیار هوش مصنوعی",
                subtitle = "گفتگو درباره قبض، باتری، کانال درآمد و مراحل کار",
                onClick = { onNavigate("assistant") },
                modifier = Modifier.testTag("dashboard_assistant_card")
            )
        },
        bottomNav = {
            EcoBottomNav(
                active = EcoNavTab.HOME,
                onSelect = { tab -> onNavigate(tab.routeKey) }
            )
        }
    )
}
