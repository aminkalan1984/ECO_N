package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.PersianFormat
import com.example.ui.components.EcoBottomNav
import com.example.ui.components.EcoNavTab
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.InkButton
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel

private data class JourneyLink(val numberFa: String, val title: String, val routeKey: String)

private val JOURNEY_LINKS = listOf(
    JourneyLink("۰۱", "ورود و OTP", "login"),
    JourneyLink("۰۲", "داشبورد نیروگاه", "dashboard"),
    JourneyLink("۰۳", "تأیید قبض", "bill"),
    JourneyLink("۰۴", "بهینه‌سازی مصرف", "optimize"),
    JourneyLink("۰۵", "طرح نیروگاه", "design"),
    JourneyLink("۰۶", "کانال درآمد", "channel"),
    JourneyLink("۰۷", "پوشش قسط", "installment"),
    JourneyLink("۰۸", "پیش‌فاکتورها", "quotes"),
    JourneyLink("۰۹", "پرونده نیروگاه", "case"),
    JourneyLink("۱۰", "درآمد و تطبیق", "income"),
    JourneyLink("۱۱", "دستیار هوش مصنوعی", "assistant"),
    JourneyLink("۱۲", "تاریخچه نسخه‌های قبض", "bills")
)

private val AUDIT_LABELS = mapOf(
    "ConsentGranted" to "رضایت ثبت شد",
    "ConsentRevoked" to "رضایت لغو شد",
    "SessionStarted" to "ورود موفق",
    "SessionEnded" to "خروج از حساب",
    "OtpFailed" to "تلاش ناموفق ورود",
    "OtpRequested" to "درخواست کد ورود",
    "TermsAccepted" to "پذیرش شرایط استفاده"
)

@Composable
fun ProfileScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onLoggedOut: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val consents by ecoViewModel.consents.collectAsStateWithLifecycle()
    val auditEvents by ecoViewModel.auditEvents.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    val activeConsent = consents.firstOrNull { it.revokedAt == null && it.scope == "bill_data" }

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "پروفایل",
                subtitle = "حساب کاربری و مسیر نیروگاه",
                onStartClick = onBack
            )

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.sun),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("پروسومر خانگی", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = PersianFormat.formatPhone(ecoState.phone),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.mutedText
                        )
                    }
                    Text("نجف‌آباد", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                }
            }
        },
        sunContent = {
            Surface(
                onClick = { onNavigate("wizard") },
                shape = RoundedCornerShape(24.dp),
                color = colors.ink,
                contentColor = colors.inkForeground,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_wizard_link")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("تکمیل پروفایل بار (۶ گام)", style = MaterialTheme.typography.titleSmall, color = colors.inkForeground)
                        Text(
                            "مصرف، انشعاب، لوازم برقی و بام",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inkForeground.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = colors.inkForeground
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("رضایت اشتراک داده", style = MaterialTheme.typography.titleMedium)
                        EcoPill(
                            text = if (activeConsent != null) "فعال" else "ثبت نشده",
                            tone = if (activeConsent != null) PillTone.INK else PillTone.SUN,
                            small = true
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "نسخه سند، زمان و شماره ماسک‌شده در Audit Log ثبت می‌شود؛ لغو رضایت هم رویداد خودش را دارد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                    Spacer(Modifier.height(12.dp))
                    InkButton(
                        text = if (activeConsent != null) "لغو رضایت" else "ثبت رضایت",
                        onClick = {
                            if (activeConsent != null) {
                                ecoViewModel.revokeConsent(activeConsent.id)
                            } else {
                                ecoViewModel.grantConsent()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_consent_toggle_button")
                    )

                    if (auditEvents.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        auditEvents.take(5).forEach { ev ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = AUDIT_LABELS[ev.action] ?: ev.action,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                )
                                Text(
                                    text = PersianFormat.tehranClock(ev.at),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.mutedText
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = PersianFormat.maskPhone(ecoState.phone),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mutedText
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("مسیر نیروگاه", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "از ورود تا صورتحساب مبتنی بر کنتور",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JOURNEY_LINKS.forEach { link ->
                    Surface(
                        onClick = { onNavigate(link.routeKey) },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journey_${link.routeKey}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 32.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.sun),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = link.numberFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = link.title,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            InkButton(
                text = "خروج از حساب",
                leadingIcon = Icons.AutoMirrored.Filled.Logout,
                onClick = { ecoViewModel.logout(onLoggedOut) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_logout_button")
            )
        },
        bottomNav = {
            EcoBottomNav(
                active = EcoNavTab.PROFILE,
                onSelect = { onNavigate(it.routeKey) }
            )
        }
    )
}
