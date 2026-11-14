package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.EcoCalc
import com.example.domain.PersianFormat
import com.example.ui.components.DropdownPill
import com.example.ui.components.EcoBottomNav
import com.example.ui.components.EcoNavTab
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.Footnote
import com.example.ui.components.HeroStat
import com.example.ui.components.InfoRow
import com.example.ui.components.InkButton
import com.example.ui.components.PanelTitle
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.components.WhiteButton
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel
import kotlin.math.abs

private enum class IncomeMonthKey(
    val persianName: String,
    val inverterKwh: Int,
    val meterKwh: Int,
    val issuedDate: String,
    val dayOfIssue: Int
) {
    MEHR("مهر", 657, 641, "۵ آبان ۱۴۰۵", 11),
    SHAHRIVAR("شهریور", 742, 731, "۴ مهر ۱۴۰۵", 41),
    MORDAD("مرداد", 815, 768, "۳ شهریور ۱۴۰۵", 72)
}

private data class ReconcileBar(
    val label: String,
    val sub: String,
    val value: Int,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var month by remember { mutableStateOf(IncomeMonthKey.MEHR) }
    var confirmOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }

    val diffPct = EcoCalc.meterDiscrepancyPct(month.inverterKwh, month.meterKwh)
    val locked = EcoCalc.canLockEnergy(month.inverterKwh, month.meterKwh)
    val billingKwh = if (locked) month.meterKwh else 0
    val lockedRate = EcoCalc.channelRate(ecoState.channel)
    val grossRial = EcoCalc.invoiceGross(month.meterKwh, lockedRate)
    val paid = month.name.lowercase() in ecoState.paidMonths

    val bars = listOf(
        ReconcileBar("اینورتر", "پایش فنی", month.inverterKwh, colors.sun),
        ReconcileBar("کنتور توزیع", "مرجع", month.meterKwh, colors.sunDeep),
        ReconcileBar("مبنای صورتحساب", if (locked) "قفل‌شده" else "در انتظار بررسی", billingKwh, colors.ink)
    )
    val maxScale = 850.0

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "درآمد ${month.persianName} ۱۴۰۵",
                subtitle = "صورتحساب خرید تضمینی",
                onStartClick = onBack
            )

            HeroStat(
                label = "مبلغ قابل دریافت از ساتبا",
                value = PersianFormat.faFixed(grossRial / 1_000_000.0, 2),
                unit = "میلیون ریال",
                aside = {
                    DropdownPill(
                        value = month,
                        options = IncomeMonthKey.entries.map { it to it.persianName },
                        onChange = { month = it },
                        modifier = Modifier.testTag("income_month_dropdown")
                    )
                }
            )

            Spacer(Modifier.height(10.dp))
            EcoPill(
                text = "مبنا: کنتور رسمی توزیع، ${PersianFormat.fa(month.meterKwh)} kWh",
                tone = PillTone.SUN
            )

            Spacer(Modifier.height(18.dp))
            Text("kWh", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.width(30.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(PersianFormat.fa(700), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                    Text(PersianFormat.fa(350), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                    Text(PersianFormat.fa(0), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                }

                Row(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    bars.forEach { b ->
                        val ratio = (b.value / maxScale).toFloat().coerceIn(0.02f, 1f)
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = PersianFormat.fa(b.value),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .fillMaxHeight(ratio * 0.85f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(b.color)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 38.dp)) {
                bars.forEach { b ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(b.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        Text(b.sub, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = colors.mutedText, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (locked) {
                    EcoPill(
                        text = "اختلاف ${PersianFormat.fa(abs(diffPct), 1)}٪ در محدوده مجاز، انرژی قفل شد",
                        tone = PillTone.SUCCESS,
                        leadingIcon = Icons.Filled.Check
                    )
                } else {
                    EcoPill(
                        text = "اختلاف ${PersianFormat.fa(abs(diffPct), 1)}٪ خارج از محدوده؛ نیاز به بررسی",
                        tone = PillTone.DANGER,
                        leadingIcon = Icons.Filled.WarningAmber
                    )
                }
            }
        },
        sunContent = {
            PanelTitle(
                title = "صورتحساب",
                aside = {
                    EcoPill(
                        text = if (paid) "پرداخت‌شده" else "صادرشده",
                        tone = if (paid) PillTone.SUCCESS else PillTone.INK,
                        small = true
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
            InfoRow("انرژی مبنا", "${PersianFormat.fa(month.meterKwh)} kWh")
            InfoRow("نرخ قفل‌شده قرارداد", "${PersianFormat.fa(lockedRate, false)} ریال")
            InfoRow("مبلغ ناخالص", "${PersianFormat.fa(grossRial, false)} ریال")
            InfoRow("تاریخ صدور", month.issuedDate, showDivider = false)

            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = if (paid) colors.success else colors.warn,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (paid) "وجه دریافت و تأیید شد"
                        else "روز ${PersianFormat.faDigits(month.dayOfIssue)} از صدور | تأخیر معمول ساتبا ۲ تا ۳ ماه",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (paid) colors.success else colors.warn
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InkButton(
                    text = if (paid) "دریافت تأیید شد" else "تأیید دریافت وجه",
                    enabled = !paid,
                    onClick = { confirmOpen = true },
                    modifier = Modifier.weight(1f).testTag("income_confirm_paid_button")
                )
                WhiteButton(
                    text = "گزارش مغایرت",
                    onClick = { reportOpen = true },
                    modifier = Modifier.testTag("income_report_discrepancy_button")
                )
            }
            Footnote(text = "واریز مستقیم ساتبا به شبای شما؛ اکوسولار وجهی نگه نمی‌دارد")
        },
        bottomNav = {
            EcoBottomNav(
                active = EcoNavTab.INCOME,
                onSelect = { onNavigate(it.routeKey) }
            )
        }
    )

    if (confirmOpen) {
        ModalBottomSheet(
            onDismissRequest = { confirmOpen = false },
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
                Text("تأیید دریافت وجه", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "آیا مبلغ ${PersianFormat.fa(grossRial)} ریال بابت صورتحساب ${month.persianName} به حساب شما واریز شده است؟",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InkButton(
                        text = "بله، دریافت کردم",
                        onClick = {
                            ecoViewModel.markMonthPaid(month.name.lowercase())
                            ecoViewModel.emitToast("دریافت وجه ${month.persianName} ثبت شد")
                            confirmOpen = false
                        },
                        modifier = Modifier.weight(1f).testTag("confirm_payment_yes_button")
                    )
                    WhiteButton(
                        text = "هنوز نه",
                        containerColor = colors.sun,
                        onClick = { confirmOpen = false }
                    )
                }
            }
        }
    }

    if (reportOpen) {
        ModalBottomSheet(
            onDismissRequest = { reportOpen = false },
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
                Text("گزارش مغایرت", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "مغایرت را شرح دهید؛ کارشناس انطباق ظرف ۲ روز کاری بررسی می‌کند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reportText,
                    onValueChange = { reportText = it },
                    placeholder = { Text("مثلاً: عدد کنتور توزیع با قرائت من مطابقت ندارد") },
                    minLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().testTag("discrepancy_report_input")
                )
                Spacer(Modifier.height(14.dp))
                InkButton(
                    text = "ارسال گزارش",
                    enabled = reportText.trim().length >= 5,
                    onClick = {
                        ecoViewModel.submitPlantReport(month.name.lowercase(), reportText) {
                            reportText = ""
                            reportOpen = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("submit_discrepancy_button")
                )
            }
        }
    }
}
