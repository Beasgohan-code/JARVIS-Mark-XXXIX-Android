package com.jarvis.mark39.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intent-based phone control (no root). Complements AccessibilityService.
 */
@Singleton
class PhoneControlService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audio: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun openDialer(number: String? = null): String {
        val intent = if (number.isNullOrBlank()) {
            Intent(Intent.ACTION_DIAL)
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.filter { it.isDigit() || it == '+' }}"))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return if (number.isNullOrBlank()) "Opened dialer" else "Dialer ready for $number (confirm to call)"
    }

    /** Place call — requires CALL_PHONE permission; falls back to dialer. */
    fun call(number: String): String {
        val cleaned = number.filter { it.isDigit() || it == '+' }
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleaned"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Calling $cleaned"
        } catch (_: SecurityException) {
            openDialer(cleaned)
            "CALL permission missing — opened dialer for $cleaned"
        } catch (e: Exception) {
            "Call failed: ${e.message}"
        }
    }

    fun openSms(number: String? = null, body: String? = null): String {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${number.orEmpty()}")
            if (!body.isNullOrBlank()) putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Opened messages${number?.let { " to $it" } ?: ""}"
    }

    fun openMaps(query: String): String {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Opened maps for \"$query\""
    }

    fun openContacts(): String {
        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Opened contacts"
    }

    fun openCamera(): String {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Opened camera"
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "JARVIS alarm"): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
            putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Alarm UI for %02d:%02d".format(hour, minute)
    }

    fun volumeUp(): String {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        return "Volume up"
    }

    fun volumeDown(): String {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        return "Volume down"
    }

    fun volumeMuteToggle(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
        } else {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        }
        return "Mute toggled"
    }

    fun setVolumePercent(percent: Int): String {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((percent.coerceIn(0, 100) / 100f) * max).toInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return "Volume set to $percent%"
    }

    fun openWifiSettings(): String {
        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Opened Wi‑Fi settings"
    }

    fun openBluetoothSettings(): String {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Opened Bluetooth settings"
    }

    fun openAirplaneSettings(): String {
        context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Opened airplane mode settings"
    }

    fun shareText(text: String): String {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Share sheet opened"
    }

    fun openAppByName(name: String): String {
        val q = name.trim().lowercase()
        if (q.isBlank()) return "Missing app name"

        // Common package aliases
        val aliases = mapOf(
            "chrome" to listOf("com.android.chrome", "com.chrome.beta", "com.chrome.dev"),
            "google chrome" to listOf("com.android.chrome"),
            "youtube" to listOf("com.google.android.youtube"),
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "instagram" to listOf("com.instagram.android"),
            "telegram" to listOf("org.telegram.messenger", "org.telegram.messenger.web"),
            "gmail" to listOf("com.google.android.gm"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "photos" to listOf("com.google.android.apps.photos"),
            "camera" to listOf("com.android.camera", "com.android.camera2", "com.google.android.GoogleCamera"),
            "settings" to listOf("com.android.settings"),
            "play store" to listOf("com.android.vending"),
            "playstore" to listOf("com.android.vending"),
            "spotify" to listOf("com.spotify.music"),
            "twitter" to listOf("com.twitter.android"),
            "x" to listOf("com.twitter.android"),
            "facebook" to listOf("com.facebook.katana"),
            "messenger" to listOf("com.facebook.orca"),
            "snapchat" to listOf("com.snapchat.android"),
            "netflix" to listOf("com.netflix.mediaclient"),
            "files" to listOf("com.google.android.apps.nbu.files", "com.android.documentsui"),
            "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
            "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
            "phone" to listOf("com.google.android.dialer", "com.android.dialer"),
            "messages" to listOf("com.google.android.apps.messaging", "com.android.mms"),
            "calendar" to listOf("com.google.android.calendar")
        )

        val pm = context.packageManager
        aliases[q]?.forEach { pkg ->
            val launch = pm.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launch != null) {
                context.startActivity(launch)
                return "Opened $pkg"
            }
        }

        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        val match = apps.firstOrNull {
            val label = it.loadLabel(pm).toString().lowercase()
            label == q || label.contains(q) || q.contains(label)
        } ?: apps.minByOrNull {
            val label = it.loadLabel(pm).toString().lowercase()
            when {
                label.startsWith(q) -> 0
                label.contains(q) -> 1
                else -> 10
            }
        }?.takeIf {
            val label = it.loadLabel(pm).toString().lowercase()
            label.contains(q) || q.length >= 3 && label.split(" ").any { w -> w.startsWith(q) }
        }

        return if (match != null) {
            val pkg = match.activityInfo.packageName
            val launch = pm.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launch != null) {
                context.startActivity(launch)
                "Opened ${match.loadLabel(pm)} ($pkg)"
            } else "Found $pkg but no launch intent"
        } else "No app matching \"$name\". Try exact name or say list apps."
    }

    /** Write text to app cache and open share sheet so user can save as .txt/.html/.py etc. */
    fun createAndShareFile(fileName: String, content: String, mime: String = "text/plain"): String {
        return try {
            val safe = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "jarvis_file.txt" }
            val file = java.io.File(context.cacheDir, safe)
            file.writeText(content)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, content.take(500))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Save / share $safe").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Created $safe — pick an app to save or share it"
        } catch (e: Exception) {
            // Fallback: share as plain text
            shareText(content)
            "Could not write file (${e.message}); shared as text instead"
        }
    }

    fun openYoutubeSearch(query: String): String {
        val q = query.trim()
        return try {
            val app = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", q)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(app)
            "Searching YouTube for "$q""
        } catch (_: Exception) {
            val url = "https://www.youtube.com/results?search_query=" + Uri.encode(q)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Opened YouTube search for "$q""
        }
    }

    fun openWeather(city: String): String {
        val q = city.trim().ifBlank { "weather" }
        val url = "https://www.google.com/search?q=" + Uri.encode("weather $q")
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Opening weather for $q"
    }

}
