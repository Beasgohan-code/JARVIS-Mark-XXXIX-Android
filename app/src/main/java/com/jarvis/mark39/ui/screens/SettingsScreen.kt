package com.jarvis.mark39.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.mark39.data.repository.SettingsRepository
import com.jarvis.mark39.ui.viewmodels.JarvisViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: JarvisViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SettingsEntryPoint::class.java
        ).settingsRepository()
    }
    val uiState by viewModel.uiState.collectAsState()

    var geminiKey by remember { mutableStateOf(settings.getGeminiApiKey()) }
    var openRouterKey by remember { mutableStateOf(settings.getOpenRouterApiKey()) }
    var confirmActions by remember { mutableStateOf(settings.isConfirmBeforeAction()) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF12121A))
            )
        },
        containerColor = Color(0xFF0A0A0F)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("API Keys", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Gemini (required) — aistudio.google.com", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = openRouterKey,
                onValueChange = { openRouterKey = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("OpenRouter API Key (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    settings.setGeminiApiKey(geminiKey)
                    settings.setOpenRouterApiKey(openRouterKey)
                    viewModel.refreshApiKeyStatus()
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saved) "Saved ✓" else "Save keys") }

            Spacer(Modifier.height(28.dp))
            Text("Safety", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Confirm before system actions", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = confirmActions, onCheckedChange = {
                    confirmActions = it
                    settings.setConfirmBeforeAction(it)
                })
            }

            Spacer(Modifier.height(28.dp))
            Text("System access", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open Accessibility settings (JARVIS control)") }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (viewModel.canDrawOverlays()) {
                        if (uiState.overlayActive) viewModel.stopOverlayBubble()
                        else viewModel.startOverlayBubble()
                    } else {
                        context.startActivity(viewModel.overlayPermissionIntent())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        !viewModel.canDrawOverlays() -> "Grant overlay permission"
                        uiState.overlayActive -> "Stop floating bubble"
                        else -> "Start floating bubble"
                    }
                )
            }

            if (uiState.screenVisionActive) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.stopScreenVision() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Stop screen vision") }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = """Voice phone control (enable Accessibility first):
• go home / go back / recents / lock phone
• open Chrome / open WhatsApp
• click Login / scroll down / swipe left
• read screen / type hello
• call 5551234 / volume up / mute
• navigate to airport / set alarm 7 30
• search latest news / screenshot

Permissions: Mic, Camera, Notifications, Overlay, Accessibility, Phone (optional for direct call).""",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color(0xFF3A3A4A),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Color.Gray,
    cursorColor = MaterialTheme.colorScheme.primary
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun settingsRepository(): SettingsRepository
}
