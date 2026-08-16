package com.jarvis.mark39.ai

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.jarvis.mark39.data.repository.MemoryRepository
import com.jarvis.mark39.service.JarvisAccessibilityService
import com.jarvis.mark39.service.PhoneControlService
import com.jarvis.mark39.service.WebSearchService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memory: MemoryRepository,
    private val webSearch: WebSearchService,
    private val phone: PhoneControlService
) {
    data class ToolDef(val name: String, val description: String, val params: String)

    val tools: List<ToolDef> = listOf(
        ToolDef("web_search", "Search the web", "query:string"),
        ToolDef("remember", "Store fact in memory", "fact:string"),
        ToolDef("recall", "Search memory", "query:string"),
        ToolDef("launch_app", "Launch app by package name", "package:string"),
        ToolDef("open_app", "Open app by display name", "name:string"),
        ToolDef("open_url", "Open URL in browser", "url:string"),
        ToolDef("list_apps", "List installed apps", ""),
        ToolDef("device_home", "Press Home", ""),
        ToolDef("device_back", "Press Back", ""),
        ToolDef("device_recents", "Open Recents", ""),
        ToolDef("device_notifications", "Open notification shade", ""),
        ToolDef("device_quick_settings", "Open quick settings", ""),
        ToolDef("device_lock", "Lock screen", ""),
        ToolDef("device_screenshot", "Take screenshot", ""),
        ToolDef("open_settings", "Open Settings section", "section?:string"),
        ToolDef("click_text", "Tap on-screen element containing text", "text:string"),
        ToolDef("type_text", "Type into focused field", "text:string"),
        ToolDef("scroll", "Scroll forward or backward", "direction:string"),
        ToolDef("swipe", "Swipe gesture", "direction:string"),
        ToolDef("read_screen", "Read visible text on screen", ""),
        ToolDef("call", "Call a phone number (or open dialer)", "number:string"),
        ToolDef("dial", "Open dialer with optional number", "number?:string"),
        ToolDef("sms", "Open SMS compose", "number?:string,body?:string"),
        ToolDef("maps", "Open maps search", "query:string"),
        ToolDef("contacts", "Open contacts", ""),
        ToolDef("camera", "Open camera app", ""),
        ToolDef("alarm", "Set alarm", "hour:int,minute:int,message?:string"),
        ToolDef("volume", "Control volume: up, down, mute, or set 0-100", "action:string"),
        ToolDef("wifi_settings", "Open Wi‑Fi settings", ""),
        ToolDef("bluetooth_settings", "Open Bluetooth settings", ""),
        ToolDef("share_text", "Share text via system sheet", "text:string"),
        ToolDef("youtube_search", "Search YouTube and open results", "query:string"),
        ToolDef("weather", "Open weather for a city", "city:string"),

        ToolDef("create_file", "Create a text/html/py/json/md file and open share/save sheet", "filename:string,content:string,mime?:string"),
        ToolDef("create_html", "Create an HTML page file and share it", "title?:string,body:string"),
        ToolDef("create_zip_note", "Explain zip creation limits and share files as text bundle", "note:string")
    )

    fun listToolsForPrompt(): String =
        tools.joinToString("\n") { "- ${it.name}(${it.params}): ${it.description}" }

    private fun a11y(): JarvisAccessibilityService? = JarvisAccessibilityService.instance

    suspend fun execute(name: String, params: Map<String, String>): String {
        return try {
            when (name.lowercase()) {
                "web_search" -> {
                    val q = params["query"] ?: params.values.firstOrNull() ?: return "Missing query"
                    val result = webSearch.search(q)
                    if (result.startsWith("No instant answer")) {
                        var url = "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(q, "UTF-8")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        result + " (opened browser)"
                    } else result
                }
                "remember" -> {
                    val fact = params["fact"] ?: params.values.firstOrNull() ?: return "Missing fact"
                    memory.storeFact(fact)
                    "Remembered: $fact"
                }
                "recall" -> {
                    val hits = memory.search(params["query"] ?: "")
                    if (hits.isEmpty()) "No matching memories." else hits.joinToString("\n") { "• ${it.content}" }
                }
                "launch_app" -> {
                    val pkg = params["package"] ?: return "Missing package"
                    a11y()?.launchApp(pkg)?.let { if (it) "Launched $pkg" else "Failed to launch $pkg" }
                        ?: run {
                            val i = context.packageManager.getLaunchIntentForPackage(pkg)
                            if (i != null) {
                                context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Launched $pkg"
                            } else "App not found: $pkg"
                        }
                }
                "open_app" -> phone.openAppByName(params["name"] ?: params.values.firstOrNull() ?: return "Missing name")
                "open_url" -> {
                    var url = params["url"] ?: params.values.firstOrNull() ?: return "Missing url"
                    if (!url.startsWith("http")) url = "https://$url"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    "Opened $url"
                }
                "list_apps" -> listLaunchableApps()
                "device_home" -> a11y()?.performGlobalActionHome()?.let { "Home" } ?: needA11y()
                "device_back" -> a11y()?.performGlobalActionBack()?.let { "Back" } ?: needA11y()
                "device_recents" -> a11y()?.performGlobalActionRecents()?.let { "Recents" } ?: needA11y()
                "device_notifications" -> a11y()?.performGlobalActionNotifications()?.let { "Notifications" } ?: needA11y()
                "device_quick_settings" -> a11y()?.performGlobalActionQuickSettings()?.let { "Quick settings" } ?: needA11y()
                "device_lock" -> a11y()?.performGlobalActionLockScreen()?.let { if (it) "Locked" else "Lock not supported" } ?: needA11y()
                "device_screenshot" -> a11y()?.performGlobalActionTakeScreenshot()?.let { if (it) "Screenshot taken" else "Screenshot failed" } ?: needA11y()
                "open_settings" -> {
                    val section = params["section"]
                    val intent = when (section?.lowercase()) {
                        "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        "wifi", "network" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                        "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        "apps" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                        "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                        "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                        else -> Intent(Settings.ACTION_SETTINGS)
                    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    "Opened Settings${section?.let { " ($it)" } ?: ""}"
                }
                "click_text" -> {
                    val t = params["text"] ?: params.values.firstOrNull() ?: return "Missing text"
                    val ok = a11y()?.clickByText(t) ?: return needA11y()
                    if (ok) "Clicked \"$t\"" else "No on-screen element matching \"$t\""
                }
                "type_text" -> {
                    val t = params["text"] ?: params.values.firstOrNull() ?: return "Missing text"
                    val ok = a11y()?.typeText(t) ?: return needA11y()
                    if (ok) "Typed text" else "No focused input field"
                }
                "scroll" -> {
                    val dir = (params["direction"] ?: "down").lowercase()
                    val ok = when (dir) {
                        "up", "backward" -> a11y()?.scrollBackward()
                        else -> a11y()?.scrollForward()
                    } ?: return needA11y()
                    if (ok) "Scrolled $dir" else "Could not scroll"
                }
                "swipe" -> {
                    val dir = (params["direction"] ?: "up").lowercase()
                    val dm = context.resources.displayMetrics
                    val w = dm.widthPixels.toFloat()
                    val h = dm.heightPixels.toFloat()
                    val ok = when (dir) {
                        "up" -> a11y()?.swipe(w / 2, h * 0.7f, w / 2, h * 0.3f)
                        "down" -> a11y()?.swipe(w / 2, h * 0.3f, w / 2, h * 0.7f)
                        "left" -> a11y()?.swipe(w * 0.8f, h / 2, w * 0.2f, h / 2)
                        "right" -> a11y()?.swipe(w * 0.2f, h / 2, w * 0.8f, h / 2)
                        else -> false
                    } ?: return needA11y()
                    if (ok) "Swiped $dir" else "Swipe failed"
                }
                "read_screen" -> a11y()?.readScreenText() ?: needA11y()
                "call" -> phone.call(params["number"] ?: return "Missing number")
                "dial" -> phone.openDialer(params["number"])
                "sms" -> phone.openSms(params["number"], params["body"])
                "maps" -> phone.openMaps(params["query"] ?: params.values.firstOrNull() ?: return "Missing query")
                "contacts" -> phone.openContacts()
                "camera" -> phone.openCamera()
                "alarm" -> {
                    val hour = params["hour"]?.toIntOrNull() ?: return "Missing hour"
                    val minute = params["minute"]?.toIntOrNull() ?: 0
                    phone.setAlarm(hour, minute, params["message"] ?: "JARVIS alarm")
                }
                "volume" -> {
                    when ((params["action"] ?: params.values.firstOrNull() ?: "").lowercase()) {
                        "up" -> phone.volumeUp()
                        "down" -> phone.volumeDown()
                        "mute" -> phone.volumeMuteToggle()
                        else -> {
                            val pct = params["action"]?.toIntOrNull() ?: params["percent"]?.toIntOrNull()
                            if (pct != null) phone.setVolumePercent(pct) else "Use volume action: up, down, mute, or 0-100"
                        }
                    }
                }
                "wifi_settings" -> phone.openWifiSettings()
                "bluetooth_settings" -> phone.openBluetoothSettings()
                "share_text" -> phone.shareText(params["text"] ?: return "Missing text")
                "youtube_search" -> phone.openYoutubeSearch(params["query"] ?: params.values.firstOrNull() ?: return "Missing query")
                "weather" -> phone.openWeather(params["city"] ?: params["query"] ?: params.values.firstOrNull() ?: "me")
                "create_file" -> {
                    val fn = params["filename"] ?: params["name"] ?: "jarvis.txt"
                    val content = params["content"] ?: params["text"] ?: return "Missing content"
                    val mime = params["mime"] ?: when {
                        fn.endsWith(".html") || fn.endsWith(".htm") -> "text/html"
                        fn.endsWith(".json") -> "application/json"
                        fn.endsWith(".py") -> "text/x-python"
                        fn.endsWith(".md") -> "text/markdown"
                        fn.endsWith(".css") -> "text/css"
                        fn.endsWith(".js") -> "text/javascript"
                        else -> "text/plain"
                    }
                    phone.createAndShareFile(fn, content, mime)
                }
                "create_html" -> {
                    val title = params["title"] ?: "JARVIS Page"
                    val body = params["body"] ?: params["content"] ?: return "Missing body"
                    val html = """
                    <!DOCTYPE html>
                    <html><head><meta charset=\"utf-8\"/><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>
                    <title>$title</title>
                    <style>body{font-family:system-ui;margin:24px;line-height:1.5;background:#0a0a0f;color:#e6edf3}
                    a{color:#00e5ff}</style></head>
                    <body><h1>$title</h1>$body</body></html>
                    """.trimIndent()
                    phone.createAndShareFile("jarvis_page.html", html, "text/html")
                }
                "create_zip_note" -> {
                    val note = params["note"] ?: params.values.firstOrNull() ?: ""
                    phone.shareText(
                        "JARVIS cannot build binary ZIP archives inside the assistant sandbox yet.\n" +
                        "I can create .txt/.html/.py/.json files for you to save.\n$note"
                    )
                    "Shared guidance + note. For multi-file zip, use Files app or a PC."
                }
                else -> "Unknown tool: $name"
            }
        } catch (e: Exception) {
            "Tool error ($name): ${e.message}"
        }
    }

    private fun needA11y() =
        "Accessibility service not enabled. Open Settings → Accessibility → enable JARVIS."

    private fun listLaunchableApps(): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { ri ->
                val label = ri.loadLabel(pm)?.toString() ?: return@mapNotNull null
                "$label → ${ri.activityInfo.packageName}"
            }
            .sorted()
            .take(50)
        return if (apps.isEmpty()) "No apps found" else apps.joinToString("\n")
    }

    fun parseAction(response: String): Pair<String, Map<String, String>>? {
        val regex = Regex("""\[ACTION:(\w+)\|([^\]]+)\]""", RegexOption.IGNORE_CASE)
        val match = regex.find(response) ?: run {
            val simple = Regex("""\[ACTION:(\w+)\]""", RegexOption.IGNORE_CASE).find(response)
            return simple?.let { it.groupValues[1] to emptyMap() }
        }
        val name = match.groupValues[1]
        val raw = match.groupValues[2].trim()
        val params = if (raw.contains("=")) {
            raw.split("|").mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) kv[0].trim() to kv[1].trim() else null
            }.toMap()
        } else {
            val tool = tools.find { it.name.equals(name, true) }
            val key = tool?.params?.substringBefore(":")?.trim()?.trimEnd('?')?.ifBlank { "value" } ?: "value"
            mapOf(key to raw)
        }
        return name to params
    }
}
