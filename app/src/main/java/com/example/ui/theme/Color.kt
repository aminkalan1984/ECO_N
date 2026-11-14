package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// EcoSolar design tokens translated from the oklch design system
val EcoCanvas = Color(0xFFF6F6F6)
val EcoCard = Color(0xFFFFFFFF)
val EcoInk = Color(0xFF1A1A1A)
val EcoInkForeground = Color(0xFFFCFCFC)
val EcoMuted = Color(0xFFEAEAEA)
val EcoMutedForeground = Color(0xFF7A7A7A)
val EcoBorder = Color(0xFFE4E4E4)

val EcoSun = Color(0xFFF4CE42)
val EcoSunDeep = Color(0xFFE8B025)
val EcoSunStrong = Color(0xFFEDC236)
val EcoSunSoft = Color(0xFFFCF3CF)
val EcoSunWash = Color(0xFFF7ECC5)

val EcoTrack = Color(0xFFE5E5E5)
val EcoTrackStrong = Color(0xFFCCCCCC)

val EcoWarn = Color(0xFFD97706)
val EcoWarnSoft = Color(0xFFFEF3C7)

val EcoSuccess = Color(0xFF16A34A)
val EcoSuccessSoft = Color(0xFFDCFCE7)

val EcoDanger = Color(0xFFDC2626)
val EcoDangerSoft = Color(0xFFFEE2E2)

val EcoDesk = Color(0xFFE9E7E0)

@Immutable
data class EcoExtendedColors(
    val sun: Color = EcoSun,
    val sunDeep: Color = EcoSunDeep,
    val sunStrong: Color = EcoSunStrong,
    val sunSoft: Color = EcoSunSoft,
    val sunWash: Color = EcoSunWash,
    val ink: Color = EcoInk,
    val inkForeground: Color = EcoInkForeground,
    val track: Color = EcoTrack,
    val trackStrong: Color = EcoTrackStrong,
    val warn: Color = EcoWarn,
    val warnSoft: Color = EcoWarnSoft,
    val success: Color = EcoSuccess,
    val successSoft: Color = EcoSuccessSoft,
    val danger: Color = EcoDanger,
    val dangerSoft: Color = EcoDangerSoft,
    val mutedText: Color = EcoMutedForeground,
    val cardBorder: Color = EcoBorder
)

val LocalEcoColors = staticCompositionLocalOf { EcoExtendedColors() }
