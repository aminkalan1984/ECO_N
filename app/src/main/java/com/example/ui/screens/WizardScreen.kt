package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.LoadProfileCalc
import com.example.domain.PersianFormat
import com.example.ui.components.EcoScreenShell
import com.example.ui.components.HBar
import com.example.ui.components.InkButton
import com.example.ui.components.ScreenHeader
import com.example.ui.components.WhiteButton
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel

@Composable
fun WizardScreen(
    ecoViewModel: EcoViewModel,
    onBack: () -> Unit,
    onOpenBill: () -> Unit,
    onCompleted: () -> Unit
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val draft by ecoViewModel.wizardDraft.collectAsStateWithLifecycle()
    val saveStatus by ecoViewModel.wizardSaveStatus.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors

    var stepError by remember { mutableStateOf<String?>(null) }
    val stepCount = LoadProfileCalc.WIZARD_STEPS.size
    val step = draft.step.coerceIn(0, stepCount - 1)
    val stepTitle = LoadProfileCalc.WIZARD_STEPS[step]
    val sharedReady = LoadProfileCalc.ownersReady(draft)

    EcoScreenShell(
        topContent = {
            ScreenHeader(
                title = "پروفایل بار",
                subtitle = "$stepTitle · گام ${PersianFormat.fa(step + 1)} از ${PersianFormat.fa(stepCount)}",
                onStartClick = onBack
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HBar(
                    ratio = (step + 1).toDouble() / stepCount.toDouble(),
                    fillColor = colors.sun,
                    trackColor = colors.ink.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${PersianFormat.fa(step + 1)}/${PersianFormat.fa(stepCount)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = saveStatus ?: if (draft.updatedAt > 0L) {
                    "پیش‌نویس ذخیره شد · ${PersianFormat.tehranClock(draft.updatedAt)}"
                } else {
                    "پیش‌نویس به‌صورت خودکار ذخیره می‌شود"
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.mutedText
            )
        },
        sunContent = {
            when (step) {
                0 -> WizardStepSectorLocation(
                    draft = draft,
                    onPatch = { transform ->
                        stepError = null
                        ecoViewModel.updateWizardDraft(transform)
                    }
                )
                1 -> WizardStepConnection(
                    draft = draft,
                    onPatch = { transform ->
                        stepError = null
                        ecoViewModel.updateWizardDraft(transform)
                    }
                )
                2 -> WizardStepAppliances(
                    draft = draft,
                    onPatch = { transform ->
                        stepError = null
                        ecoViewModel.updateWizardDraft(transform)
                    }
                )
                3 -> WizardStepRoof(
                    draft = draft,
                    onPatch = { transform ->
                        stepError = null
                        ecoViewModel.updateWizardDraft(transform)
                    }
                )
                4 -> WizardStepLoadProfile(
                    draft = draft,
                    billKwh = ecoState.bill.totalKwh,
                    onOpenBill = onOpenBill
                )
                else -> WizardStepReview(draft = draft)
            }

            if (!stepError.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stepError.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.warn,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (step > 0) {
                    WhiteButton(
                        text = "مرحله قبل",
                        onClick = {
                            stepError = null
                            ecoViewModel.updateWizardDraft { it.copy(step = (step - 1).coerceAtLeast(0)) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wizard_prev_button")
                    )
                }
                if (step < stepCount - 1) {
                    InkButton(
                        text = "مرحله بعد",
                        onClick = {
                            val err = LoadProfileCalc.validateStep(step, draft)
                            if (err != null) {
                                stepError = err
                            } else {
                                stepError = null
                                ecoViewModel.updateWizardDraft {
                                    it.copy(step = (step + 1).coerceAtMost(stepCount - 1))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wizard_next_button")
                    )
                } else {
                    InkButton(
                        text = "ثبت پروفایل و ساخت طرح",
                        enabled = sharedReady,
                        onClick = { ecoViewModel.completeWizard(onCompleted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wizard_complete_button")
                    )
                }
            }
        }
    )
}
