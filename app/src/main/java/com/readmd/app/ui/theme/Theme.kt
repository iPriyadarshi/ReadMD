package com.readmd.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.annotation.StringRes
import com.readmd.app.R

/** Persisted by ordinal: append new modes at the end so saved values keep their meaning. */
enum class ThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
    SEPIA(R.string.theme_sepia),
    BLACK(R.string.theme_black);

    companion object {
        fun fromOrdinal(value: Int): ThemeMode =
            entries.getOrElse(value) { SYSTEM }
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E9C),
    secondary = Color(0xFF4A6572),
    tertiary = Color(0xFF00695C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF90A4AE),
    tertiary = Color(0xFF4DB6AC),
)

// Warm paper tones; body text is #5B4636 on #F4ECD8 (about 7:1 contrast).
private val SepiaColors = lightColorScheme(
    primary = Color(0xFF8B5A2B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8D4B0),
    onPrimaryContainer = Color(0xFF3B2A1A),
    secondary = Color(0xFF7A6A53),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3D3AE),
    onSecondaryContainer = Color(0xFF3B2A1A),
    tertiary = Color(0xFF6B7F3A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0C987),
    onTertiaryContainer = Color(0xFF3B2A1A),
    background = Color(0xFFF4ECD8),
    onBackground = Color(0xFF5B4636),
    surface = Color(0xFFF4ECD8),
    onSurface = Color(0xFF5B4636),
    surfaceVariant = Color(0xFFE8DCC0),
    onSurfaceVariant = Color(0xFF6B5A47),
    surfaceContainerLowest = Color(0xFFFAF4E6),
    surfaceContainerLow = Color(0xFFEFE5CD),
    surfaceContainer = Color(0xFFEAE0C6),
    surfaceContainerHigh = Color(0xFFE6DABF),
    surfaceContainerHighest = Color(0xFFE1D4B8),
    outline = Color(0xFF8C7A63),
    outlineVariant = Color(0xFFD3C4A5),
)

// Pure-black backgrounds for OLED screens; accents come from the underlying dark scheme.
private fun ColorScheme.toTrueBlack() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF1C1C1C),
)

/** Replaces the primary color with [accent] and derives matching on/container colors. */
private fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val container = lerp(accent, if (dark) Color.Black else Color.White, 0.75f)
    return copy(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
        primaryContainer = container,
        onPrimaryContainer = if (dark) Color.White else Color.Black,
        surfaceTint = accent,
    )
}

@Composable
fun ReadMdTheme(
    themeMode: ThemeMode,
    accent: AccentColor = AccentColor.DEFAULT,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT, ThemeMode.SEPIA -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val colorScheme = when {
        themeMode == ThemeMode.SEPIA -> SepiaColors
        themeMode == ThemeMode.BLACK -> (if (dynamicColor) dynamicDarkColorScheme(context) else DarkColors).toTrueBlack()
        dark && dynamicColor -> dynamicDarkColorScheme(context)
        !dark && dynamicColor -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }.let { scheme ->
        if (accent == AccentColor.DEFAULT || themeMode == ThemeMode.SEPIA) {
            scheme
        } else {
            scheme.withAccent(accent.swatch(dark), dark)
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
