package com.ygoular.keepit.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal fun KeepItTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors,
        content = content,
    )
}

private val lightColors = lightColorScheme(
    primary = Color(0xFF245744),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBDD),
    onPrimaryContainer = Color(0xFF153A2C),
    surface = Color(0xFFF7F7F0),
    surfaceContainer = Color(0xFFEEEEE5),
    surfaceContainerLow = Color(0xFFFEFEF8),
    onSurface = Color(0xFF1D2420),
    onSurfaceVariant = Color(0xFF48554D),
    outline = Color(0xFF717E73),
    outlineVariant = Color(0xFFD0D9D0),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFADD8BD),
    onPrimary = Color(0xFF163B2B),
    primaryContainer = Color(0xFF284936),
    onPrimaryContainer = Color(0xFFE0F1E2),
    surface = Color(0xFF171E19),
    surfaceContainer = Color(0xFF222B24),
    surfaceContainerLow = Color(0xFF1E2620),
    onSurface = Color(0xFFE8EEE6),
    onSurfaceVariant = Color(0xFFBBC8BE),
    outline = Color(0xFF8E9D91),
    outlineVariant = Color(0xFF445249),
)
