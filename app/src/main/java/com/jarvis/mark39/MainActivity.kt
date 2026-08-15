package com.jarvis.mark39

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.jarvis.mark39.data.repository.SettingsRepository
import com.jarvis.mark39.ui.navigation.JarvisNavGraph
import com.jarvis.mark39.ui.screens.LockScreen
import com.jarvis.mark39.ui.theme.AppStyleId
import com.jarvis.mark39.ui.theme.JARVISTheme
import com.jarvis.mark39.ui.theme.JarvisThemeState
import com.jarvis.mark39.ui.theme.WallpaperId
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThemeRoot { settings ->
                SideEffect {
                    if (settings.isHideFromRecents()) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    }
                }
                var unlocked by remember {
                    mutableStateOf(
                        !settings.isAppLockEnabled() || settings.getAppPin().isBlank()
                    )
                }
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = unlocked,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "lock"
                    ) { isOpen ->
                        if (!isOpen) {
                            LockScreen(
                                expectedPin = settings.getAppPin(),
                                onUnlocked = { unlocked = true }
                            )
                        } else {
                            val navController = rememberNavController()
                            JarvisNavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryMain {
    fun settingsRepository(): SettingsRepository
}

@Composable
fun ThemeRoot(content: @Composable (SettingsRepository) -> Unit) {
    val context = LocalContext.current
    val settings = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SettingsEntryMain::class.java
        ).settingsRepository()
    }
    val state = remember {
        JarvisThemeState(
            style = runCatching { AppStyleId.valueOf(settings.getStyleId()) }.getOrDefault(AppStyleId.JARVIS_CYAN),
            lightMode = settings.isLightMode(),
            wallpaper = runCatching { WallpaperId.valueOf(settings.getWallpaperId()) }.getOrDefault(WallpaperId.DEEP_SPACE)
        )
    }
    JARVISTheme(state = state) {
        content(settings)
    }
}
