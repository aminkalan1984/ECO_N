package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.ChannelId
import com.example.domain.EcoCalc
import com.example.domain.PersianFormat
import com.example.ui.components.DropdownPill
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.Footnote
import com.example.ui.components.HBar
import com.example.ui.components.HeroStat
import com.example.ui.components.IconCircle
import com.example.ui.components.InfoRow
import com.example.ui.components.InkButton
import com.example.ui.components.PanelTitle
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.components.WhiteButton
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel
import kotlin.math.abs

private enum class ChannelHorizon(val years: Int, val label: String) {
    Y1(1, "سال ۱"),
    Y5(5, "۵ سال"),
    Y20(20, "۲۰ سال")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onContinueToInstallment: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var selectedId by remember(ecoState.channel) { mutableStateOf(ecoState.channel) }
    var horizon by remember { mutableStateOf(ChannelHorizon.Y1) }
    var hideDetails by remember { mutableStateOf(false) }
    var whyOpen by remember { mutableStateOf(false) }
    var sensitivityOpen by remember { mutableStateOf(false) }

    val selectedSummary = EcoCalc.CHANNELS.first { it.channel.id == selectedId }
    val bestSummary = EcoCalc.CHANNELS.maxBy { it.annualMillion }
    val maxAnnual = EcoCalc.CHANNELS.maxOf { it.annualMillion }.coerceAtLeast(1.0)
    val totalMillion = selectedSummary.annualMillion * horizon.years

    val heroVal = if (totalMillion >= 10_000.0) {
        PersianFormat.fa(totalMillion / 1000.0, 1)
    } else {
        PersianFormat.fa(totalMillion, 1)
    }
    val heroUnit = when {
        totalMillion >= 10_000.0 -> "میلیارد ریال در ${PersianFormat.fa(horizon.years)} سال"
        horizon.years == 1 -> "میلیون ریال در سال"
        else -> "میلیون ریال در ${PersianFormat.fa(horizon.years)} سال"
    }

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "کانال فروش برق",
                subtitle = "نسخه نرخ: آبان ۱۴۰۵",
                onStartClick = onBack
            )

            HeroStat(
                label = if (selectedId == bestSummary.channel.id) "بهترین گزینه مجاز برای انشعاب شما"
                else "درآمد کانال ${selectedSummary.channel.name}",
                value = heroVal,
                unit = heroUnit,
                aside = {
                    DropdownPill(
                        value = horizon,
                        options = ChannelHorizon.entries.map { it to it.label },
                        onChange = { horizon = it },
                        modifier = Modifier.testTag("channel_horizon_dropdown")
                    )
                }
            )

            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                EcoCalc.CHANNELS.forEach { summary ->
                    val ch = summary.channel
                    val active = ch.id == selectedId
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = ch.id }
                            .padding(vertical = 2.dp)
                            .testTag("channel_option_${ch.id.rawId}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = ch.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (ch.id == bestSummary.channel.id) {
                                    EcoPill(text = "پیشنهاد", tone = PillTone.INK, small = true)
                                }
                            }
                            Text(
                                text = "${PersianFormat.fa(summary.annualMillion * horizon.years, 1)} م.ر",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HBar(
                            ratio = summary.annualMillion / maxAnnual,
                            fillColor = when {
                                active -> colors.sunDeep
                                ch.isFeedInTariff -> colors.sun
                                else -> colors.trackStrong
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.warnSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = colors.warn,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "خرید تضمینی و خودتأمین روی یک انشعاب ممکن نیست",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "یکی را انتخاب کنید یا انشعاب جداگانه بگیرید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                        )
                    }
                }
            }
        },
        sunContent = {
            val ch = selectedSummary.channel
            PanelTitle(
                title = "جزئیات کانال منتخب",
                aside = {
                    IconCircle(
                        onClick = { hideDetails = !hideDetails },
                        contentDescription = if (hideDetails) "نمایش جزئیات" else "پنهان کردن جزئیات",
                        modifier = Modifier.testTag("channel_toggle_details")
                    ) {
                        Icon(
                            imageVector = if (hideDetails) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )

            if (!hideDetails) {
                Spacer(Modifier.height(8.dp))
                InfoRow("نرخ قرارداد", "${PersianFormat.fa(ch.rateRialPerKwh, false)} ریال بر kWh")
                InfoRow(
                    "مدت قرارداد",
                    if (ch.contractYears > 1) "${PersianFormat.fa(ch.contractYears)} سال با تعدیل دوره‌ای" else "سالانه، قابل تمدید"
                )
                InfoRow(
                    label = if (ch.isFeedInTariff) "تأخیر پرداخت ساتبا" else "زمان دریافت منفعت",
                    value = ch.paymentDelay ?: "هم‌زمان با قبض",
                    badge = ch.paymentDelay?.let {
                        { EcoPill(text = "ریسک نقدینگی", tone = PillTone.WARN, small = true) }
                    }
                )
                InfoRow("شرط فعال‌سازی", ch.activation, showDivider = false)
            }

            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = { whyOpen = true },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().testTag("channel_why_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = "چرا این کانال؟ ورودی‌ها، فرضیات و گزینه‌های جایگزین",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InkButton(
                    text = "انتخاب این کانال",
                    onClick = {
                        ecoViewModel.chooseChannel(selectedId)
                        ecoViewModel.emitToast("کانال «${ch.name}» انتخاب شد")
                        onContinueToInstallment()
                    },
                    modifier = Modifier.weight(1f).testTag("channel_select_button")
                )
                WhiteButton(
                    text = "تحلیل حساسیت",
                    onClick = { sensitivityOpen = true },
                    modifier = Modifier.testTag("channel_sensitivity_button")
                )
            }

            Footnote(
                text = "اعداد براساس تولید ${PersianFormat.fa(EcoCalc.ANNUAL_PRODUCTION_KWH)} kWh و نسخه فعال جدول نرخ است"
            )
        }
    )

    if (whyOpen) {
        ModalBottomSheet(
            onDismissRequest = { whyOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .navigationBarsPadding()
            ) {
                Text("چرا این کانال؟", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "مسیریاب کانال، گزینه‌ها را فقط پیشنهاد می‌دهد؛ تصمیم نهایی با شماست.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        InfoRow("تولید سالانه", "${PersianFormat.fa(EcoCalc.ANNUAL_PRODUCTION_KWH)} kWh")
                        InfoRow("نوع انشعاب", "خانگی، تک‌فاز")
                        InfoRow("باتری در طرح", if (ecoState.battery) "دارد" else "ندارد")
                        InfoRow("نسخه جدول نرخ", "آبان ۱۴۰۵", showDivider = false)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "کانال باتری‌دار بیشترین درآمد را دارد اما نیازمند حداقل ۷,۵ کیلووات‌ساعت باتری است. خرید تضمینی پایه بدون باتری فعال می‌شود. خودتأمین درآمد نقدی ندارد و به‌صورت کاهش قبض است.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (sensitivityOpen) {
        ModalBottomSheet(
            onDismissRequest = { sensitivityOpen = false },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("تحلیل حساسیت", style = MaterialTheme.typography.titleLarge)
                Text(
                    "درآمد سالانه ${selectedSummary.channel.name} با تغییر تولید",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                listOf(-20, -10, 0, 10, 20).forEach { delta ->
                    val valM = selectedSummary.annualMillion * (1.0 + delta / 100.0)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (delta == 0) "تولید مبنا"
                                else "تولید ${if (delta > 0) "+" else "−"}${PersianFormat.fa(abs(delta))}٪",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${PersianFormat.fa(valM, 1)} م.ر",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        HBar(
                            ratio = valM / (selectedSummary.annualMillion * 1.2),
                            fillColor = if (delta == 0) colors.ink else colors.sunDeep
                        )
                    }
                }
            }
        }
    }
}
