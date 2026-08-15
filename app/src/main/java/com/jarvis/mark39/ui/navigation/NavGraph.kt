package com.jarvis.mark39.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jarvis.mark39.ui.screens.ChatScreen
import com.jarvis.mark39.ui.screens.SettingsScreen
import com.jarvis.mark39.ui.screens.TaskPlannerScreen
import com.jarvis.mark39.ui.screens.VisionScreen
import com.jarvis.mark39.ui.screens.VoiceOrbScreen

object Routes {
    const val VOICE = "voice"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val TASKS = "tasks"
    const val VISION = "vision"
}

@Composable
fun JarvisNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.VOICE) {
        composable(Routes.VOICE) {
            VoiceOrbScreen(
                onNavigateToChat = { navController.navigate(Routes.CHAT) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToTasks = { navController.navigate(Routes.TASKS) },
                onNavigateToVision = { navController.navigate(Routes.VISION) }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TASKS) {
            TaskPlannerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.VISION) {
            VisionScreen(onBack = { navController.popBackStack() })
        }
    }
}
