package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.BillData
import com.example.data.BillExtractionUiState
import com.example.domain.BillChecks
import com.example.domain.BillField
import com.example.domain.BillIssue
import com.example.domain.Confidence
import com.example.domain.EcoCalc
import com.example.domain.PersianFormat
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.Footnote
import com.example.ui.components.InfoCircleButton
import com.example.ui.components.InfoRow
import com.example.ui.components.InkButton
import com.example.ui.components.PillTone
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SolarSavingsCard
import com.example.ui.components.WhiteButton
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.BillAiViewModel
import com.example.ui.viewmodel.EcoViewModel
import java.io.File

private enum class BillRowKey(val field: BillField, val label: String, val unit: String?, val numeric: Boolean) {
    ID(BillField.BILL_ID, "شناسه قبض", null, false),
    PERIOD(BillField.PERIOD, "دوره صورتحساب", null, false),
    PEAK(BillField.PEAK_KWH, "مصرف اوج", "kWh", true),
    MID(BillField.MID_KWH, "مصرف میان‌باری", "kWh", true),
    LOW(BillField.LOW_KWH, "مصرف کم‌باری", "kWh", true),
    TOTAL(BillField.AMOUNT_RIAL, "مبلغ کل قبض", "ریال", true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    ecoViewModel: EcoViewModel,
    billAiViewModel: BillAiViewModel,
    onBack: () -> Unit,
    onContinueToOptimize: () -> Unit
) {
    val context = LocalContext.current
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val aiUiState by billAiViewModel.uiState.collectAsStateWithLifecycle()
    val mergedIssues by billAiViewModel.mergedIssues.collectAsStateWithLifecycle()
    val photoUri by billAiViewModel.selectedPhotoUri.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var draft by remember(ecoState.bill) { mutableStateOf(ecoState.bill) }
    var editingKey by remember { mutableStateOf<BillRowKey?>(null) }
    var manualMode by remember { mutableStateOf(false) }
    var needsReview by remember {
        mutableStateOf(if (ecoState.lowConfidenceReviewed) emptySet() else setOf(BillRowKey.LOW))
    }
    var issueKeys by remember { mutableStateOf(emptySet<BillRowKey>()) }
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var extractedPeriodDays by remember { mutableStateOf(60) }
    var confidencePct by remember { mutableStateOf(92) }
    var sourceLabel by remember { mutableStateOf("manual") }
    var confirmSheetOpen by remember { mutableStateOf(false) }
    var infoSheetOpen by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            billAiViewModel.analyzeBillPhoto(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            billAiViewModel.analyzeBillPhoto(context, uri)
        }
    }

    fun launchCameraCapture() {
        runCatching {
            val dir = File(context.cacheDir, "bill_photos").apply { mkdirs() }
            val file = File(dir, "bill_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure {
            ecoViewModel.emitToast("دوربین در دسترس نیست؛ می‌توانید تصویر قبض را از گالری انتخاب کنید.")
        }
    }

    LaunchedEffect(aiUiState) {
        when (val s = aiUiState) {
            is BillExtractionUiState.Success -> {
                val b = s.bill
                if (!b.isElectricityBill) {
                    aiSummary = null
                    ecoViewModel.emitToast("این تصویر شبیه قبض برق نیست؛ عکس واضح قبض را بارگذاری کنید.")
                } else {
                    aiSummary = b.summary?.trim()?.takeIf { it.isNotEmpty() }
                    extractedPeriodDays = b.periodDays ?: 60
                    draft = BillData(
                        id = b.billId.orEmpty(),
                        period = b.period.orEmpty(),
                        peak = b.peakKwh ?: 0,
                        mid = b.midKwh ?: 0,
                        low = b.lowKwh ?: 0,
                        total = b.amountRial ?: 0L
                    )
                    val flagged = mutableSetOf<BillRowKey>()
                    val withIssue = mutableSetOf<BillRowKey>()
                    BillRowKey.entries.forEach { rk ->
                        if (b.confidence[rk.field] == Confidence.LOW ||
                            (rk == BillRowKey.ID && b.billId == null) ||
                            (rk == BillRowKey.PERIOD && b.period == null) ||
                            (rk == BillRowKey.PEAK && b.peakKwh == null) ||
                            (rk == BillRowKey.MID && b.midKwh == null) ||
                            (rk == BillRowKey.LOW && b.lowKwh == null) ||
                            (rk == BillRowKey.TOTAL && b.amountRial == null)
                        ) {
                            flagged.add(rk)
                        }
                    }
                    mergedIssues.orEmpty().forEach { issue ->
                        val mapped = when (issue.field) {
                            BillField.BILL_ID -> listOf(BillRowKey.ID)
                            BillField.PERIOD -> listOf(BillRowKey.PERIOD)
                            BillField.PEAK_KWH -> listOf(BillRowKey.PEAK)
                            BillField.MID_KWH -> listOf(BillRowKey.MID)
                            BillField.LOW_KWH -> listOf(BillRowKey.LOW)
                            BillField.TOTAL_KWH -> listOf(BillRowKey.PEAK, BillRowKey.MID, BillRowKey.LOW)
                            BillField.AMOUNT_RIAL -> listOf(BillRowKey.TOTAL)
                            BillField.GENERAL -> emptyList()
                        }
                        flagged.addAll(mapped)
                        withIssue.addAll(mapped)
                    }
                    needsReview = flagged
                    issueKeys = withIssue
                    confidencePct = BillChecks.overallConfidence(b.confidence)
                    sourceLabel = "ai"
                    manualMode = false
                    editingKey = null
                    val count = mergedIssues?.size ?: 0
                    ecoViewModel.emitToast(
                        if (count > 0) "قبض خوانده شد (${PersianFormat.fa(count)} مورد نیاز به بررسی)"
                        else "قبض با هوش مصنوعی خوانده شد"
                    )
                }
            }
            is BillExtractionUiState.Error -> {
                manualMode = true
                ecoViewModel.emitToast(s.message)
            }
            else -> Unit
        }
    }

    val scanning = aiUiState is BillExtractionUiState.Loading
    val dirty = draft != ecoState.bill
    val usage = BillChecks.analyzeBillUsage(draft.peak, draft.mid, draft.low, draft.total)
    val solarSavings = EcoCalc.estimateSolarSavingsFromBill(
        peakKwh = draft.peak,
        midKwh = draft.mid,
        lowKwh = draft.low,
        amountRial = draft.total,
        periodDays = extractedPeriodDays,
        channelId = ecoState.channel
    )

    fun saveAndProceed() {
        val issuesSummary = mergedIssues.orEmpty().joinToString("؛ ") { "${it.field.label}: ${it.message}" }
        ecoViewModel.confirmBill(
            bill = draft,
            source = if (dirty && sourceLabel == "manual") "manual" else sourceLabel,
            confidence = if (needsReview.isEmpty()) "high" else "low",
            issuesText = issuesSummary,
            dirty = dirty
        ) {
            onContinueToOptimize()
        }
    }

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "تأیید اطلاعات قبض",
                onStartClick = onBack,
                endContent = {
                    InfoCircleButton(
                        onClick = { infoSheetOpen = true },
                        modifier = Modifier.testTag("bill_info_button")
                    )
                }
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "مرحله ۲ از ۳ | قبض برق",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (idx < 2) colors.ink else colors.track)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 160.dp, height = 140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .clickable(enabled = !scanning) {
                                launchCameraCapture()
                            }
                            .testTag("bill_photo_preview"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUri),
                                contentDescription = "تصویر قبض برق",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    listOf(0.85f, 1f, 0.6f, 0.9f).forEach { w ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(w)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(colors.track)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = null,
                                        tint = colors.sunDeep,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "لمس برای اسکن قبض",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.mutedText
                                    )
                                }
                            }
                        }
                        if (scanning) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(colors.ink.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = colors.sun)
                            }
                        }
                    }

                    EcoPill(
                        text = if (scanning) "هوش مصنوعی در حال خواندن قبض..." else "اطمینان استخراج ${PersianFormat.fa(confidencePct)}٪",
                        tone = if (scanning) PillTone.SUN else PillTone.SUCCESS,
                        small = true,
                        dotColor = if (scanning) colors.sunDeep else colors.success,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { launchCameraCapture() },
                            enabled = !scanning,
                            shape = RoundedCornerShape(50),
                            color = colors.ink,
                            modifier = Modifier.height(48.dp).testTag("bill_camera_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = "اسکن با دوربین",
                                    tint = colors.inkForeground,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "اسکن دوربین",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.inkForeground
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !scanning,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.height(48.dp).testTag("bill_gallery_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = "انتخاب از گالری", modifier = Modifier.size(16.dp))
                                Text(if (photoUri != null) "عکس مجدد" else "گالری", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        },
        sunContent = {
            Text(
                text = "داده‌های استخراج‌شده",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "هر مقدار را بررسی کنید؛ مبنای تحلیل همین داده‌هاست",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    BillRowKey.entries.forEachIndexed { idx, rowKey ->
                        val isEditing = manualMode || editingKey == rowKey
                        val flagged = rowKey in needsReview
                        val rawVal = when (rowKey) {
                            BillRowKey.ID -> draft.id
                            BillRowKey.PERIOD -> draft.period
                            BillRowKey.PEAK -> draft.peak.toString()
                            BillRowKey.MID -> draft.mid.toString()
                            BillRowKey.LOW -> draft.low.toString()
                            BillRowKey.TOTAL -> draft.total.toString()
                        }
                        val formattedDisplay = when (rowKey) {
                            BillRowKey.ID -> if (draft.id.isBlank()) "—" else PersianFormat.faDigits(draft.id)
                            BillRowKey.PERIOD -> draft.period.ifBlank { "—" }
                            BillRowKey.PEAK -> "${PersianFormat.fa(draft.peak, false)} kWh"
                            BillRowKey.MID -> "${PersianFormat.fa(draft.mid, false)} kWh"
                            BillRowKey.LOW -> "${PersianFormat.fa(draft.low, false)} kWh"
                            BillRowKey.TOTAL -> "${PersianFormat.fa(draft.total, false)} ریال"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (flagged) colors.warnSoft else MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rowKey.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.mutedText
                                    )
                                    if (flagged) {
                                        Spacer(Modifier.height(3.dp))
                                        EcoPill(
                                            text = if (rowKey in issueKeys) "ناسازگار" else "اطمینان پایین",
                                            tone = PillTone.WARN,
                                            small = true
                                        )
                                    }
                                }
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = if (rowKey.numeric || rowKey == BillRowKey.ID) PersianFormat.faDigits(rawVal) else rawVal,
                                        onValueChange = { next ->
                                            needsReview = needsReview - rowKey
                                            val latinDigits = PersianFormat.toLatinDigits(next).filter { it.isDigit() }
                                            draft = when (rowKey) {
                                                BillRowKey.ID -> draft.copy(id = latinDigits.take(15))
                                                BillRowKey.PERIOD -> draft.copy(period = next)
                                                BillRowKey.PEAK -> draft.copy(peak = latinDigits.toIntOrNull() ?: 0)
                                                BillRowKey.MID -> draft.copy(mid = latinDigits.toIntOrNull() ?: 0)
                                                BillRowKey.LOW -> draft.copy(low = latinDigits.toIntOrNull() ?: 0)
                                                BillRowKey.TOTAL -> draft.copy(total = latinDigits.toLongOrNull() ?: 0L)
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = if (rowKey.numeric || rowKey == BillRowKey.ID) KeyboardType.Number else KeyboardType.Text
                                        ),
                                        modifier = Modifier.width(136.dp)
                                    )
                                } else {
                                    Text(
                                        text = formattedDisplay,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        needsReview = needsReview - rowKey
                                        editingKey = if (editingKey == rowKey) null else rowKey
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("bill_edit_${rowKey.name.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = if (editingKey == rowKey) Icons.Filled.Check else Icons.Filled.Edit,
                                        contentDescription = "ویرایش ${rowKey.label}",
                                        tint = if (flagged) colors.warn else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (idx < BillRowKey.entries.lastIndex && !flagged) {
                            HorizontalDivider(color = colors.cardBorder)
                        }
                    }
                }
            }

            if (mergedIssues != null) {
                Spacer(Modifier.height(12.dp))
                val issueList: List<BillIssue> = mergedIssues.orEmpty()
                if (issueList.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = colors.warn, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "ناسازگاری‌های شناسایی‌شده (${PersianFormat.fa(issueList.size)})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            issueList.forEach { issue ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (issue.isRuleSource) colors.warn else colors.sunDeep,
                                        modifier = Modifier.padding(top = 3.dp).size(14.dp)
                                    )
                                    Text(
                                        text = "${issue.field.label}: ${PersianFormat.faDigits(issue.message)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(18.dp))
                            Text(
                                text = "هوش مصنوعی ناسازگاری‌ای در قبض پیدا نکرد",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.success
                            )
                        }
                    }
                }
            }

            if (usage != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ShowChart, contentDescription = null, tint = colors.sunDeep, modifier = Modifier.size(18.dp))
                            Text("تحلیل محتوای قبض", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        InfoRow("جمع مصرف سه بازه", "${PersianFormat.fa(usage.totalKwh)} kWh")
                        InfoRow(
                            "سهم اوج / میان‌باری / کم‌باری",
                            "${PersianFormat.fa(usage.peakSharePct)}٪ / ${PersianFormat.fa(usage.midSharePct)}٪ / ${PersianFormat.fa(usage.lowSharePct)}٪"
                        )
                        InfoRow(
                            "هزینه هر کیلووات‌ساعت",
                            usage.pricePerKwh?.let { "${PersianFormat.fa(it)} ریال" } ?: "—",
                            showDivider = false
                        )
                        if (!aiSummary.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = PersianFormat.faDigits(aiSummary.orEmpty()),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (solarSavings != null) {
                Spacer(Modifier.height(12.dp))
                SolarSavingsCard(estimate = solarSavings)
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InkButton(
                    text = "تأیید و ادامه",
                    enabled = !scanning,
                    onClick = {
                        if (needsReview.isNotEmpty()) confirmSheetOpen = true else saveAndProceed()
                    },
                    modifier = Modifier.weight(1f).testTag("bill_confirm_button")
                )
                WhiteButton(
                    text = if (manualMode) "پایان اصلاح" else "اصلاح دستی",
                    highlighted = manualMode,
                    onClick = {
                        manualMode = !manualMode
                        needsReview = emptySet()
                    },
                    modifier = Modifier.testTag("bill_manual_edit_button")
                )
            }
            Footnote(
                text = "هر اصلاح با شماره بازبینی ذخیره می‌شود و نسخه قبلی حفظ می‌ماند" +
                    if (ecoState.billRevision > 1) " (بازبینی فعلی: ${PersianFormat.faDigits(ecoState.billRevision)})" else ""
            )
        }
    )

    if (confirmSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { confirmSheetOpen = false },
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
                Text("این موارد را بررسی کنید", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "مقادیر زیر اطمینان پایین دارند یا با بقیه قبض همخوانی ندارند. آیا با قبض شما مطابقت دارند؟",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Spacer(Modifier.height(14.dp))
                needsReview.forEach { rk ->
                    val valStr = when (rk) {
                        BillRowKey.ID -> PersianFormat.faDigits(draft.id).ifBlank { "—" }
                        BillRowKey.PERIOD -> draft.period.ifBlank { "—" }
                        BillRowKey.PEAK -> "${PersianFormat.fa(draft.peak, false)} kWh"
                        BillRowKey.MID -> "${PersianFormat.fa(draft.mid, false)} kWh"
                        BillRowKey.LOW -> "${PersianFormat.fa(draft.low, false)} kWh"
                        BillRowKey.TOTAL -> "${PersianFormat.fa(draft.total, false)} ریال"
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rk.label, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            Text(valStr, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InkButton(
                        text = "بله، درست است",
                        onClick = {
                            needsReview = emptySet()
                            confirmSheetOpen = false
                            saveAndProceed()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    WhiteButton(
                        text = "اصلاح مقدار",
                        containerColor = colors.sun,
                        onClick = {
                            val first = needsReview.firstOrNull()
                            confirmSheetOpen = false
                            if (first != null) {
                                needsReview = needsReview - first
                                editingKey = first
                            }
                        }
                    )
                }
            }
        }
    }

    if (infoSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { infoSheetOpen = false },
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
                Text("راهنمای تأیید قبض", style = MaterialTheme.typography.titleLarge)
                Text("• روی «دوربین» یا «گالری» بزنید و عکس قبض برق خود را انتخاب کنید.", style = MaterialTheme.typography.bodyMedium)
                Text("• هوش مصنوعی شناسه، دوره، مصرف اوج، میان‌باری، کم‌باری و مبلغ را استخراج می‌کند.", style = MaterialTheme.typography.bodyMedium)
                Text("• ردیف نارنجی یعنی اطمینان پایین است یا با بقیه قبض همخوانی ندارد؛ آن را با قبض مقایسه کنید.", style = MaterialTheme.typography.bodyMedium)
                Text("• برای اصلاح هر مقدار روی آیکن مداد بزنید یا «اصلاح دستی» را انتخاب کنید.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
