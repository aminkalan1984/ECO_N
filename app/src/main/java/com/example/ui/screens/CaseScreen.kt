package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ActionCard
import com.example.ui.components.EcoBottomNav
import com.example.ui.components.EcoNavTab
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.HeroStat
import com.example.ui.components.InkButton
import com.example.ui.components.PanelTitle
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.components.WhiteButton
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel

private enum class CaseStepState { DONE, CURRENT, TODO }

private data class CaseStep(
    val title: String,
    val note: String,
    val state: CaseStepState,
    val hasDocs: Boolean = false
)

private val CASE_STEPS = listOf(
    CaseStep("ثبت درخواست", "تکمیل شد، ۱۲ مهر", CaseStepState.DONE),
    CaseStep("تشکیل پرونده در مهرسان", "تکمیل شد، ۱۵ مهر", CaseStepState.DONE),
    CaseStep("بارگذاری مدارک مالکیت و شبا", "تأیید شد، ۱۸ مهر", CaseStepState.DONE, hasDocs = true),
    CaseStep("بازدید فنی شرکت توزیع", "در جریان، مسئول: شرکت توزیع اصفهان", CaseStepState.CURRENT),
    CaseStep("نصب و راه‌اندازی", "پیمانکار منتخب مناقصه", CaseStepState.TODO),
    CaseStep("تست و نصب کنتور دوطرفه", "شرکت توزیع", CaseStepState.TODO),
    CaseStep("شروع بهره‌برداری", "فعال‌سازی قرارداد و پایش", CaseStepState.TODO)
)

private val INSTALL_STAGES = listOf("تحویل تجهیزات", "سازه", "نصب پنل", "سیم‌کشی", "راه‌اندازی")

private val VERIFIED_DOCS = listOf(
    "سند مالکیت ملک" to "۱۸ مهر",
    "کارت ملی مالک" to "۱۸ مهر",
    "شماره شبا به نام مالک" to "۱۸ مهر",
    "آخرین قبض برق" to "۱۶ مهر"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var docsOpen by remember { mutableStateOf(false) }
    var visitOpen by remember { mutableStateOf(false) }

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "پرونده نیروگاه",
                subtitle = "کد رهگیری مهرسان ۰۴۱۲-۷۷۸۱۵",
                onStartClick = onBack
            )

            HeroStat(
                label = "مرحله جاری",
                value = "۴ از ۷",
                aside = {
                    EcoPill(
                        text = if (ecoState.visitConfirmed) "بازدید زمان‌بندی شد" else "در انتظار بازدید",
                        tone = if (ecoState.visitConfirmed) PillTone.SUCCESS else PillTone.WARN,
                        dotColor = if (ecoState.visitConfirmed) colors.success else colors.warn
                    )
                }
            )

            Spacer(Modifier.height(10.dp))
            EcoPill(
                text = "مهلت بازدید شرکت توزیع: ۳ روز کاری",
                tone = PillTone.WHITE,
                leadingIcon = Icons.Filled.Schedule
            )

            Spacer(Modifier.height(20.dp))
            Column {
                CASE_STEPS.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (step.state) {
                                            CaseStepState.DONE -> colors.ink
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .border(
                                        width = when (step.state) {
                                            CaseStepState.CURRENT -> 3.dp
                                            CaseStepState.TODO -> 2.dp
                                            CaseStepState.DONE -> 0.dp
                                        },
                                        color = when (step.state) {
                                            CaseStepState.CURRENT -> colors.sunDeep
                                            CaseStepState.TODO -> colors.trackStrong
                                            CaseStepState.DONE -> colors.ink
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when (step.state) {
                                    CaseStepState.DONE -> Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = colors.inkForeground,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    CaseStepState.CURRENT -> Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(colors.sunDeep)
                                    )
                                    CaseStepState.TODO -> Unit
                                }
                            }
                            if (idx < CASE_STEPS.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(34.dp)
                                        .background(if (step.state == CaseStepState.DONE) colors.ink else colors.trackStrong)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (idx < CASE_STEPS.lastIndex) 12.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (step.state == CaseStepState.TODO)
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f)
                                    else MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = step.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedText
                                )
                            }
                            if (step.hasDocs) {
                                Text(
                                    text = "مدارک",
                                    style = MaterialTheme.typography.labelMedium,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier
                                        .clickable { docsOpen = true }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                        .testTag("case_docs_button")
                                )
                            }
                            if (step.state == CaseStepState.CURRENT) {
                                EcoPill(text = "جاری", tone = PillTone.SUN, small = true)
                            }
                        }
                    }
                }
            }
        },
        sunContent = {
            PanelTitle(
                title = "مراحل نصب پیمانکار",
                aside = { Text("۰ از ۵", style = MaterialTheme.typography.titleSmall) }
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                INSTALL_STAGES.forEach { stage ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stage,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = "هر مرحله با عکس دارای زمان و موقعیت تأیید می‌شود",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
            }

            if (ecoState.visitConfirmed) {
                ActionCard(
                    icon = Icons.Filled.CheckCircle,
                    iconTint = colors.success,
                    title = "زمان بازدید تأیید شد",
                    subtitle = "سه‌شنبه ۲۰ آبان، ساعت ۱۰ تا ۱۲",
                    onClick = { visitOpen = true },
                    modifier = Modifier.testTag("case_visit_card")
                )
            } else {
                ActionCard(
                    icon = Icons.Filled.Schedule,
                    title = "زمان بازدید را تأیید کنید",
                    subtitle = "پیشنهاد شرکت توزیع: سه‌شنبه ۲۰ آبان، ۱۰ تا ۱۲",
                    onClick = { visitOpen = true },
                    modifier = Modifier.testTag("case_visit_card")
                )
            }
        },
        bottomNav = {
            EcoBottomNav(
                active = EcoNavTab.CASE,
                onSelect = { onNavigate(it.routeKey) }
            )
        }
    )

    if (docsOpen) {
        ModalBottomSheet(
            onDismissRequest = { docsOpen = false },
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
                Text("مدارک بارگذاری‌شده", style = MaterialTheme.typography.titleLarge)
                Text(
                    "همه مدارک توسط کارشناس تأیید شده‌اند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                VERIFIED_DOCS.forEach { (docName, date) ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.sunSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Text(docName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            EcoPill(text = "تأیید، $date", tone = PillTone.SUCCESS, small = true)
                        }
                    }
                }
            }
        }
    }

    if (visitOpen) {
        ModalBottomSheet(
            onDismissRequest = { visitOpen = false },
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
                Text("بازدید فنی شرکت توزیع", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "کارشناس برای بررسی بام و انشعاب مراجعه می‌کند. لطفاً در زمان بازدید در محل حضور داشته باشید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("زمان پیشنهادی", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                        Spacer(Modifier.height(4.dp))
                        Text("سه‌شنبه ۲۰ آبان ۱۴۰۵", style = MaterialTheme.typography.titleLarge)
                        Text("ساعت ۱۰ تا ۱۲", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InkButton(
                        text = if (ecoState.visitConfirmed) "تأیید شده" else "تأیید زمان",
                        onClick = {
                            ecoViewModel.setVisitConfirmed(true)
                            ecoViewModel.emitToast("زمان بازدید تأیید شد")
                            visitOpen = false
                        },
                        modifier = Modifier.weight(1f).testTag("visit_confirm_button")
                    )
                    WhiteButton(
                        text = "درخواست تغییر",
                        containerColor = colors.sun,
                        onClick = {
                            ecoViewModel.setVisitConfirmed(false)
                            ecoViewModel.emitToast("درخواست تغییر زمان ارسال شد")
                            visitOpen = false
                        },
                        modifier = Modifier.testTag("visit_reschedule_button")
                    )
                }
            }
        }
    }
}
