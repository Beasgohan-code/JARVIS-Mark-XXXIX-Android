package com.jarvis.mark39.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColors = darkColorScheme(
    primary = Color(0xFF00E5FF),          // Cyan accent
    onPrimary = Color.Black,
    secondary = Color(0xFF76FF03),        // Green accent
    onSecondary = Color.Black,
    tertiary = Color(0xFFFF4081),
    background = Color(0xFF0A0A0F),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF12121A),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E2A),
    onSurfaceVariant = Color(0xFFB0B0C0),
    error = Color(0xFFFF5252),
    outline = Color(0xFF3A3A4A)
)

@Composable
fun JARVISTheme(
    darkTheme: Boolean = true, // Always dark for JARVIS aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColors,
        typography = JarvisTypography,
        content = content
    )
}
