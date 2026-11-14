package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val EcoLightColorScheme = lightColorScheme(
    primary = EcoInk,
    onPrimary = EcoInkForeground,
    primaryContainer = EcoSun,
    onPrimaryContainer = EcoInk,
    secondary = EcoSunDeep,
    onSecondary = EcoInk,
    secondaryContainer = EcoSunSoft,
    onSecondaryContainer = EcoInk,
    tertiary = EcoSuccess,
    onTertiary = EcoInkForeground,
    background = EcoCanvas,
    onBackground = EcoInk,
    surface = EcoCard,
    onSurface = EcoInk,
    surfaceVariant = EcoMuted,
    onSurfaceVariant = EcoMutedForeground,
    outline = EcoBorder,
    outlineVariant = EcoTrackStrong,
    error = EcoDanger,
    onError = EcoInkForeground,
    errorContainer = EcoDangerSoft,
    onErrorContainer = EcoDanger
)

val EcoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object EcoTheme {
    val colors: EcoExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEcoColors.current
}

@Composable
fun EcoSolarTheme(
    content: @Composable () -> Unit
) {
    val extendedColors = EcoExtendedColors()
    CompositionLocalProvider(
        LocalEcoColors provides extendedColors,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = EcoLightColorScheme,
            typography = EcoTypography,
            shapes = EcoShapes,
            content = content
        )
    }
}
