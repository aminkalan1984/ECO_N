package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.AuthOtpEngine
import com.example.domain.PersianFormat
import com.example.ui.components.Footnote
import com.example.ui.components.InkButton
import com.example.ui.components.SunMark
import com.example.ui.theme.EcoTheme
import com.example.ui.viewmodel.EcoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    ecoViewModel: EcoViewModel,
    onLoginSuccess: () -> Unit
) {
    val otpUi by ecoViewModel.otpUi.collectAsStateWithLifecycle()
    val colors = EcoTheme.colors
    var consentOpen by remember { mutableStateOf(false) }

    val phoneValid = AuthOtpEngine.isValidIranMobile(otpUi.phoneInput)
    val canVerify = otpUi.codeInput.length == AuthOtpEngine.OTP_LENGTH && otpUi.acceptedTerms

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(colors.sun),
                contentAlignment = Alignment.Center
            ) {
                SunMark(modifier = Modifier.size(76.dp), color = colors.ink)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedText,
                textAlign = TextAlign.Center
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = colors.sun
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 26.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "ورود با شماره موبایل",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))

                if (!otpUi.stepIsCode) {
                    Text(
                        text = "شماره موبایل خود را وارد کنید تا کد تأیید ارسال شود",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                    )
                    Spacer(Modifier.height(16.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = PersianFormat.faDigits(otpUi.phoneInput),
                            onValueChange = { raw ->
                                val digits = PersianFormat.toLatinDigits(raw).filter { it.isDigit() }.take(11)
                                ecoViewModel.onPhoneChanged(digits)
                            },
                            placeholder = {
                                Text(
                                    text = "۰۹۱۳ ۰۰۰ ۰۰۰۰",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = colors.mutedText
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                letterSpacing = 3.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (phoneValid) ecoViewModel.requestOtp() }
                            ),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = colors.ink,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("login_phone_input")
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "کد ۶ رقمی به ${PersianFormat.formatPhone(otpUi.phoneInput)} پیامک شد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { ecoViewModel.editPhoneNumber() },
                            modifier = Modifier.testTag("edit_phone_button")
                        ) {
                            Text(
                                text = "ویرایش شماره",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.ink,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = PersianFormat.faDigits(otpUi.codeInput),
                            onValueChange = { ecoViewModel.onCodeChanged(it) },
                            placeholder = {
                                Text(
                                    text = "— — — — — —",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = colors.mutedText
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                letterSpacing = 8.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (canVerify) ecoViewModel.verifyOtp(onLoginSuccess) }
                            ),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = colors.ink,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("login_otp_input")
                        )
                    }

                    if (!otpUi.devPreviewCode.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, colors.ink.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "کد یکبارمصرف آزمایشی (نسخه محلی)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = PersianFormat.faDigits(otpUi.devPreviewCode.orEmpty()),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 6.sp
                                    ),
                                    modifier = Modifier.testTag("dev_otp_code")
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (otpUi.resendCountdown > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "ارسال مجدد کد تا ${PersianFormat.faTimer(otpUi.resendCountdown)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
                                )
                            }
                        } else {
                            TextButton(
                                onClick = { ecoViewModel.requestOtp() },
                                enabled = !otpUi.busy,
                                modifier = Modifier.testTag("resend_otp_button")
                            ) {
                                Text(
                                    text = "ارسال مجدد کد",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.ink,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }
                }

                if (!otpUi.error.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = otpUi.error.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.warn,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { ecoViewModel.onAcceptedTermsChanged(!otpUi.acceptedTerms) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (otpUi.acceptedTerms) colors.ink else MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (otpUi.acceptedTerms) 0.dp else 2.dp,
                                    color = if (otpUi.acceptedTerms) Color.Transparent else colors.trackStrong,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (otpUi.acceptedTerms) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "موافقت با شرایط",
                                    tint = colors.inkForeground,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "با شرایط استفاده و رضایت‌نامه اشتراک داده قبض و انشعاب برق موافقم.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "مشاهده متن رضایت‌نامه",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .clickable { consentOpen = true }
                                    .padding(vertical = 4.dp)
                                    .testTag("view_consent_link")
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                if (!otpUi.stepIsCode) {
                    InkButton(
                        text = if (otpUi.busy) "در حال ارسال…" else "دریافت کد تأیید",
                        onClick = { ecoViewModel.requestOtp() },
                        enabled = phoneValid && !otpUi.busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_otp_button")
                    )
                } else {
                    InkButton(
                        text = if (otpUi.busy) "در حال بررسی…" else "تأیید و ورود",
                        onClick = { ecoViewModel.verifyOtp(onLoginSuccess) },
                        enabled = canVerify && !otpUi.busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_otp_button")
                    )
                }

                Footnote(
                    text = "داده‌ها فقط روی دستگاه شما و در سرورهای مجاز نگهداری می‌شود",
                    icon = Icons.Filled.Lock
                )
            }
        }
    }

    if (consentOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { consentOpen = false },
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
                    text = "رضایت‌نامه اشتراک داده",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "نسخه ۱,۲ — آبان ۱۴۰۵",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "با تأیید این رضایت‌نامه، به اکوسولار اجازه می‌دهید اطلاعات قبض برق، شناسه قبض و مشخصات انشعاب شما را برای تحلیل مصرف و طراحی نیروگاه استفاده کند.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "داده‌ها بدون اجازه شما با هیچ شخص ثالثی به اشتراک گذاشته نمی‌شوند و هر زمان بخواهید می‌توانید این رضایت را از بخش پروفایل لغو کنید.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))
                InkButton(
                    text = "خواندم و موافقم",
                    onClick = {
                        ecoViewModel.onAcceptedTermsChanged(true)
                        consentOpen = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("consent_agree_button")
                )
            }
        }
    }
}
