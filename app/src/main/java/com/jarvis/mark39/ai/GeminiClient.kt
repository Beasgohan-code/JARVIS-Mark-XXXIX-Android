package com.jarvis.mark39.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.jarvis.mark39.data.repository.MemoryRepository
import com.jarvis.mark39.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor(
    private val settings: SettingsRepository,
    private val memoryRepository: MemoryRepository,
    private val toolRegistry: dagger.Lazy<ToolRegistry>
) {
    private var model: GenerativeModel? = null
    private var chatSession: com.google.ai.client.generativeai.Chat? = null

    /**
     * generativeai SDK: FunctionDeclaration(name, description, parameters: Map<String, Schema>)
     * Schema.str(description) for string params.
     */
    private fun buildFunctionDeclarations(): List<FunctionDeclaration> = listOf(
        FunctionDeclaration(
            name = "web_search",
            description = "Search the web for current information",
            parameters = mapOf("query" to Schema.str("Search query string"))
        ),
        FunctionDeclaration(
            name = "remember",
            description = "Store a fact in long-term memory",
            parameters = mapOf("fact" to Schema.str("Fact or preference to store"))
        ),
        FunctionDeclaration(
            name = "recall",
            description = "Search long-term memory",
            parameters = mapOf("query" to Schema.str("What to search for in memory"))
        ),
        FunctionDeclaration(
            name = "launch_app",
            description = "Launch an Android app by package name",
            parameters = mapOf("package" to Schema.str("Android package name e.g. com.android.chrome"))
        ),
        FunctionDeclaration(
            name = "open_app",
            description = "Open an app by its display name",
            parameters = mapOf("name" to Schema.str("App display name e.g. Chrome"))
        ),
        FunctionDeclaration(
            name = "open_url",
            description = "Open a URL in the browser",
            parameters = mapOf("url" to Schema.str("Full URL"))
        ),
        FunctionDeclaration(
            name = "list_apps",
            description = "List installed launchable apps",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "device_home",
            description = "Press the Home button",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "device_back",
            description = "Press the Back button",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "device_recents",
            description = "Open Recents / overview",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "device_notifications",
            description = "Open the notification shade",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "device_quick_settings",
            description = "Open quick settings",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "device_lock",
            description = "Lock the screen",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "device_screenshot",
            description = "Take a screenshot",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "open_settings",
            description = "Open Android Settings (optional section: wifi, bluetooth, accessibility, apps)",
            parameters = mapOf("section" to Schema.str("Optional settings section name"))
        ),
        FunctionDeclaration(
            name = "click_text",
            description = "Tap on-screen UI that contains the given text",
            parameters = mapOf("text" to Schema.str("Visible text to click"))
        ),
        FunctionDeclaration(
            name = "type_text",
            description = "Type into the focused input field",
            parameters = mapOf("text" to Schema.str("Text to type"))
        ),
        FunctionDeclaration(
            name = "scroll",
            description = "Scroll the screen up or down",
            parameters = mapOf("direction" to Schema.str("up or down"))
        ),
        FunctionDeclaration(
            name = "swipe",
            description = "Swipe gesture",
            parameters = mapOf("direction" to Schema.str("up, down, left, or right"))
        ),
        FunctionDeclaration(
            name = "read_screen",
            description = "Read visible text from the current screen",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "call",
            description = "Call a phone number",
            parameters = mapOf("number" to Schema.str("Phone number"))
        ),
        FunctionDeclaration(
            name = "dial",
            description = "Open the dialer with optional number",
            parameters = mapOf("number" to Schema.str("Optional phone number"))
        ),
        FunctionDeclaration(
            name = "sms",
            description = "Open SMS compose",
            parameters = mapOf(
                "number" to Schema.str("Phone number"),
                "body" to Schema.str("Message body")
            )
        ),
        FunctionDeclaration(
            name = "maps",
            description = "Open maps search",
            parameters = mapOf("query" to Schema.str("Place or address"))
        ),
        FunctionDeclaration(
            name = "contacts",
            description = "Open contacts app",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "camera",
            description = "Open camera app",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "alarm",
            description = "Set an alarm",
            parameters = mapOf(
                "hour" to Schema.str("Hour 0-23"),
                "minute" to Schema.str("Minute 0-59"),
                "message" to Schema.str("Optional alarm label")
            )
        ),
        FunctionDeclaration(
            name = "volume",
            description = "Control volume: up, down, mute, or a number 0-100",
            parameters = mapOf("action" to Schema.str("up, down, mute, or 0-100"))
        ),
        FunctionDeclaration(
            name = "wifi_settings",
            description = "Open Wi-Fi settings",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "bluetooth_settings",
            description = "Open Bluetooth settings",
            parameters = emptyMap()
        ),
        FunctionDeclaration(
            name = "share_text",
            description = "Share text via the system share sheet",
            parameters = mapOf("text" to Schema.str("Text to share"))
        )
    )

    private fun ensureModel(): GenerativeModel {
        val key = settings.getGeminiApiKey()
        require(key.isNotBlank()) { "Gemini API key is missing. Set it in Settings." }

        if (model == null) {
            model = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = key,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 2048
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
                ),
                systemInstruction = content { text(SYSTEM_PROMPT) },
                tools = listOf(Tool(functionDeclarations = buildFunctionDeclarations()))
            )
            chatSession = model!!.startChat()
        }
        return model!!
    }

    fun resetChat() {
        model = null
        chatSession = null
    }

    suspend fun sendMessage(userText: String, extraContext: String = "", maxToolRounds: Int = 6): String =
        withContext(Dispatchers.IO) {
            val m = ensureModel()
            val memoryCtx = memoryRepository.getContextForPrompt()
            val fullPrompt = buildString {
                if (memoryCtx.isNotBlank()) append(memoryCtx).append("\n\n")
                if (extraContext.isNotBlank()) append(extraContext).append("\n\n")
                append(userText)
            }

            var response = chatSession?.sendMessage(fullPrompt) ?: m.generateContent(fullPrompt)
            var rounds = 0
            while (rounds < maxToolRounds) {
                val functionCalls = response.functionCalls
                if (functionCalls.isEmpty()) break

                val functionResponses = functionCalls.map { call ->
                    val args = mutableMapOf<String, String>()
                    call.args.forEach { (k, v) -> args[k] = v.toString() }
                    val result = toolRegistry.get().execute(call.name, args)
                    FunctionResponsePart(call.name, JSONObject().put("result", result))
                }

                val responseContent = content {
                    functionResponses.forEach { part(it) }
                }
                response = chatSession?.sendMessage(responseContent)
                    ?: m.generateContent(responseContent)
                rounds++
            }

            response.text?.trim().orEmpty().ifBlank { "Done." }
        }

    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String = "Describe what you see and suggest useful actions."
    ): String = withContext(Dispatchers.IO) {
        val response = ensureModel().generateContent(
            content {
                image(bitmap)
                text(prompt)
            }
        )
        response.text?.trim().orEmpty()
    }

    suspend fun analyzeScreen(bitmap: Bitmap): String = analyzeImage(
        bitmap,
        "Android screen. Describe the UI, extract visible text, and suggest 2-3 actions."
    )

    suspend fun analyzeImages(bitmaps: List<Bitmap>, prompt: String): String =
        withContext(Dispatchers.IO) {
            if (bitmaps.isEmpty()) return@withContext "No images."
            val response = ensureModel().generateContent(
                content {
                    bitmaps.forEach { image(it) }
                    text(prompt)
                }
            )
            response.text?.trim().orEmpty()
        }

    companion object {
        private const val SYSTEM_PROMPT = """
You are JARVIS Mark XXXIX on Android — helpful, concise, slightly witty.
You can control the phone with tools: open apps, Home/Back/Recents, notifications, click text, type, scroll, swipe, read screen, call/SMS/maps, volume, settings, web search, memory.
Prefer tools over guessing. Confirm before destructive actions (call, lock).
When the user gives a command, execute with tools then give a short confirmation.
""".trimIndent()
    }
}
