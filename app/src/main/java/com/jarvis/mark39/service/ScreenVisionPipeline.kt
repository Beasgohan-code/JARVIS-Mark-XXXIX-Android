package com.jarvis.mark39.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import androidx.activity.result.ActivityResult
import com.jarvis.mark39.ai.GeminiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaProjection → capture frame → Gemini vision analysis pipeline.
 */
@Singleton
class ScreenVisionPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemini: GeminiClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _lastAnalysis = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val lastAnalysis: SharedFlow<String> = _lastAnalysis.asSharedFlow()

    private var collectJob: Job? = null

    fun createCaptureIntent(): Intent {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mpm.createScreenCaptureIntent()
    }

    fun startFromActivityResult(result: ActivityResult) {
        if (result.resultCode != android.app.Activity.RESULT_OK || result.data == null) return
        val intent = Intent(context, ScreenCaptureService::class.java).apply {
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
            putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
        }
        context.startForegroundService(intent)
        _isActive.value = true
        // Frames are emitted by the service via a static bridge for simplicity
        ScreenCaptureService.onFrame = { bitmap ->
            scope.launch(Dispatchers.IO) {
                try {
                    val analysis = gemini.analyzeScreen(bitmap)
                    _lastAnalysis.emit(analysis)
                } catch (e: Exception) {
                    _lastAnalysis.emit("Screen analysis error: ${e.message}")
                }
            }
        }
    }

    fun analyzeOnce(bitmap: Bitmap) {
        scope.launch(Dispatchers.IO) {
            try {
                _lastAnalysis.emit(gemini.analyzeScreen(bitmap))
            } catch (e: Exception) {
                _lastAnalysis.emit("Error: ${e.message}")
            }
        }
    }

    fun stop() {
        context.stopService(Intent(context, ScreenCaptureService::class.java))
        ScreenCaptureService.onFrame = null
        _isActive.value = false
        collectJob?.cancel()
    }
}
