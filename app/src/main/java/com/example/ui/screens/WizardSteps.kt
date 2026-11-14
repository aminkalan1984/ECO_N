package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.ApplianceEntry
import com.example.domain.CapacityLimit
import com.example.domain.LoadProfileCalc
import com.example.domain.PersianFormat
import com.example.domain.ProfileDraft
import com.example.domain.RateTables
import com.example.domain.Sector
import com.example.domain.Tariff
import com.example.ui.components.EcoPill
import com.example.ui.components.EcoSwitch
import com.example.ui.components.HBar
import com.example.ui.components.InfoRow
import com.example.ui.components.PillTone
import com.example.ui.components.ScenarioLabel
import com.example.ui.theme.EcoTheme

@Composable
fun WizardStepSectorLocation(
    draft: ProfileDraft,
    onPatch: ((ProfileDraft) -> ProfileDraft) -> Unit
) {
    val colors = EcoTheme.colors
    Text("این نیروگاه برای چه بخشی است؟", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Sector.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { sector ->
                    val selected = draft.sector == sector
                    Surface(
                        onClick = { onPatch { it.copy(sector = sector) } },
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) colors.ink else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("wizard_sector_${sector.raw}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = sector.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) colors.inkForeground else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    WizardField(label = "شهر محل نصب") {
        WizardTextField(
            value = draft.city,
            onValueChange = { city -> onPatch { it.copy(city = city) } },
            placeholder = "مثلاً اصفهان",
            modifier = Modifier.testTag("wizard_city_input")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WizardStepConnection(
    draft: ProfileDraft,
    onPatch: ((ProfileDraft) -> ProfileDraft) -> Unit
) {
    val colors = EcoTheme.colors
    Text("مشخصات انشعاب برق", style = MaterialTheme.typography.titleLarge)

    WizardField(
        label = "شماره کنتور یا شناسه قبض",
        hint = "روی قبض برق، زیر شناسه قبض درج شده است"
    ) {
        WizardTextField(
            value = PersianFormat.faDigits(draft.connection.meterSerial),
            onValueChange = { raw ->
                val digits = PersianFormat.toLatinDigits(raw).filter { it.isDigit() }.take(24)
                onPatch { it.copy(connection = it.connection.copy(meterSerial = digits)) }
            },
            placeholder = "شماره کنتور",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.testTag("wizard_meter_input")
        )
    }

    WizardField(label = "نوع تعرفه") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tariff.entries.forEach { tariff ->
                val selected = draft.connection.tariffRaw == tariff.raw
                Surface(
                    onClick = { onPatch { it.copy(connection = it.connection.copy(tariffRaw = tariff.raw)) } },
                    shape = RoundedCornerShape(50),
                    color = if (selected) colors.ink else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = tariff.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.inkForeground else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    WizardField(label = "توان قراردادی انشعاب (کیلووات)") {
        WizardTextField(
            value = if (draft.connection.demandKw > 0) PersianFormat.fa(draft.connection.demandKw) else "",
            onValueChange = { raw ->
                val num = PersianFormat.toLatinDigits(raw).replace(',', '.').toDoubleOrNull() ?: 0.0
                onPatch { it.copy(connection = it.connection.copy(demandKw = num.coerceIn(0.0, 1000.0))) }
            },
            placeholder = "مثلاً ۵",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.testTag("wizard_demand_input")
        )
    }

    Spacer(Modifier.height(14.dp))
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("کنتور دوطرفه", style = MaterialTheme.typography.titleSmall)
                Text(
                    "شرط تزریق به شبکه و قرارداد خرید تضمینی",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
            }
            EcoSwitch(
                checked = draft.connection.bidirectional,
                onCheckedChange = { b ->
                    onPatch { it.copy(connection = it.connection.copy(bidirectional = b)) }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WizardStepAppliances(
    draft: ProfileDraft,
    onPatch: ((ProfileDraft) -> ProfileDraft) -> Unit
) {
    val colors = EcoTheme.colors
    Text("لوازم برقی اصلی", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(4.dp))
    Text(
        "هر لوازم را با تعداد و ساعت کار روزانه اضافه کنید.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
    )

    Spacer(Modifier.height(12.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RateTables.ACTIVE.scenario.appliances.forEach { spec ->
            Surface(
                onClick = {
                    onPatch { cur ->
                        val existing = cur.appliances.firstOrNull { it.kind == spec.kind }
                        val nextList = if (existing != null) {
                            cur.appliances.map { item ->
                                if (item.kind == spec.kind) item.copy(count = (item.count + 1).coerceAtMost(20))
                                else item
                            }
                        } else {
                            cur.appliances + ApplianceEntry(
                                id = "${spec.kind}-${cur.appliances.size + 1}",
                                kind = spec.kind,
                                label = spec.label,
                                ratedKw = spec.ratedKw,
                                count = 1,
                                hoursPerDay = if (spec.kind == "fridge") 24.0 else 4.0
                            )
                        }
                        cur.copy(appliances = nextList)
                    }
                },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("wizard_add_appliance_${spec.kind}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(spec.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    if (draft.appliances.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "هنوز لوازم برقی اضافه نشده است",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.appliances.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.label, style = MaterialTheme.typography.titleSmall)
                            TextButton(
                                onClick = {
                                    onPatch { cur ->
                                        cur.copy(appliances = cur.appliances.filterNot { it.id == item.id })
                                    }
                                }
                            ) {
                                Text("حذف", style = MaterialTheme.typography.labelSmall, color = colors.mutedText)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WizardCounter(
                                label = "تعداد",
                                value = item.count,
                                min = 1,
                                max = 20,
                                onChange = { nextCount ->
                                    onPatch { cur ->
                                        cur.copy(
                                            appliances = cur.appliances.map {
                                                if (it.id == item.id) it.copy(count = nextCount) else it
                                            }
                                        )
                                    }
                                }
                            )
                            WizardCounter(
                                label = "ساعت",
                                value = item.hoursPerDay.toInt(),
                                min = 0,
                                max = 24,
                                onChange = { nextHrs ->
                                    onPatch { cur ->
                                        cur.copy(
                                            appliances = cur.appliances.map {
                                                if (it.id == item.id) it.copy(hoursPerDay = nextHrs.toDouble()) else it
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WizardStepRoof(
    draft: ProfileDraft,
    onPatch: ((ProfileDraft) -> ProfileDraft) -> Unit
) {
    val colors = EcoTheme.colors
    Text("مشخصات بام", style = MaterialTheme.typography.titleLarge)

    WizardField(label = "مساحت قابل استفاده بام (متر مربع)") {
        WizardTextField(
            value = if (draft.roof.area > 0) PersianFormat.fa(draft.roof.area) else "",
            onValueChange = { raw ->
                val area = PersianFormat.toLatinDigits(raw).replace(',', '.').toDoubleOrNull() ?: 0.0
                onPatch { it.copy(roof = it.roof.copy(area = area.coerceIn(0.0, 5000.0))) }
            },
            placeholder = "مثلاً ۶۴",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.testTag("wizard_roof_area_input")
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            WizardField(label = "شیب (درجه)") {
                WizardTextField(
                    value = PersianFormat.fa(draft.roof.tilt),
                    onValueChange = { raw ->
                        val v = PersianFormat.toLatinDigits(raw).filter { it.isDigit() }.toIntOrNull() ?: 0
                        onPatch { it.copy(roof = it.roof.copy(tilt = v.coerceIn(0, 60))) }
                    },
                    placeholder = "۳۰",
                    keyboardType = KeyboardType.Number
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            WizardField(label = "سایه‌اندازی (٪)") {
                WizardTextField(
                    value = PersianFormat.fa(draft.roof.shadowPct),
                    onValueChange = { raw ->
                        val v = PersianFormat.toLatinDigits(raw).filter { it.isDigit() }.toIntOrNull() ?: 0
                        onPatch { it.copy(roof = it.roof.copy(shadowPct = v.coerceIn(0, 60))) }
                    },
                    placeholder = "۶",
                    keyboardType = KeyboardType.Number
                )
            }
        }
    }

    WizardField(label = "جهت اصلی بام") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LoadProfileCalc.AZIMUTHS.forEach { az ->
                val active = draft.roof.azimuth == az
                Surface(
                    onClick = { onPatch { it.copy(roof = it.roof.copy(azimuth = az)) } },
                    shape = RoundedCornerShape(50),
                    color = if (active) colors.ink else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = az,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) colors.inkForeground else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("بام مشاع", style = MaterialTheme.typography.titleSmall)
                Text(
                    "رضایت ۱۰۰٪ مالکان پیش از شروع پروژه لازم است",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
            }
            EcoSwitch(
                checked = draft.sharedOwnership,
                onCheckedChange = { shared ->
                    onPatch { cur ->
                        cur.copy(
                            sharedOwnership = shared,
                            owners = if (shared) {
                                cur.owners.copy(total = cur.owners.total.coerceAtLeast(2))
                            } else {
                                cur.owners.copy(total = 1, granted = 0)
                            }
                        )
                    }
                }
            )
        }
    }

    if (draft.sharedOwnership) {
        val pct = LoadProfileCalc.ownerConsentPct(draft)
        val ready = LoadProfileCalc.ownersReady(draft)
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WizardCounter(
                    label = "مالکان",
                    value = draft.owners.total,
                    min = 2,
                    max = 50,
                    onChange = { total ->
                        onPatch { cur ->
                            cur.copy(owners = cur.owners.copy(total = total, granted = cur.owners.granted.coerceAtMost(total)))
                        }
                    }
                )
                WizardCounter(
                    label = "رضایت داده‌اند",
                    value = draft.owners.granted,
                    min = 0,
                    max = draft.owners.total,
                    onChange = { granted ->
                        onPatch { cur -> cur.copy(owners = cur.owners.copy(granted = granted)) }
                    }
                )
                HBar(
                    ratio = pct / 100.0,
                    fillColor = if (ready) colors.success else colors.sunDeep
                )
                Text(
                    text = "${PersianFormat.fa(pct)}٪ رضایت ثبت‌شده" +
                        if (ready) " · آماده ادامه" else " · تا ۱۰۰٪ پروژه قابل شروع نیست",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.mutedText
                )
            }
        }
    }
}

@Composable
fun WizardStepLoadProfile(
    draft: ProfileDraft,
    billKwh: Int,
    onOpenBill: () -> Unit
) {
    val profile = LoadProfileCalc.buildLoadProfile(draft)
    val consentPct = LoadProfileCalc.ownerConsentPct(draft)
    val ready = LoadProfileCalc.ownersReady(draft)
    val limitText = when (profile.limitedBy) {
        CapacityLimit.LOAD -> "بر اساس مصرف سالانه شما"
        CapacityLimit.ROOF -> "محدود به مساحت بام"
        CapacityLimit.CAP -> "سقف مجاز ${PersianFormat.fa(RateTables.ACTIVE.loan.maxCapacityKwp)} کیلووات"
    }

    Text("پروفایل بار شما", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(4.dp))
    Text(
        "ظرفیت پیشنهادی از مصرف خودتان، مساحت بام و سقف مجاز محاسبه شده است.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
    )
    Spacer(Modifier.height(10.dp))
    InfoRow("مصرف روزانه لوازم", "${PersianFormat.fa(profile.dailyKwh)} kWh")
    InfoRow("توان هم‌زمان", "${PersianFormat.fa(profile.peakKw)} kW")
    InfoRow("مصرف سالانه", "${PersianFormat.fa(profile.annualConsumptionKwh)} kWh")
    InfoRow("قبض تأییدشده", "${PersianFormat.fa(billKwh)} kWh")
    InfoRow("ظرفیت پیشنهادی", "${PersianFormat.fa(profile.suggestedKwp)} کیلووات")
    InfoRow("تولید سالانه برآوردی", "${PersianFormat.fa(profile.expectedAnnualKwh)} kWh")
    InfoRow("محدودکننده ظرفیت", limitText, showDivider = false)

    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ScenarioLabel(text = RateTables.ACTIVE.label)
        EcoPill(text = "بازده ${PersianFormat.fa(profile.yieldPerKwp)} kWh/kW", tone = PillTone.WHITE, small = true)
    }

    if (!ready) {
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "برای شروع پروژه، رضایت ${PersianFormat.fa(100 - consentPct)}٪ باقی‌مانده مالکان مشاع لازم است.",
                style = MaterialTheme.typography.labelMedium,
                color = EcoTheme.colors.warn,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    Spacer(Modifier.height(10.dp))
    Surface(
        onClick = onOpenBill,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("بازبینی و تأیید قبض برق", style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
        }
    }
}

@Composable
fun WizardStepReview(draft: ProfileDraft) {
    val profile = LoadProfileCalc.buildLoadProfile(draft)
    val ready = LoadProfileCalc.ownersReady(draft)
    val colors = EcoTheme.colors

    Text("مرور و ثبت", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(10.dp))
    InfoRow("بخش", draft.sector?.label ?: "—")
    InfoRow("شهر", draft.city.ifBlank { "—" })
    InfoRow("شماره کنتور", PersianFormat.faDigits(draft.connection.meterSerial).ifBlank { "—" })
    InfoRow("تعرفه", Tariff.fromRaw(draft.connection.tariffRaw).label)
    InfoRow("توان قراردادی", "${PersianFormat.fa(draft.connection.demandKw)} کیلووات")
    InfoRow("لوازم برقی", "${PersianFormat.fa(draft.appliances.size)} مورد")
    InfoRow("مساحت بام", "${PersianFormat.fa(draft.roof.area)} متر مربع")
    InfoRow(
        "بام مشاع",
        if (draft.sharedOwnership) "${PersianFormat.fa(LoadProfileCalc.ownerConsentPct(draft))}٪ رضایت" else "خیر"
    )
    InfoRow("ظرفیت پیشنهادی", "${PersianFormat.fa(profile.suggestedKwp)} کیلووات", showDivider = false)

    Spacer(Modifier.height(10.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EcoPill(text = RateTables.ACTIVE.label, tone = PillTone.SUN, small = true)
        if (ready) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(14.dp))
                Text("قیدها تأیید شد", style = MaterialTheme.typography.labelSmall, color = colors.success)
            }
        } else {
            Text("رضایت مالکان کامل نیست", style = MaterialTheme.typography.labelSmall, color = colors.warn)
        }
    }
}

@Composable
private fun WizardField(label: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
        Spacer(Modifier.height(6.dp))
        content()
        if (!hint.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(hint, style = MaterialTheme.typography.labelSmall, color = EcoTheme.colors.mutedText)
        }
    }
}

@Composable
private fun WizardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = EcoTheme.colors.mutedText) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = EcoTheme.colors.ink,
            unfocusedBorderColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun WizardCounter(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = EcoTheme.colors.mutedText)
        IconButton(
            onClick = { onChange((value - 1).coerceAtLeast(min)) },
            enabled = value > min,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(EcoTheme.colors.ink.copy(alpha = 0.08f))
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "کاهش", modifier = Modifier.size(16.dp))
        }
        Text(
            text = PersianFormat.faDigits(value),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(26.dp),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = { onChange((value + 1).coerceAtMost(max)) },
            enabled = value < max,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(EcoTheme.colors.ink.copy(alpha = 0.08f))
        ) {
            Icon(Icons.Filled.Add, contentDescription = "افزایش", modifier = Modifier.size(16.dp))
        }
    }
}
