package com.vasudev.ai.master.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3A4272),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF0B1026),
    secondary = Color(0xFFB25E00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF2E2000),
    background = Color(0xFFF8F5EF),
    onBackground = Color(0xFF1B1B22),
    surface = Color(0xFFF8F5EF),
    onSurface = Color(0xFF1B1B22),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF45464F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF151B3B),
    primaryContainer = Color(0xFF3A4272),
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFFFFB86F),
    onSecondary = Color(0xFF2E2000),
    background = Color(0xFF0B1026),
    onBackground = Color(0xFFE4E1EC),
    surface = Color(0xFF0B1026),
    onSurface = Color(0xFFE4E1EC),
    surfaceVariant = Color(0xFF262B45),
    onSurfaceVariant = Color(0xFFC6C5D0)
)

@Composable
fun MyAiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
