package com.jarvis.mark39.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fast local voice command patterns before hitting Gemini.
 * Returns tool result string if handled, null to fall through to LLM.
 */
@Singleton
class VoiceCommandRouter @Inject constructor(
    private val tools: ToolRegistry
) {
    suspend fun tryHandle(raw: String): String? {
        val text = raw.trim().lowercase()
        if (text.isBlank()) return null

        // Home / back / recents
        when {
            text in listOf("go home", "home", "homepage") -> return tools.execute("device_home", emptyMap())
            text in listOf("go back", "back", "navigate back") -> return tools.execute("device_back", emptyMap())
            text in listOf("recents", "recent apps", "overview") -> return tools.execute("device_recents", emptyMap())
            text in listOf("notifications", "show notifications", "notification shade") ->
                return tools.execute("device_notifications", emptyMap())
            text in listOf("quick settings", "control center") ->
                return tools.execute("device_quick_settings", emptyMap())
            text in listOf("lock phone", "lock screen", "lock") ->
                return tools.execute("device_lock", emptyMap())
            text in listOf("screenshot", "take screenshot", "capture screen") ->
                return tools.execute("device_screenshot", emptyMap())
            text in listOf("read screen", "what's on screen", "what is on my screen") ->
                return tools.execute("read_screen", emptyMap())
            text in listOf("volume up", "turn up volume", "louder") ->
                return tools.execute("volume", mapOf("action" to "up"))
            text in listOf("volume down", "quieter", "turn down volume") ->
                return tools.execute("volume", mapOf("action" to "down"))
            text in listOf("mute", "silence") ->
                return tools.execute("volume", mapOf("action" to "mute"))
            text in listOf("open camera", "camera") ->
                return tools.execute("camera", emptyMap())
            text in listOf("open contacts", "contacts") ->
                return tools.execute("contacts", emptyMap())
            text in listOf("wifi", "wi-fi", "open wifi") ->
                return tools.execute("wifi_settings", emptyMap())
            text in listOf("bluetooth") ->
                return tools.execute("bluetooth_settings", emptyMap())
        }

        // open <app>
        Regex("""^(?:open|launch|start)\s+(.+)$""").find(text)?.let { m ->
            val name = m.groupValues[1].trim()
            if (name !in listOf("settings", "url", "maps")) {
                return tools.execute("open_app", mapOf("name" to name))
            }
        }

        // call <number or contact-ish>
        Regex("""^(?:call|phone)\s+(.+)$""").find(text)?.let { m ->
            return tools.execute("call", mapOf("number" to m.groupValues[1].trim()))
        }

        // text/sms
        Regex("""^(?:text|sms|message)\s+(\+?[\d\s-]+)(?:\s+(.+))?$""").find(text)?.let { m ->
            return tools.execute("sms", mapOf("number" to m.groupValues[1].trim(), "body" to m.groupValues.getOrElse(2) { "" }))
        }

        // navigate / maps
        Regex("""^(?:navigate to|directions to|maps?)\s+(.+)$""").find(text)?.let { m ->
            return tools.execute("maps", mapOf("query" to m.groupValues[1].trim()))
        }

        // click / tap
        Regex("""^(?:click|tap|press)\s+(.+)$""").find(text)?.let { m ->
            return tools.execute("click_text", mapOf("text" to m.groupValues[1].trim()))
        }

        // scroll / swipe
        Regex("""^scroll\s+(up|down)$""").find(text)?.let { m ->
            return tools.execute("scroll", mapOf("direction" to m.groupValues[1]))
        }
        Regex("""^swipe\s+(up|down|left|right)$""").find(text)?.let { m ->
            return tools.execute("swipe", mapOf("direction" to m.groupValues[1]))
        }

        // type
        Regex("""^(?:type|enter|write)\s+(.+)$""").find(text)?.let { m ->
            return tools.execute("type_text", mapOf("text" to m.groupValues[1].trim()))
        }

        // search
        Regex("""^(?:search|google|look up)\s+(.+)$""").find(text)?.let { m ->
            return tools.execute("web_search", mapOf("query" to m.groupValues[1].trim()))
        }

        // volume N
        Regex("""^volume\s+(\d{1,3})$""").find(text)?.let { m ->
            return tools.execute("volume", mapOf("action" to m.groupValues[1]))
        }

        // alarm
        Regex("""^(?:set alarm|alarm)\s+(?:for\s+)?(\d{1,2})(?::|\s)(\d{2})(?:\s+(.+))?$""").find(text)?.let { m ->
            return tools.execute(
                "alarm",
                mapOf(
                    "hour" to m.groupValues[1],
                    "minute" to m.groupValues[2],
                    "message" to m.groupValues.getOrElse(3) { "JARVIS" }
                )
            )
        }

        return null
    }
}
