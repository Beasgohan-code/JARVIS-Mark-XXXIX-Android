package com.jarvis.mark39.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.mark39.domain.model.JarvisUiEvent
import com.jarvis.mark39.ui.viewmodels.JarvisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskPlannerScreen(
    onBack: () -> Unit,
    viewModel: JarvisViewModel = hiltViewModel()
) {
    var goal by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val lastAssistant = messages.lastOrNull { it.role.name == "ASSISTANT" }?.content

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Agent", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
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
            Text(
                "Describe a multi-step goal. JARVIS will use tools (search, memory, launch apps, open URLs, system actions) to complete it.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Goal") },
                placeholder = { Text("e.g. Search for latest SpaceX news and remember the summary") },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFF3A3A4A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.Gray
                )
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (goal.isNotBlank()) {
                        viewModel.onEvent(JarvisUiEvent.RunTask(goal.trim()))
                    }
                },
                enabled = goal.isNotBlank() && !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(if (uiState.isProcessing) "  Running…" else "  Execute task")
            }

            Spacer(Modifier.height(24.dp))
            Text("Example goals", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Search for weather in Istanbul and remember it",
                "List installed apps",
                "Open Wi‑Fi settings",
                "Search latest AI news then summarize"
            ).forEach { example ->
                Button(
                    onClick = { goal = example },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E2A)
                    )
                ) {
                    Text(example, color = Color.White)
                }
            }

            if (lastAssistant != null && lastAssistant.contains("Task", ignoreCase = true)) {
                Spacer(Modifier.height(24.dp))
                Text("Last agent result", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    lastAssistant,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E2A), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                )
            }
        }
    }
}
