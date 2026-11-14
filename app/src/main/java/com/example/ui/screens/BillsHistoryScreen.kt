package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.PersianFormat
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.InfoRow
import com.example.ui.components.InkButton
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillsHistoryScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val revisions by ecoViewModel.billRevisions.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    val basisVersion = revisions.lastOrNull { it.confirmed }?.version ?: ecoState.billRevision
    val currentKwh = ecoState.bill.totalKwh

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "تاریخچه نسخه‌های قبض",
                subtitle = "اصلاح قبض، نسخه جدید می‌سازد",
                onStartClick = onBack
            )
            Spacer(Modifier.height(14.dp))
            EcoPill(
                text = "مبنا: آخرین نسخه تأییدشده",
                tone = PillTone.SUN
            )
        },
        sunContent = {
            Text(
                text = "قبض فعلی",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            InfoRow("دوره", ecoState.bill.period)
            InfoRow("جمع مصرف", "${PersianFormat.fa(currentKwh)} kWh")
            InfoRow("مبلغ", "${PersianFormat.fa(ecoState.bill.total, false)} ریال", showDivider = false)

            Spacer(Modifier.height(14.dp))
            InkButton(
                text = "ثبت نسخه جدید از قبض فعلی",
                onClick = { ecoViewModel.appendCurrentBillAsRevision() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("append_bill_revision_button")
            )

            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("نسخه‌ها", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(15.dp), tint = colors.mutedText)
                    Text(
                        text = "${PersianFormat.fa(revisions.size)} نسخه",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mutedText
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (revisions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "هنوز نسخه‌ای ثبت نشده است. با دکمه بالا قبض فعلی را ثبت کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    revisions.asReversed().forEach { rev ->
                        val isBasis = rev.version == basisVersion && ecoState.billRevision == rev.version
                        val sourceLabel = when (rev.source) {
                            "ai" -> "هوش مصنوعی"
                            "offline_ocr" -> "آفلاین"
                            else -> "دستی"
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = if (isBasis) BorderStroke(2.dp, colors.ink) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(colors.sun),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = PersianFormat.fa(rev.version),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Text(
                                            text = rev.period,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    if (isBasis) {
                                        EcoPill(
                                            text = "مبنای تحلیل",
                                            tone = PillTone.INK,
                                            small = true,
                                            leadingIcon = Icons.Filled.Check
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val totalRevKwh = rev.peakKwh + rev.midKwh + rev.lowKwh
                                    Text("${PersianFormat.fa(totalRevKwh)} kWh", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                                    Text("${PersianFormat.fa(rev.amountRial, false)} ریال", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                                    Text(sourceLabel, style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                                    Text(if (rev.confirmed) "تأییدشده" else "پیش‌نویس", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                                    Text(PersianFormat.tehranClock(rev.createdAt), style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                                }
                                if (!isBasis) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .clickable { ecoViewModel.selectRevisionAsBasis(rev) }
                                            .padding(vertical = 4.dp)
                                            .testTag("select_revision_${rev.version}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "استفاده در تحلیل",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.ink
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "هر اصلاح قبض یک نسخه جدید اضافه می‌کند؛ نسخه‌های قبلی برای حسابرسی حذف نمی‌شوند. تحلیل مصرف از آخرین نسخه تأییدشده می‌خواند.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.mutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
