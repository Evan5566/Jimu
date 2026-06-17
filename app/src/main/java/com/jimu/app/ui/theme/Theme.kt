package com.jimu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = selectJimuColorScheme(darkTheme),
        typography = Typography,
        content = content
    )
}

internal fun selectJimuColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) JimuDarkColorScheme else JimuLightColorScheme
}
