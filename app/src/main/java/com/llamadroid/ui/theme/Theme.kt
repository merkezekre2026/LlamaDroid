package com.llamadroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.llamadroid.domain.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF365B4D),
    secondary = androidx.compose.ui.graphics.Color(0xFF626A36),
    tertiary = androidx.compose.ui.graphics.Color(0xFF6D5367),
    background = androidx.compose.ui.graphics.Color(0xFFFAFAF8),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFA6D0BE),
    secondary = androidx.compose.ui.graphics.Color(0xFFD0D79A),
    tertiary = androidx.compose.ui.graphics.Color(0xFFDCB8D1),
    background = androidx.compose.ui.graphics.Color(0xFF111412),
    surface = androidx.compose.ui.graphics.Color(0xFF1A1D1B),
)

@Composable
fun LlamaDroidTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
