package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.PersianFormat
import com.example.domain.RateTables
import com.example.ui.theme.EcoTheme

enum class EcoNavTab(
    val routeKey: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("dashboard", R.string.nav_home, Icons.Filled.Home, "nav_home"),
    DESIGN("design", R.string.nav_design, Icons.Filled.WbSunny, "nav_design"),
    CASE("case", R.string.nav_case, Icons.Filled.Description, "nav_case"),
    INCOME("income", R.string.nav_income, Icons.AutoMirrored.Filled.TrendingUp, "nav_income"),
    PROFILE("profile", R.string.nav_profile, Icons.Filled.Person, "nav_profile")
}

@Composable
fun EcoBottomNav(
    active: EcoNavTab,
    onSelect: (EcoNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = EcoTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.sunStrong,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EcoNavTab.entries.forEach { tab ->
                val isSelected = tab == active
                Surface(
                    onClick = { onSelect(tab) },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag(tab.testTag),
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else colors.sunStrong,
                    shadowElevation = if (isSelected) 3.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

data class EcoNotificationItem(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val time: String,
    val routeKey: String
)

private val NOTIFICATION_ITEMS = listOf(
    EcoNotificationItem(
        icon = Icons.Filled.Speed,
        title = "قرائت کنتور مهر ثبت شد",
        body = "تطبیق سه‌طرفه انجام شد؛ اختلاف ۲,۴٪ در محدوده مجاز است.",
        time = "۱۰ دقیقه پیش",
        routeKey = "income"
    ),
    EcoNotificationItem(
        icon = Icons.Filled.Schedule,
        title = "زمان بازدید شرکت توزیع",
        body = "پیشنهاد: سه‌شنبه ۲۰ آبان، ساعت ۱۰ تا ۱۲. لطفاً تأیید کنید.",
        time = "۲ ساعت پیش",
        routeKey = "case"
    ),
    EcoNotificationItem(
        icon = Icons.Filled.Description,
        title = "۳ پیش‌فاکتور جدید",
        body = "تأمین‌کنندگان برای طرح ۵ کیلوواتی قیمت ارسال کردند.",
        time = "دیروز",
        routeKey = "quotes"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    open: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (!open) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            Text(
                text = "اعلان‌ها",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "آخرین رویدادهای نیروگاه شما",
                style = MaterialTheme.typography.bodySmall,
                color = EcoTheme.colors.mutedText
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NOTIFICATION_ITEMS.forEach { item ->
                    Surface(
                        onClick = {
                            onDismiss()
                            onNavigate(item.routeKey)
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EcoTheme.colors.sunSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = item.time,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EcoTheme.colors.mutedText
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EcoTheme.colors.mutedText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class MenuNavItem(
    val routeKey: String,
    val label: String,
    val hint: String,
    val icon: ImageVector,
    val badge: String? = null
)

private val MENU_JOURNEY_ITEMS = listOf(
    MenuNavItem("dashboard", "داشبورد نیروگاه", "تولید و درآمد امروز", Icons.Filled.Home),
    MenuNavItem("wizard", "پروفایل بار", "ویزارد ۶ گامه مصرف و بام", Icons.Filled.Assignment),
    MenuNavItem("bill", "قبض برق", "اسکن، تحلیل و تأیید", Icons.AutoMirrored.Filled.ReceiptLong),
    MenuNavItem("bills", "تاریخچه نسخه‌ها", "نسخه‌دار شدن اصلاحات", Icons.Filled.History),
    MenuNavItem("optimize", "بهینه‌سازی مصرف", "برنامه دمایی", Icons.Filled.Tune),
    MenuNavItem("design", "طرح نیروگاه", "ظرفیت و تولید ماهانه", Icons.Filled.WbSunny),
    MenuNavItem("channel", "کانال درآمد", "خرید تضمینی یا خودتأمین", Icons.AutoMirrored.Filled.CompareArrows),
    MenuNavItem("installment", "پوشش قسط", "وام و شبیه‌ساز", Icons.Filled.AccountBalance),
    MenuNavItem("case", "پرونده نیروگاه", "مهرسان و مدارک", Icons.Filled.Description),
    MenuNavItem("income", "درآمد و صورتحساب", "تطبیق کنتور", Icons.AutoMirrored.Filled.TrendingUp)
)

private val MENU_TOOL_ITEMS = listOf(
    MenuNavItem("assistant", "دستیار هوش مصنوعی", "گفتگو درباره نیروگاه و قبض", Icons.Filled.AutoAwesome, badge = "AI"),
    MenuNavItem("quotes", "پیش‌فاکتورها", "مقایسه پیشنهاد نصب", Icons.Filled.RequestQuote)
)

@Composable
fun MainMenuDrawerContent(
    currentRouteKey: String,
    phone: String,
    loggedIn: Boolean,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = EcoTheme.colors
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
        modifier = Modifier.widthIn(max = 330.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.sun),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "منوی اکوسولار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "همه بخش‌های نیروگاه در یک نگاه",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText
                        )
                    }
                }
                IconCircle(
                    onClick = onClose,
                    contentDescription = stringResource(R.string.cd_close_menu)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Surface(
                    onClick = { onNavigate("profile") },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.sunSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("حساب کاربری", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = PersianFormat.maskPhone(phone),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.mutedText
                            )
                        }
                        EcoPill(
                            text = if (loggedIn) "فعال" else "مهمان",
                            tone = PillTone.MUTED,
                            small = true
                        )
                    }
                }

                DrawerSectionTitle("مسیر نیروگاه")
                MENU_JOURNEY_ITEMS.forEach { item ->
                    DrawerNavRow(
                        item = item,
                        active = currentRouteKey == item.routeKey,
                        onClick = { onNavigate(item.routeKey) }
                    )
                }

                DrawerSectionTitle("ابزارها")
                MENU_TOOL_ITEMS.forEach { item ->
                    DrawerNavRow(
                        item = item,
                        active = currentRouteKey == item.routeKey,
                        onClick = { onNavigate(item.routeKey) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = onLogout,
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_logout_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.ink.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "خروج از حساب",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            HorizontalDivider(color = colors.trackStrong.copy(alpha = 0.4f))
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "جدول نرخ ${RateTables.ACTIVE.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
                Text(
                    text = "${PersianFormat.fa(10)} بخش · نسخه اندروید",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = EcoTheme.colors.mutedText,
        modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun DrawerNavRow(
    item: MenuNavItem,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = EcoTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (active) colors.ink else MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) colors.inkForeground.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (active) colors.inkForeground else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (active) colors.inkForeground else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.badge != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) colors.inkForeground.copy(alpha = 0.2f) else colors.sun
                        ) {
                            Text(
                                text = item.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) colors.inkForeground else colors.ink,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = item.hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) colors.inkForeground.copy(alpha = 0.72f) else colors.mutedText
                )
            }
        }
    }
}
