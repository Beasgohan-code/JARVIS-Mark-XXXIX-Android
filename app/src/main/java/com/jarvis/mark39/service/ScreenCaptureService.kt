package com.jarvis.mark39.service

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.mark39.JarvisApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Captures screen frames via MediaProjection and invokes [onFrame].
 */
class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val _frameFlow = MutableSharedFlow<Bitmap>(extraBufferCapacity = 1)
    val frameFlow = _frameFlow.asSharedFlow()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)

        if (resultCode == Activity.RESULT_OK && data != null) {
            startCapture(resultCode, data)
        }
        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val targetW = 720
        val targetH = (height * targetW / width.toFloat()).toInt().coerceAtLeast(1)

        imageReader = ImageReader.newInstance(targetW, targetH, PixelFormat.RGBA_8888, 2)

        handlerThread = HandlerThread("ScreenCapture").also { it.start() }
        handler = Handler(handlerThread!!.looper)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "JARVIS-Screen",
            targetW, targetH, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )

        // Capture every 5s for vision pipeline (cost control)
        handler?.post(object : Runnable {
            override fun run() {
                captureOnce()
                handler?.postDelayed(this, 5000)
            }
        })
    }

    private fun captureOnce() {
        val image = imageReader?.acquireLatestImage() ?: return
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            _frameFlow.tryEmit(cropped)
            onFrame?.invoke(cropped)
        } finally {
            image.close()
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, JarvisApplication.CHANNEL_ID)
            .setContentTitle("JARVIS Screen Capture")
            .setContentText("Screen vision analysis active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        handlerThread?.quitSafely()
        onFrame = null
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 3902
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        /** Bridge for ScreenVisionPipeline (simple static callback). */
        @Volatile
        var onFrame: ((Bitmap) -> Unit)? = null
    }
}
