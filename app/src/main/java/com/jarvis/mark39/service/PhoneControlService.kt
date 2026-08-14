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
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(main, 0).firstOrNull {
            it.loadLabel(pm).toString().equals(name, ignoreCase = true) ||
                it.loadLabel(pm).toString().contains(name, ignoreCase = true)
        }
        return if (match != null) {
            val pkg = match.activityInfo.packageName
            val launch = pm.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launch != null) {
                context.startActivity(launch)
                "Opened ${match.loadLabel(pm)} ($pkg)"
            } else "Found $pkg but no launch intent"
        } else "No app matching \"$name\""
    }
}
