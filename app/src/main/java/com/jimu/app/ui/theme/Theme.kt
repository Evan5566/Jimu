package com.jimu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val JimuLightColorScheme = lightColorScheme(
    primary = AccentBlueStrong,
    onPrimary = TextPrimaryDark,
    primaryContainer = CardBlue,
    onPrimaryContainer = TextPrimaryLight,

    secondary = AccentBlue,
    onSecondary = TextPrimaryDark,

    background = BackgroundBlue,
    onBackground = TextPrimaryLight,

    surface = SurfaceWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = TextSecondaryLight,

    outline = BorderSoft
)

private val JimuDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimaryDark,
    primaryContainer = NightBlue,
    onPrimaryContainer = TextPrimaryDark,

    secondary = AccentBlue,
    onSecondary = TextPrimaryDark,

    background = DeepNavy,
    onBackground = TextPrimaryDark,

    surface = NightBlue,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextPrimaryDark,

    outline = OceanBlue
)

@Composable
fun JimuTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) JimuLightColorScheme else JimuLightColorScheme,
        typography = Typography,
        content = content
    )
}