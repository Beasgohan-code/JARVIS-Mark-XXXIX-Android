package com.jarvis.mark39.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class AppStyleId(val label: String) {
    JARVIS_CYAN("JARVIS Cyan"),
    NEON_PURPLE("Neon Purple"),
    EMERALD("Emerald"),
    AMBER("Amber Gold"),
    MINIMAL("Minimal Gray")
}

enum class WallpaperId(val label: String) {
    DEEP_SPACE("Deep Space"),
    AURORA("Aurora"),
    GRID("Neon Grid"),
    SOLID("Solid Surface"),
    SUNSET("Sunset")
}

data class StylePack(
    val id: AppStyleId,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

val STYLE_PACKS = mapOf(
    AppStyleId.JARVIS_CYAN to StylePack(AppStyleId.JARVIS_CYAN, Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF69F0AE)),
    AppStyleId.NEON_PURPLE to StylePack(AppStyleId.NEON_PURPLE, Color(0xFFD500F9), Color(0xFF651FFF), Color(0xFF00E5FF)),
    AppStyleId.EMERALD to StylePack(AppStyleId.EMERALD, Color(0xFF00E676), Color(0xFF1DE9B6), Color(0xFF76FF03)),
    AppStyleId.AMBER to StylePack(AppStyleId.AMBER, Color(0xFFFFC400), Color(0xFFFF6D00), Color(0xFFFFAB00)),
    AppStyleId.MINIMAL to StylePack(AppStyleId.MINIMAL, Color(0xFF90A4AE), Color(0xFF78909C), Color(0xFFB0BEC5))
)

fun colorSchemeFor(style: AppStyleId, light: Boolean): ColorScheme {
    val pack = STYLE_PACKS[style] ?: STYLE_PACKS[AppStyleId.JARVIS_CYAN]!!
    return if (light) {
        lightColorScheme(
            primary = pack.primary,
            onPrimary = Color.Black,
            secondary = pack.secondary,
            onSecondary = Color.White,
            tertiary = pack.tertiary,
            background = Color(0xFFF5F7FA),
            onBackground = Color(0xFF0D1117),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0D1117),
            surfaceVariant = Color(0xFFEEF1F5),
            onSurfaceVariant = Color(0xFF5C6B7A),
            error = Color(0xFFD32F2F),
            outline = Color(0xFFCFD8DC)
        )
    } else {
        darkColorScheme(
            primary = pack.primary,
            onPrimary = Color(0xFF001018),
            secondary = pack.secondary,
            onSecondary = Color.White,
            tertiary = pack.tertiary,
            background = Color(0xFF05070C),
            onBackground = Color(0xFFE6EDF3),
            surface = Color(0xFF0D1117),
            onSurface = Color(0xFFE6EDF3),
            surfaceVariant = Color(0xFF161B22),
            onSurfaceVariant = Color(0xFF8B949E),
            error = Color(0xFFFF6B6B),
            outline = Color(0xFF30363D)
        )
    }
}

fun wallpaperBrush(id: WallpaperId, primary: Color, light: Boolean): Brush {
    return when (id) {
        WallpaperId.DEEP_SPACE -> Brush.verticalGradient(
            if (light) listOf(Color(0xFFE8EEF5), Color(0xFFF5F7FA), Color(0xFFE0E7EF))
            else listOf(Color(0xFF05070C), Color(0xFF0A1220), Color(0xFF05070C))
        )
        WallpaperId.AURORA -> Brush.verticalGradient(
            if (light) listOf(Color(0xFFE0F7FA), Color(0xFFF3E5F5), Color(0xFFE8F5E9))
            else listOf(Color(0xFF061018), Color(0xFF0E1A2E), Color(0xFF0A1F18))
        )
        WallpaperId.GRID -> Brush.linearGradient(
            if (light) listOf(Color(0xFFF0F4F8), Color(0xFFE3EAF2))
            else listOf(Color(0xFF0A0E14), Color(0xFF121820))
        )
        WallpaperId.SOLID -> Brush.verticalGradient(
            listOf(
                if (light) Color(0xFFF5F7FA) else Color(0xFF0A0A0F),
                if (light) Color(0xFFF5F7FA) else Color(0xFF0A0A0F)
            )
        )
        WallpaperId.SUNSET -> Brush.verticalGradient(
            if (light) listOf(Color(0xFFFFF3E0), Color(0xFFFCE4EC), Color(0xFFE3F2FD))
            else listOf(Color(0xFF1A0A10), Color(0xFF1A1020), Color(0xFF0A1018))
        )
    }
}
