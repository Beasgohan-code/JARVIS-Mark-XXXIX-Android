package com.jarvis.mark39.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.mark39.domain.model.JarvisUiEvent
import com.jarvis.mark39.domain.model.VoiceState
import com.jarvis.mark39.ui.components.VoiceOrb
import com.jarvis.mark39.ui.viewmodels.JarvisViewModel

@Composable
fun VoiceOrbScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToVision: () -> Unit = {},
    viewModel: JarvisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onEvent(JarvisUiEvent.StartListening)
    }

    fun requestMicAndListen() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED -> {
                viewModel.onEvent(JarvisUiEvent.StartListening)
            }
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val statusText = when (uiState.voiceState) {
        VoiceState.IDLE -> if (uiState.hasApiKey) "Tap the orb to speak" else "Set API key in Settings"
        VoiceState.LISTENING -> "Listening…"
        VoiceState.PROCESSING -> "Thinking…"
        VoiceState.SPEAKING -> "Speaking…"
        VoiceState.ERROR -> uiState.error ?: "Error"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0A0A12), Color(0xFF050508)))
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JARVIS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                IconButton(onClick = onNavigateToVision) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Vision", tint = Color.White)
                }
                IconButton(onClick = onNavigateToTasks) {
                    Icon(Icons.Default.PlaylistAddCheck, contentDescription = "Tasks", tint = Color.White)
                }
                IconButton(onClick = onNavigateToChat) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color.White)
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VoiceOrb(
                state = uiState.voiceState,
                size = 200.dp,
                onTap = {
                    when (uiState.voiceState) {
                        VoiceState.LISTENING -> viewModel.onEvent(JarvisUiEvent.StopListening)
                        else -> requestMicAndListen()
                    }
                },
                onLongPress = onNavigateToSettings
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            if (uiState.partialTranscript.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\"${uiState.partialTranscript}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            uiState.error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        Text(
            text = "Mark XXXIX  •  Gemini + Tools",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
