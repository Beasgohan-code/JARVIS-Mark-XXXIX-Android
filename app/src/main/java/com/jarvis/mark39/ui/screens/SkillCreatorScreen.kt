package com.jarvis.mark39.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.mark39.ui.viewmodels.JarvisViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillCreatorScreen(
    onBack: () -> Unit,
    viewModel: JarvisViewModel = hiltViewModel()
) {
    val skills by viewModel.customSkills.collectAsState()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary
    val card = Color(0xCC0C121C)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Skill Creator", color = Color.White)
                        Text(
                            "Custom skills inject into JARVIS prompts",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8B949E)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF03050A))
            )
        },
        containerColor = Color(0xFF03050A)
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("New skill", color = primary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Code Reviewer") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(primary)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("When to use this skill") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(primary)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    placeholder = { Text("Detailed behavior rules for JARVIS…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(primary)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank() && instructions.isNotBlank()) {
                            scope.launch {
                                viewModel.addCustomSkill(name, description, instructions)
                                name = ""; description = ""; instructions = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Create skill", color = Color.Black)
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Your skills", color = primary, style = MaterialTheme.typography.titleMedium)
            }
            items(skills, key = { it.id }) { skill ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(card)
                        .border(1.dp, primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(skill.name, color = Color.White, style = MaterialTheme.typography.titleSmall)
                            if (skill.description.isNotBlank()) {
                                Text(skill.description, color = Color(0xFF8B949E), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Switch(
                            checked = skill.enabled,
                            onCheckedChange = {
                                scope.launch { viewModel.setCustomSkillEnabled(skill.id, it) }
                            }
                        )
                        IconButton(onClick = {
                            scope.launch { viewModel.deleteCustomSkill(skill.id) }
                        }) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFF8B949E))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        skill.instructions.take(200) + if (skill.instructions.length > 200) "…" else "",
                        color = Color(0xFFB0B8C4),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun fieldColors(primary: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = primary,
    unfocusedBorderColor = Color(0xFF2A3344),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = primary,
    focusedLabelColor = primary,
    unfocusedLabelColor = Color(0xFF8B949E)
)
