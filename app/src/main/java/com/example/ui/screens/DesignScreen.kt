package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.EcoCalc
import com.example.domain.PersianFormat
import com.example.domain.RateTables
import com.example.ui.components.BarDatum
import com.example.ui.components.DropdownPill
import com.example.ui.components.EcoBarChart
import com.example.ui.components.EcoBottomNav
import com.example.ui.components.EcoNavTab
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.EcoSwitch
import com.example.ui.components.HeroStat
import com.example.ui.components.InfoCircleButton
import com.example.ui.components.InfoRow
import com.example.ui.components.InkButton
import com.example.ui.components.PanelTitle
import com.example.ui.components.PillTone
import com.example.ui.components.ScenarioLabel
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel
import kotlin.math.ceil
import kotlin.math.roundToInt

private enum class DesignDetailMode(val label: String) {
    SIMPLE("ساده"),
    DETAILED("دقیق")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DesignScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors
    val table = RateTables.ACTIVE
    val scenario = table.scenario

    var mode by remember { mutableStateOf(DesignDetailMode.SIMPLE) }
    var selectedMonth by remember { mutableIntStateOf(4) }
    var infoOpen by remember { mutableStateOf(false) }

    val panelCount = ceil((scenario.pilotCapacityKwp * 1000.0) / scenario.panelWatt).toInt()
    val requiredArea = (scenario.pilotCapacityKwp * scenario.roofAreaPerKwp).roundToInt()
    val monthlyData = remember(scenario.monthlyProductionKwh) {
        scenario.monthlyProductionKwh.mapIndexed { idx, v ->
            BarDatum(PersianFormat.faDigits(idx + 1), v.toDouble())
        }
    }
    val maxVal = scenario.monthlyProductionKwh.maxOrNull() ?: 800
    val chartMax = ceil(maxVal / 100.0).toInt() * 100

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "طرح پیشنهادی نیروگاه",
                subtitle = "سرویس ۲ هوش مصنوعی",
                onStartClick = onBack
            )

            HeroStat(
                label = "ظرفیت پیشنهادی",
                value = PersianFormat.fa(scenario.pilotCapacityKwp),
                unit = "کیلووات پیک",
                aside = {
                    DropdownPill(
                        value = mode,
                        options = DesignDetailMode.entries.map { it to it.label },
                        onChange = { mode = it },
                        modifier = Modifier.testTag("design_mode_dropdown")
                    )
                }
            )

            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EcoPill(
                    text = "تولید سالانه ${PersianFormat.fa(EcoCalc.ANNUAL_PRODUCTION_KWH)} kWh",
                    tone = PillTone.SUN
                )
                EcoPill(
                    text = "سایه‌اندازی ${PersianFormat.fa(scenario.shadowLossPct)}٪",
                    tone = PillTone.WHITE
                )
                ScenarioLabel(text = table.label)
            }

            EcoBarChart(
                data = monthlyData,
                max = chartMax.toDouble(),
                ticks = listOf(0, chartMax / 2, chartMax),
                selectedIndex = selectedMonth,
                onSelect = { selectedMonth = it },
                tooltipText = { idx, datum ->
                    val monthName = PersianFormat.SHAMSI_MONTHS.getOrElse(idx) { "" }
                    "$monthName ${PersianFormat.fa(datum.value)} kWh"
                },
                footLabel = "ماه شمسی"
            )
        },
        sunContent = {
            PanelTitle(
                title = "مشخصات طرح",
                aside = {
                    InfoCircleButton(
                        onClick = { infoOpen = true },
                        modifier = Modifier.testTag("design_info_button")
                    )
                }
            )
            Spacer(Modifier.height(6.dp))
            InfoRow("پنل", "${PersianFormat.fa(panelCount)} عدد ${PersianFormat.fa(scenario.panelWatt)} وات")
            InfoRow(
                "اینورتر",
                "${if (ecoState.battery) "هیبرید" else "متصل به شبکه"} ${PersianFormat.fa(scenario.inverterKw)} کیلووات، ۲ MPPT"
            )
            InfoRow(
                "مساحت لازم بام",
                "${PersianFormat.fa(requiredArea)} از ${PersianFormat.fa(scenario.roofAreaAvailable)} متر مربع"
            )
            InfoRow(
                label = "جهت و شیب",
                value = "${scenario.azimuth}، ${PersianFormat.fa(scenario.tiltDeg)} درجه",
                showDivider = mode == DesignDetailMode.DETAILED
            )
            if (mode == DesignDetailMode.DETAILED) {
                InfoRow("تلفات کل سیستم", "${PersianFormat.fa(scenario.systemLossPct)}٪")
                InfoRow("ضریب عملکرد PR", "${PersianFormat.fa((scenario.performanceRatio * 100).roundToInt())}٪")
                InfoRow(
                    label = "ساعات آفتابی معادل",
                    value = "${PersianFormat.fa(scenario.peakSunHours, 1)} ساعت در روز",
                    showDivider = false
                )
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.sunSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BatteryChargingFull,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("نسخه باتری‌دار", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "حداقل ${PersianFormat.fa(EcoCalc.MIN_BATTERY_KWH, 1)} kWh باتری، شرط نرخ ${PersianFormat.fa(EcoCalc.BATTERY_FIT_RATE, false)} ریال",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                        Text(
                            text = "شارژ باتری از شبکه مجاز نیست",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText.copy(alpha = 0.8f)
                        )
                    }
                    EcoSwitch(
                        checked = ecoState.battery,
                        onCheckedChange = { enabled ->
                            ecoViewModel.setBattery(enabled)
                            ecoViewModel.emitToast(
                                if (enabled) "نسخه باتری‌دار فعال شد" else "نسخه بدون باتری انتخاب شد"
                            )
                        },
                        modifier = Modifier.testTag("design_battery_switch")
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            InkButton(
                text = "مقایسه کانال‌های درآمد",
                onClick = { onNavigate("channel") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("design_compare_channels_button")
            )
        },
        bottomNav = {
            EcoBottomNav(
                active = EcoNavTab.DESIGN,
                onSelect = { onNavigate(it.routeKey) }
            )
        }
    )

    if (infoOpen) {
        ModalBottomSheet(
            onDismissRequest = { infoOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("طرح چگونه محاسبه شد؟", style = MaterialTheme.typography.titleLarge)
                Text(
                    "ظرفیت بر اساس مصرف سالانه قبض، مساحت قابل استفاده بام و تابش خورشید در نجف‌آباد انتخاب شده است.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "تولید ماهانه با در نظر گرفتن ${PersianFormat.fa(scenario.shadowLossPct)}٪ سایه‌اندازی و ${PersianFormat.fa(scenario.systemLossPct)}٪ تلفات سیستم برآورد شده است.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "همه این مقادیر بخشی از سناریوی نسخه ${table.version} جدول نرخ هستند و با تغییر نسخه به‌روز می‌شوند.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
