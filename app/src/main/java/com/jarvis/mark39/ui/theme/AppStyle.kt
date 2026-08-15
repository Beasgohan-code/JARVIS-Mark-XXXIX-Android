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
    MINIMAL("Minimal Gray"),
    MIDNIGHT("Midnight Blue"),
    ROSE("Rose Glass")
}

enum class WallpaperId(val label: String) {
    DEEP_SPACE("Deep Space"),
    AURORA("Aurora"),
    GRID("Neon Grid"),
    SOLID("Solid Surface"),
    SUNSET("Sunset"),
    MESH("Mesh Gradient"),
    VOID("Void")
}

data class StylePack(
    val id: AppStyleId,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

val STYLE_PACKS = mapOf(
    AppStyleId.JARVIS_CYAN to StylePack(AppStyleId.JARVIS_CYAN, Color(0xFF00F0FF), Color(0xFF7C4DFF), Color(0xFF69F0AE)),
    AppStyleId.NEON_PURPLE to StylePack(AppStyleId.NEON_PURPLE, Color(0xFFE040FB), Color(0xFF651FFF), Color(0xFF00E5FF)),
    AppStyleId.EMERALD to StylePack(AppStyleId.EMERALD, Color(0xFF00E676), Color(0xFF1DE9B6), Color(0xFFAEEA00)),
    AppStyleId.AMBER to StylePack(AppStyleId.AMBER, Color(0xFFFFC400), Color(0xFFFF6D00), Color(0xFFFFAB00)),
    AppStyleId.MINIMAL to StylePack(AppStyleId.MINIMAL, Color(0xFFB0BEC5), Color(0xFF78909C), Color(0xFFCFD8DC)),
    AppStyleId.MIDNIGHT to StylePack(AppStyleId.MIDNIGHT, Color(0xFF448AFF), Color(0xFF536DFE), Color(0xFF82B1FF)),
    AppStyleId.ROSE to StylePack(AppStyleId.ROSE, Color(0xFFFF80AB), Color(0xFFEA80FC), Color(0xFFFF4081))
)

fun colorSchemeFor(style: AppStyleId, light: Boolean): ColorScheme {
    val pack = STYLE_PACKS[style] ?: STYLE_PACKS[AppStyleId.JARVIS_CYAN]!!
    return if (light) {
        lightColorScheme(
            primary = pack.primary,
            onPrimary = Color(0xFF001018),
            secondary = pack.secondary,
            onSecondary = Color.White,
            tertiary = pack.tertiary,
            background = Color(0xFFF4F6FA),
            onBackground = Color(0xFF0B0F14),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0B0F14),
            surfaceVariant = Color(0xFFE8ECF2),
            onSurfaceVariant = Color(0xFF5A6570),
            error = Color(0xFFE53935),
            outline = Color(0xFFD0D7DE)
        )
    } else {
        darkColorScheme(
            primary = pack.primary,
            onPrimary = Color(0xFF001018),
            secondary = pack.secondary,
            onSecondary = Color.White,
            tertiary = pack.tertiary,
            background = Color(0xFF03050A),
            onBackground = Color(0xFFE8EEF5),
            surface = Color(0xFF0A0E16),
            onSurface = Color(0xFFE8EEF5),
            surfaceVariant = Color(0xFF121826),
            onSurfaceVariant = Color(0xFF9AA5B5),
            error = Color(0xFFFF6B7A),
            outline = Color(0xFF2A3344)
        )
    }
}

fun wallpaperBrush(id: WallpaperId, primary: Color, light: Boolean): Brush {
    return when (id) {
        WallpaperId.DEEP_SPACE -> Brush.verticalGradient(
            if (light) listOf(Color(0xFFE8EEF8), Color(0xFFF5F7FC), Color(0xFFDDE5F2))
            else listOf(Color(0xFF02040A), Color(0xFF0A1220), Color(0xFF050810))
        )
        WallpaperId.AURORA -> Brush.verticalGradient(
            if (light) listOf(Color(0xFFE0F7FA), Color(0xFFF3E5F5), Color(0xFFE8F5E9))
            else listOf(Color(0xFF041018), Color(0xFF0C1A2E), Color(0xFF0A1C18))
        )
        WallpaperId.GRID -> Brush.linearGradient(
            if (light) listOf(Color(0xFFF0F4F8), Color(0xFFE3EAF2))
            else listOf(Color(0xFF070B12), Color(0xFF101820))
        )
        WallpaperId.SOLID -> Brush.verticalGradient(
            listOf(
                if (light) Color(0xFFF4F6FA) else Color(0xFF03050A),
                if (light) Color(0xFFF4F6FA) else Color(0xFF03050A)
            )
        )
        WallpaperId.SUNSET -> Brush.verticalGradient(
            if (light) listOf(Color(0xFFFFF3E0), Color(0xFFFCE4EC), Color(0xFFE3F2FD))
            else listOf(Color(0xFF180A10), Color(0xFF1A1024), Color(0xFF080E18))
        )
        WallpaperId.MESH -> Brush.linearGradient(
            colors = if (light)
                listOf(Color(0xFFE3F2FD), Color(0xFFF3E5F5), Color(0xFFE8F5E9))
            else
                listOf(Color(0xFF0A1628), primary.copy(alpha = 0.25f), Color(0xFF12081C), Color(0xFF061018))
        )
        WallpaperId.VOID -> Brush.radialGradient(
            colors = if (light)
                listOf(Color(0xFFFFFFFF), Color(0xFFE8ECF2))
            else
                listOf(primary.copy(alpha = 0.12f), Color(0xFF02040A), Color(0xFF000000))
        )
    }
}

/** Glass panel colors */
object Glass {
    val panelDark = Color(0xCC0C121C)
    val panelLight = Color(0xE6FFFFFF)
    val strokeDark = Color(0x33FFFFFF)
    val strokeLight = Color(0x22000000)
}
