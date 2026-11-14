package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.PersianFormat
import com.example.domain.SolarSavingsEstimate
import com.example.ui.theme.EcoTheme

@Composable
fun SolarSavingsCard(
    estimate: SolarSavingsEstimate,
    modifier: Modifier = Modifier
) {
    val colors = EcoTheme.colors
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("bill_solar_savings_card")
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
                            .background(colors.sunSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = colors.sunDeep,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "برآورد صرفه‌جویی خورشیدی",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "بر پایه مصرف استخراج‌شده از قبض",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText
                        )
                    }
                }
                EcoPill(
                    text = "${PersianFormat.fa(estimate.recommendedKwp, 1)} kWp",
                    tone = PillTone.SUN,
                    small = true
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "پوشش برق مصرفی با پنل خورشیدی",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Text(
                    text = "${PersianFormat.fa(estimate.billOffsetPct)}٪",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.success
                )
            }
            Spacer(Modifier.height(6.dp))
            HBar(
                ratio = estimate.billOffsetPct / 100.0,
                fillColor = colors.success
            )

            Spacer(Modifier.height(12.dp))
            InfoRow(
                label = "صرفه‌جویی برآوردی ماهانه (خودتأمین)",
                value = "${PersianFormat.fa(estimate.monthlySelfSavingsRial, false)} ریال"
            )
            InfoRow(
                label = "درآمد ماهانه فروش تضمینی",
                value = "${PersianFormat.fa(estimate.monthlyFitRevenueRial, false)} ریال"
            )
            InfoRow(
                label = "تولید سالانه برآوردی",
                value = "${PersianFormat.fa(estimate.estimatedAnnualSolarKwh, false)} kWh"
            )
            InfoRow(
                label = "کاهش سالانه CO₂",
                value = "${PersianFormat.fa(estimate.annualCo2SavedKg, false)} کیلوگرم",
                showDivider = false
            )
        }
    }
}
