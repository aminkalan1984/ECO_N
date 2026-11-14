package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.EcoCalc
import com.example.domain.PersianFormat
import com.example.domain.PlanId
import com.example.domain.RateTables
import com.example.ui.components.DropdownPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.HeroStat
import com.example.ui.components.InfoCircleButton
import com.example.ui.components.InfoRow
import com.example.ui.components.InkButton
import com.example.ui.components.PanelTitle
import com.example.ui.components.ScenarioLabel
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SetpointStep
import com.example.ui.components.TempPlanChart
import com.example.ui.components.WhiteButton
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel
import kotlin.math.roundToInt

private enum class DayHorizon(val label: String) {
    TODAY("امروز"),
    TOMORROW("فردا")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizeScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onContinueToDesign: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors
    val scenario = RateTables.ACTIVE.scenario

    var day by remember { mutableStateOf(DayHorizon.TODAY) }
    var selectedPlan by remember(ecoState.plan) { mutableStateOf(ecoState.plan) }
    var optionsOpen by remember { mutableStateOf(false) }
    var infoOpen by remember { mutableStateOf(false) }

    val planRow = scenario.optimizationPlans.first { it.id == selectedPlan }
    val shift = if (day == DayHorizon.TOMORROW) 1.5 else 0.0
    val savingPct = if (day == DayHorizon.TOMORROW) planRow.savingPct + 2 else planRow.savingPct
    val baselineKwh = ecoState.bill.totalKwh
    val savedKwh = ((baselineKwh * savingPct) / 100.0).roundToInt()

    val steps = remember(planRow) {
        listOf(
            SetpointStep(0.0, 10.0, 26),
            SetpointStep(10.0, 16.5, 25),
            SetpointStep(16.5, 19.0, planRow.preCoolTemp),
            SetpointStep(19.0, 23.0, planRow.peakTemp),
            SetpointStep(23.0, 24.0, 26)
        )
    }

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "بهینه‌سازی مصرف",
                subtitle = "سرویس ۱ هوش مصنوعی",
                onStartClick = onBack
            )

            HeroStat(
                label = "صرفه‌جویی برآوردی ماهانه",
                value = "${PersianFormat.fa(savingPct)}٪",
                unit = "حدود ${PersianFormat.fa(savedKwh)} kWh در ماه",
                aside = {
                    DropdownPill(
                        value = day,
                        options = DayHorizon.entries.map { it to it.label },
                        onChange = { day = it },
                        modifier = Modifier.testTag("optimize_day_dropdown")
                    )
                }
            )

            Spacer(Modifier.height(10.dp))
            ScenarioLabel(
                text = "مبتنی بر پیش‌بینی دمای ${if (day == DayHorizon.TODAY) "فردا" else "پس‌فردا"} · ${EcoCalc.RATE_TABLE_VERSION}"
            )

            TempPlanChart(
                steps = steps,
                shift = shift,
                preCoolTemp = planRow.preCoolTemp
            )
        },
        sunContent = {
            PanelTitle(
                title = "چرا این پیشنهاد؟",
                aside = {
                    InfoCircleButton(
                        onClick = { infoOpen = true },
                        modifier = Modifier.testTag("optimize_info_button")
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
            InfoRow(
                label = "ضریب حساسیت دمایی",
                value = "${PersianFormat.fa(scenario.temperatureSensitivity, 2)} بر هر درجه"
            )
            InfoRow(label = "منبع دما", value = "پیش‌بینی هواشناسی نجف‌آباد")
            InfoRow(
                label = "تجهیز اصلی",
                value = "کولر گازی ${PersianFormat.fa(scenario.mainApplianceKw, 1)} کیلووات"
            )
            InfoRow(
                label = "نسخه جدول نرخ",
                value = EcoCalc.RATE_TABLE_VERSION,
                showDivider = false
            )

            Spacer(Modifier.height(10.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "E = P × t × (1 + α·|T_amb − T_set|)",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InkButton(
                    text = "پذیرش برنامه",
                    onClick = {
                        ecoViewModel.acceptPlan(selectedPlan)
                        ecoViewModel.emitToast("برنامه «${planRow.name}» فعال شد")
                        onContinueToDesign()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("optimize_accept_button")
                )
                WhiteButton(
                    text = "گزینه‌های دیگر",
                    onClick = { optionsOpen = true },
                    modifier = Modifier.testTag("optimize_options_button")
                )
            }
        }
    )

    if (optionsOpen) {
        ModalBottomSheet(
            onDismissRequest = { optionsOpen = false },
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
                Text("گزینه‌های برنامه دمایی", style = MaterialTheme.typography.titleLarge)
                Text(
                    "یکی را انتخاب کنید؛ نمودار بلافاصله به‌روز می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                scenario.optimizationPlans.forEach { option ->
                    val active = option.id == selectedPlan
                    Surface(
                        onClick = {
                            selectedPlan = option.id
                            optionsOpen = false
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = if (active) BorderStroke(2.dp, colors.ink) else null,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_option_${option.id.rawId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (active) colors.ink else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = if (active) 0.dp else 2.dp,
                                        color = colors.trackStrong,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = colors.inkForeground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(option.name, style = MaterialTheme.typography.titleSmall)
                                Text(option.desc, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            }
                            Text(
                                text = "${PersianFormat.fa(option.savingPct)}٪",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

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
                Text("منطق محاسبه", style = MaterialTheme.typography.titleLarge)
                Text(
                    "مصرف کولر (E) از توان (P)، مدت کار (t) و اختلاف دمای بیرون با دمای تنظیم‌شده محاسبه می‌شود. ضریب α میزان حساسیت مصرف به هر درجه اختلاف است.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "با پیش‌سرمایش خانه پیش از ساعت اوج تعرفه (۱۹ تا ۲۳) و افزایش جزئی دما در اوج، مصرف در گران‌ترین ساعات کاهش می‌یابد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
