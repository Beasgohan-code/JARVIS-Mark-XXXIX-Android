package com.jarvis.mark39.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.jarvis.mark39.JarvisApplication
import com.jarvis.mark39.MainActivity
import com.jarvis.mark39.R
import kotlin.math.abs

/**
 * Floating cyan bubble (SYSTEM_ALERT_WINDOW / overlay).
 * Tap → open JARVIS. Drag to move.
 */
class OverlayBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: ImageView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (bubbleView == null) showBubble()
        return START_STICKY
    }

    private fun showBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            dp(56), dp(56),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 200
        }

        bubbleView = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setBackgroundColor(0xCC00E5FF.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = 12f
            setOnTouchListener(object : android.view.View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var touchX = 0f
                private var touchY = 0f
                private var moved = false

                override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            touchX = event.rawX
                            touchY = event.rawY
                            moved = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - touchX).toInt()
                            val dy = (event.rawY - touchY).toInt()
                            if (abs(dx) > 8 || abs(dy) > 8) moved = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager?.updateViewLayout(bubbleView, params)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!moved) openApp()
                            return true
                        }
                    }
                    return false
                }
            })
        }

        try {
            windowManager?.addView(bubbleView, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, JarvisApplication.CHANNEL_ID)
            .setContentTitle("JARVIS bubble active")
            .setContentText("Tap bubble to open")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        bubbleView?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        bubbleView = null
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 3903
    }
}
