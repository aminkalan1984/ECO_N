package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LoanSpec
import com.example.domain.EcoCalc
import com.example.domain.PersianFormat
import com.example.ui.components.CoverageGauge
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.HBar
import com.example.ui.components.InkButton
import com.example.ui.components.PanelTitle
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val M = 1_000_000.0
private const val B = 1_000_000_000.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onContinueToQuotes: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var loanSheetOpen by remember { mutableStateOf(false) }
    var draftLoan by remember(ecoState.loan) { mutableStateOf(ecoState.loan) }
    var strategy by remember(ecoState.strategy) { mutableStateOf(ecoState.strategy) }

    val loan = ecoState.loan
    val installment = EcoCalc.monthlyInstallment(loan.amount.toDouble(), loan.rate, loan.months)
    val income = EcoCalc.MONTHLY_FIT_INCOME.toDouble()
    val ratio = EcoCalc.coverageRatio(income, installment)
    val gap = max(0.0, installment - income)
    val years = EcoCalc.coverageByYear(ratio)
    val maxLoan = EcoCalc.maxCoverableLoan(income, loan.rate, loan.months)
    val longerMonths = min(120, loan.months * 2)
    val longerRatio = EcoCalc.coverageRatio(
        income,
        EcoCalc.monthlyInstallment(loan.amount.toDouble(), loan.rate, longerMonths)
    )

    val strategies = listOf(
        Triple(
            1,
            "وام کمتر با آورده نقدی بیشتر",
            "سقف قابل پوشش با درآمد سال اول حدود ${PersianFormat.faFixed(maxLoan / B, 2)} میلیارد ریال"
        ),
        Triple(
            2,
            "افزایش مدت بازپرداخت",
            "با ${PersianFormat.fa(longerMonths)} ماه پوشش به ${PersianFormat.fa((longerRatio * 100).roundToInt())}٪ می‌رسد؛ از طریق بانک‌های همکار جایگزین"
        ),
        Triple(
            3,
            "پرداخت مابه‌التفاوت ماهانه",
            "حدود ${PersianFormat.fa(gap / M, 1)} میلیون ریال در ماه در سال اول"
        )
    )

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "پوشش قسط وام",
                subtitle = "شبیه‌ساز چندساله",
                onStartClick = onBack
            )

            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = {
                        draftLoan = ecoState.loan
                        loanSheetOpen = true
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                    modifier = Modifier.testTag("installment_edit_loan_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "وام ساتبا ${PersianFormat.fa(loan.amount / B, 2)} میلیارد ریال، سود ${PersianFormat.fa(loan.rate)}٪، ${PersianFormat.fa(loan.months)} ماه",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }

            CoverageGauge(ratio = ratio, label = "پوشش قسط سال اول")

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("درآمد ماهانه FIT", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${PersianFormat.faFixed(income / M, 2)} میلیون ریال",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("قسط ماهانه", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${PersianFormat.faFixed(installment / M, 2)} میلیون ریال",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (gap > 0) {
                    EcoPill(
                        text = "شکاف ماهانه حدود ${PersianFormat.faFixed(gap / M, 2)} میلیون ریال",
                        tone = PillTone.DANGER,
                        leadingIcon = Icons.Filled.WarningAmber
                    )
                } else {
                    EcoPill(
                        text = "درآمد کل قسط را پوشش می‌دهد",
                        tone = PillTone.SUCCESS
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مسیر چندساله", style = MaterialTheme.typography.titleMedium)
                EcoPill(text = "سناریو، فرض تعدیل ۵۰٪ سالانه نرخ", tone = PillTone.SUN, small = true)
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                years.forEachIndexed { idx, r ->
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("سال ${PersianFormat.fa(idx + 1)}", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            Text("${PersianFormat.fa((r * 100).roundToInt())}٪", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        HBar(ratio = r, fillColor = if (idx == 0) colors.sunDeep else colors.sun)
                    }
                }
            }
        },
        sunContent = {
            PanelTitle(
                title = "چه کار کنم؟",
                aside = {
                    Text(
                        text = "سه راهکار",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            )
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                strategies.forEach { (id, title, desc) ->
                    val active = strategy == id
                    Surface(
                        onClick = { strategy = id },
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = if (active) BorderStroke(2.dp, colors.ink) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("strategy_card_$id")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.ink),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = PersianFormat.fa(id),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.inkForeground,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.titleSmall)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            InkButton(
                text = "ادامه با راهکار انتخابی",
                onClick = {
                    val chosen = strategy
                    if (chosen == null) {
                        ecoViewModel.emitToast("ابتدا یکی از سه راهکار را انتخاب کنید")
                    } else {
                        ecoViewModel.chooseStrategy(chosen)
                        ecoViewModel.emitToast("راهکار ${PersianFormat.fa(chosen)} ثبت شد")
                        onContinueToQuotes()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("installment_continue_button")
            )
        }
    )

    if (loanSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { loanSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            val previewInstallment = EcoCalc.monthlyInstallment(
                draftLoan.amount.toDouble(),
                draftLoan.rate,
                draftLoan.months
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("ویرایش مشخصات وام", style = MaterialTheme.typography.titleLarge)
                Text(
                    "شبیه‌سازی بلافاصله به‌روز می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )

                LoanSliderRow(
                    label = "مبلغ وام",
                    valueText = "${PersianFormat.fa(draftLoan.amount / B, 2)} میلیارد ریال",
                    value = (draftLoan.amount / B).toFloat(),
                    range = 0.5f..5.0f,
                    onChange = { draftLoan = draftLoan.copy(amount = (it * B).roundToLong()) }
                )
                LoanSliderRow(
                    label = "نرخ سود سالانه",
                    valueText = "${PersianFormat.fa(draftLoan.rate, 1)}٪",
                    value = draftLoan.rate.toFloat(),
                    range = 0f..30f,
                    onChange = { draftLoan = draftLoan.copy(rate = (it * 2).roundToInt() / 2.0) }
                )
                LoanSliderRow(
                    label = "مدت بازپرداخت",
                    valueText = "${PersianFormat.fa(draftLoan.months)} ماه",
                    value = draftLoan.months.toFloat(),
                    range = 12f..120f,
                    onChange = { draftLoan = draftLoan.copy(months = it.roundToInt()) }
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "قسط ماهانه: ${PersianFormat.faFixed(previewInstallment / M, 2)} میلیون ریال",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                InkButton(
                    text = "اعمال تغییرات",
                    onClick = {
                        ecoViewModel.updateLoan(draftLoan)
                        loanSheetOpen = false
                    },
                    modifier = Modifier.fillMaxWidth().testTag("apply_loan_changes_button")
                )
            }
        }
    }
}

@Composable
private fun LoanSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = EcoTheme.colors.mutedText)
            Text(valueText, style = MaterialTheme.typography.titleSmall)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = EcoTheme.colors.ink,
                activeTrackColor = EcoTheme.colors.sunDeep
            )
        )
    }
}
