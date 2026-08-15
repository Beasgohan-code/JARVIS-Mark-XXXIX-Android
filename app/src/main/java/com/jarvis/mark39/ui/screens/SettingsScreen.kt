package com.jarvis.mark39.ui.screens
import android.app.Activity
import com.jarvis.mark39.ui.theme.WallpaperId
import com.jarvis.mark39.ui.theme.AppStyleId

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.mark39.ai.GeminiClient
import com.jarvis.mark39.ai.SystemPrompts
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
    var groqKey by remember { mutableStateOf(settings.getGroqApiKey()) }
    var openRouterKey by remember { mutableStateOf(settings.getOpenRouterApiKey()) }
    var selectedModel by remember { mutableStateOf(settings.getGeminiModel()) }
    var modelMenuOpen by remember { mutableStateOf(false) }
    var confirmActions by remember { mutableStateOf(settings.isConfirmBeforeAction()) }
    var saved by remember { mutableStateOf(false) }
    var selectedPromptId by remember { mutableStateOf(settings.getSystemPromptId()) }
    var depthMode by remember { mutableStateOf(settings.getDepthMode()) }
    var customPrompt by remember { mutableStateOf(settings.getCustomSystemPrompt()) }
    var llmProvider by remember { mutableStateOf(settings.getLlmProvider()) }
    var orModel by remember { mutableStateOf(settings.getOpenRouterModel()) }
    var skillWeb by remember { mutableStateOf(settings.isSkillWebSearch()) }
    var skillPhone by remember { mutableStateOf(settings.isSkillPhoneControl()) }
    var skillCoding by remember { mutableStateOf(settings.isSkillCoding()) }
    var skillVision by remember { mutableStateOf(settings.isSkillVision()) }
    var skillAgent by remember { mutableStateOf(settings.isSkillAgent()) }
    var skillSaveTokens by remember { mutableStateOf(settings.isSkillSaveTokens()) }
    var fallbackEnabled by remember { mutableStateOf(settings.isFallbackEnabled()) }
    var groqModel by remember { mutableStateOf(settings.getGroqModel()) }
    var selectedStyle by remember { mutableStateOf(settings.getStyleId()) }
    var lightMode by remember { mutableStateOf(settings.isLightMode()) }
    var selectedWallpaper by remember { mutableStateOf(settings.getWallpaperId()) }
    var appLock by remember { mutableStateOf(settings.isAppLockEnabled()) }
    var appPin by remember { mutableStateOf(settings.getAppPin()) }
    var hideRecents by remember { mutableStateOf(settings.isHideFromRecents()) }
    var incognito by remember { mutableStateOf(settings.isIncognitoMode()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1117))
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
            SectionTitle("API")
            Spacer(Modifier.height(8.dp))
            Text(
                "Get a free key at aistudio.google.com/apikey",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            Text("Model", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8B949E))
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161B22))
                    .clickable { modelMenuOpen = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedModel, color = Color(0xFF00E5FF), modifier = Modifier.weight(1f))
                Text("▼", color = Color.Gray)
                DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
                    GeminiClient.AVAILABLE_MODELS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                selectedModel = m
                                modelMenuOpen = false
                                saved = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = openRouterKey,
                onValueChange = { openRouterKey = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("OpenRouter API Key (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = groqKey,
                onValueChange = { groqKey = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Groq API Key (free tier)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                "console.groq.com — fast free Llama models. Fallback uses all keys you set.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    settings.setGeminiApiKey(geminiKey)
                    settings.setOpenRouterApiKey(openRouterKey)
                    settings.setGroqApiKey(groqKey)
                    settings.setGroqModel(groqModel)
                    settings.setSkillSaveTokens(skillSaveTokens)
                    settings.setFallbackEnabled(fallbackEnabled)
                    settings.setGeminiModel(selectedModel)
                    settings.setSystemPromptId(selectedPromptId)
                    settings.setDepthMode(depthMode)
                    settings.setCustomSystemPrompt(customPrompt)
                    settings.setLlmProvider(llmProvider)
                    settings.setOpenRouterModel(orModel)
                    settings.setSkillWebSearch(skillWeb)
                    settings.setSkillPhoneControl(skillPhone)
                    settings.setSkillCoding(skillCoding)
                    settings.setSkillVision(skillVision)
                    settings.setSkillAgent(skillAgent)
                    settings.setStyleId(selectedStyle)
                    settings.setLightMode(lightMode)
                    settings.setWallpaperId(selectedWallpaper)
                    settings.setAppLockEnabled(appLock)
                    settings.setAppPin(appPin)
                    settings.setHideFromRecents(hideRecents)
                    settings.setIncognitoMode(incognito)
                    viewModel.refreshApiKeyStatus()
                    viewModel.newChatSession()
                    saved = true
                    (context as? Activity)?.recreate()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (saved) "Saved ✓" else "Save") }

            Spacer(Modifier.height(28.dp))
            
            Spacer(Modifier.height(28.dp))
            SectionTitle("LLM provider")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("gemini" to "Gemini", "groq" to "Groq", "openrouter" to "OpenRouter").forEach { (id, label) ->
                    val sel = llmProvider == id
                    Text(
                        label,
                        color = if (sel) Color.Black else Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Color(0xFF00E5FF) else Color(0xFF161B22))
                            .clickable { llmProvider = id; saved = false }
                            .padding(12.dp)
                    )
                }
            }
            if (llmProvider == "groq") {
                Spacer(Modifier.height(8.dp))
                Text("Groq model", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8B949E))
                listOf(
                    "llama-3.3-70b-versatile",
                    "llama-3.1-8b-instant",
                    "llama-3.1-70b-versatile",
                    "gemma2-9b-it",
                    "mixtral-8x7b-32768"
                ).forEach { m ->
                    val sel = groqModel == m
                    Text(
                        m,
                        color = if (sel) Color(0xFF00E5FF) else Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { groqModel = m; saved = false }
                            .padding(10.dp)
                    )
                }
            }
            if (llmProvider == "openrouter") {
                Spacer(Modifier.height(8.dp))
                Text("OpenRouter model", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8B949E))
                Spacer(Modifier.height(6.dp))
                Column {
                    listOf(
                        "anthropic/claude-3.5-sonnet",
                        "anthropic/claude-3-haiku",
                        "openai/gpt-4o-mini",
                        "openai/gpt-4o",
                        "google/gemini-2.0-flash-001",
                        "meta-llama/llama-3.1-70b-instruct",
                        "deepseek/deepseek-chat"
                    ).forEach { m ->
                        val sel = orModel == m
                        Text(
                            m,
                            color = if (sel) Color(0xFF00E5FF) else Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { orModel = m; saved = false }
                                .padding(10.dp)
                        )
                    }
                }
                Text(
                    "Get key: openrouter.ai — Claude, GPT, Llama, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8B949E)
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle("Skills")
            Spacer(Modifier.height(8.dp))
            SkillRow("Web search", skillWeb) { skillWeb = it; saved = false }
            SkillRow("Phone control", skillPhone) { skillPhone = it; saved = false }
            SkillRow("Coding help", skillCoding) { skillCoding = it; saved = false }
            SkillRow("Vision / camera", skillVision) { skillVision = it; saved = false }
            SkillRow("Agent mode (multi-step)", skillAgent) { skillAgent = it; saved = false }
            SkillRow("Save tokens (short replies)", skillSaveTokens) { skillSaveTokens = it; saved = false }
            SkillRow("Auto fallback (Gemini→Groq→OpenRouter)", fallbackEnabled) { fallbackEnabled = it; saved = false }
            Text(
                "Agent mode uses extra tool steps. No full Linux sandbox on phone — actions stay on-device.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E)
            )
            Text(
                "Sandbox boundary: cannot install packages, root the device, or run arbitrary native binaries.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E)
            )

            SectionTitle("Safety")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Confirm before system actions", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = confirmActions, onCheckedChange = {
                    confirmActions = it
                    settings.setConfirmBeforeAction(it)
                })
            }

            Spacer(Modifier.height(28.dp))
            SectionTitle("System access")
            Spacer(Modifier.height(8.dp))
            
            Text(
                "If Accessibility shows “App was denied access”: Settings → Apps → JARVIS → three-dot menu → Allow restricted settings, then enable Accessibility again. (Required on many phones for sideloaded apps.)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFAB40),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            ModernButton("Accessibility (phone control)") {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            Spacer(Modifier.height(8.dp))
            ModernButton(
                when {
                    !viewModel.canDrawOverlays() -> "Grant overlay permission"
                    uiState.overlayActive -> "Stop floating bubble"
                    else -> "Start floating bubble"
                }
            ) {
                if (viewModel.canDrawOverlays()) {
                    if (uiState.overlayActive) viewModel.stopOverlayBubble()
                    else viewModel.startOverlayBubble()
                } else {
                    context.startActivity(viewModel.overlayPermissionIntent())
                }
            }
            if (uiState.screenVisionActive) {
                Spacer(Modifier.height(8.dp))
                ModernButton("Stop screen vision") { viewModel.stopScreenVision() }
            }

            Spacer(Modifier.height(28.dp))
            
            Spacer(Modifier.height(28.dp))
            
            Spacer(Modifier.height(28.dp))
            SectionTitle("Personality / System prompt")
            Spacer(Modifier.height(8.dp))
            Text(
                "How JARVIS thinks and replies",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E)
            )
            Spacer(Modifier.height(8.dp))
            Column {
                SystemPrompts.PRESETS.forEach { preset ->
                    val sel = selectedPromptId == preset.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF161B22))
                            .clickable {
                                selectedPromptId = preset.id
                                saved = false
                            }
                            .padding(12.dp)
                    ) {
                        Text(
                            preset.label,
                            color = if (sel) Color(0xFF00E5FF) else Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            preset.description,
                            color = Color(0xFF8B949E),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                val customSel = selectedPromptId == "custom"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (customSel) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF161B22))
                        .clickable {
                            selectedPromptId = "custom"
                            saved = false
                        }
                        .padding(12.dp)
                ) {
                    Text(
                        "Custom",
                        color = if (customSel) Color(0xFF00E5FF) else Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Write your own system prompt",
                        color = Color(0xFF8B949E),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (selectedPromptId == "custom") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it; saved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom system prompt") },
                    minLines = 4,
                    maxLines = 10,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            
            Spacer(Modifier.height(16.dp))
            Text("Answer depth", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8B949E))
            Spacer(Modifier.height(6.dp))
            Column {
                listOf(
                    "quick" to "Quick — short & fast",
                    "balanced" to "Balanced — Gemini-like",
                    "deep" to "Deep — Claude-like care",
                    "thorough" to "Thorough — Kimi-like detail"
                ).forEach { (id, label) ->
                    val sel = depthMode == id
                    Text(
                        label,
                        color = if (sel) Color(0xFF00E5FF) else Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF161B22))
                            .clickable { depthMode = id; saved = false }
                            .padding(12.dp)
                    )
                }
            }
            SectionTitle("Appearance")
            Spacer(Modifier.height(8.dp))
            Text("Style", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8B949E))
            Spacer(Modifier.height(6.dp))
            Column {
                enumValues<AppStyleId>().toList().chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { style ->
                            val sel = selectedStyle == style.name
                            Text(
                                style.label,
                                color = if (sel) Color.Black else Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) Color(0xFF00E5FF) else Color(0xFF161B22))
                                    .clickable { selectedStyle = style.name; saved = false }
                                    .padding(12.dp)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Wallpaper", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8B949E))
            Spacer(Modifier.height(6.dp))
            Column {
                enumValues<WallpaperId>().toList().chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { w ->
                            val sel = selectedWallpaper == w.name
                            Text(
                                w.label,
                                color = if (sel) Color.Black else Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) Color(0xFF00E5FF) else Color(0xFF161B22))
                                    .clickable { selectedWallpaper = w.name; saved = false }
                                    .padding(12.dp)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Light mode", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = lightMode, onCheckedChange = {
                    lightMode = it
                    saved = false
                })
            }

            
            Spacer(Modifier.height(28.dp))
            SectionTitle("Security")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("App lock (PIN)", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = appLock, onCheckedChange = { appLock = it; saved = false })
            }
            if (appLock) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = appPin,
                    onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) { appPin = it; saved = false } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PIN (4–8 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Hide content in Recents", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = hideRecents, onCheckedChange = { hideRecents = it; saved = false })
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Incognito (do not save chats)", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = incognito, onCheckedChange = { incognito = it; saved = false })
            }
            Text(
                "API keys are stored in EncryptedSharedPreferences. PIN stays on-device only.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E),
                modifier = Modifier.padding(top = 8.dp)
            )

            SectionTitle("Chat")
            Spacer(Modifier.height(8.dp))
            ModernButton("New chat session (clear history)") {
                viewModel.newChatSession()
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = """Voice control (enable Accessibility first):
• go home / go back / recents / lock phone
• open Chrome / open WhatsApp
• click Login / scroll down / swipe left
• read screen / type hello
• call 5551234 / volume up / mute
• search latest news / screenshot

Tip: if chat fails with model error, pick another model above and Save.""",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E)
            )

            Spacer(Modifier.height(32.dp))
            SectionTitle("About")
            Spacer(Modifier.height(8.dp))
            Text(
                "JARVIS Mark XXXIX\nGemini • Voice • Phone control • Vision\nSessions • Themes • App lock",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E)
            )

        }
    }
}

@Composable
private fun SkillRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF00E5FF).copy(alpha = 0.08f))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF00E5FF))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF00E5FF),
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun ModernButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) { Text(label) }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00E5FF),
    unfocusedBorderColor = Color(0xFF30363D),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFF00E5FF),
    unfocusedLabelColor = Color.Gray,
    cursorColor = Color(0xFF00E5FF),
    focusedContainerColor = Color(0xFF161B22),
    unfocusedContainerColor = Color(0xFF161B22)
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun settingsRepository(): SettingsRepository
}
