package com.jarvis.mark39.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

data class JarvisThemeState(
    val style: AppStyleId = AppStyleId.JARVIS_CYAN,
    val lightMode: Boolean = false,
    val wallpaper: WallpaperId = WallpaperId.DEEP_SPACE
)

val LocalJarvisTheme = staticCompositionLocalOf { JarvisThemeState() }

@Composable
fun JARVISTheme(
    state: JarvisThemeState = JarvisThemeState(),
    content: @Composable () -> Unit
) {
    val scheme = colorSchemeFor(state.style, state.lightMode)
    CompositionLocalProvider(LocalJarvisTheme provides state) {
        MaterialTheme(
            colorScheme = scheme,
            typography = JarvisTypography,
            content = content
        )
    }
}
