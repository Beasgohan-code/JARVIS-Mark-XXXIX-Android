package com.jarvis.mark39.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.mark39.domain.model.JarvisUiEvent
import com.jarvis.mark39.domain.model.VoiceState
import com.jarvis.mark39.ui.components.AmbientGlowBackground
import com.jarvis.mark39.ui.components.GlassBar
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
    ) { granted -> if (granted) viewModel.onEvent(JarvisUiEvent.StartListening) }

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
        AmbientGlowBackground(modifier = Modifier.fillMaxSize(), color = primary)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Text(
                text = "JARVIS",
                color = primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )
            Text(
                text = "MARK XXXIX",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.weight(1f))

            VoiceOrb(
                state = uiState.voiceState,
                size = 240.dp,
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            AnimatedVisibility(
                visible = uiState.partialTranscript.isNotBlank(),
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut()
            ) {
                Text(
                    text = "“${uiState.partialTranscript}”",
                    color = primary.copy(alpha = 0.95f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp)
                )
            }

            uiState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp, start = 20.dp, end = 20.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            .clip(RoundedCornerShape(22.dp))
                            .background(primary.copy(alpha = 0.08f))
                            .border(1.dp, primary.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                            .clickable { viewModel.onEvent(JarvisUiEvent.SendText(phrase)) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            GlassBar(modifier = Modifier.padding(bottom = 28.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun NavChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.12f))
                .border(1.dp, primary.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 11.sp)
    }
}
