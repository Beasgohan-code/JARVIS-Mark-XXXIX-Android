package com.jarvis.mark39.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.mark39.domain.model.JarvisUiEvent
import com.jarvis.mark39.domain.model.VoiceState
import com.jarvis.mark39.ui.components.VoiceOrb
import com.jarvis.mark39.ui.theme.LocalJarvisTheme
import com.jarvis.mark39.ui.theme.wallpaperBrush
import com.jarvis.mark39.ui.viewmodels.JarvisViewModel

@Composable
fun VoiceOrbScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVision: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    viewModel: JarvisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalJarvisTheme.current
    val primary = MaterialTheme.colorScheme.primary

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onEvent(JarvisUiEvent.StartListening)
    }

    fun requestMicAndListen() {
        micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val statusText = when (uiState.voiceState) {
        VoiceState.IDLE -> "Tap the orb to speak"
        VoiceState.LISTENING -> "Listening…"
        VoiceState.PROCESSING -> "Thinking…"
        VoiceState.SPEAKING -> "Speaking…"
        VoiceState.ERROR -> "Something went wrong"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wallpaperBrush(theme.wallpaper, primary, theme.lightMode))
    ) {
        // Ambient glow behind orb
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF00E5FF).copy(alpha = 0.12f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = "JARVIS",
                color = Color(0xFF00E5FF),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(
                text = "Mark XXXIX",
                color = Color.White.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.weight(1f))

            VoiceOrb(
                state = uiState.voiceState,
                size = 220.dp,
                onTap = {
                    when (uiState.voiceState) {
                        VoiceState.LISTENING -> viewModel.onEvent(JarvisUiEvent.StopListening)
                        else -> requestMicAndListen()
                    }
                },
                onLongPress = onNavigateToSettings
            )

            Spacer(Modifier.height(28.dp))
            Text(
                text = statusText,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            if (uiState.partialTranscript.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "“${uiState.partialTranscript}”",
                    color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            uiState.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = it,
                    color = Color(0xFFFF6B6B),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.weight(1f))


            // Quick voice-style actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "What can you do?",
                    "go home",
                    "volume up",
                    "open Chrome",
                    "search news"
                ).forEach { phrase ->
                    Text(
                        text = phrase,
                        color = primary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .clickable {
                                viewModel.onEvent(JarvisUiEvent.SendText(phrase))
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Glass bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF12181F).copy(alpha = 0.92f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavChip(Icons.Default.Chat, "Chat", onNavigateToChat)
                NavChip(Icons.Default.CameraAlt, "Vision", onNavigateToVision)
                NavChip(Icons.Default.TaskAlt, "Tasks", onNavigateToTasks)
                NavChip(Icons.Default.Settings, "Settings", onNavigateToSettings)
            }
        }
    }
}

@Composable
private fun NavChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}
