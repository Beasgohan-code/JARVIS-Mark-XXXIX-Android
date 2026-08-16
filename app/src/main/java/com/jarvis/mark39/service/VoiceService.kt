package com.jarvis.mark39.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.jarvis.mark39.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null
    private var onSpeakingFinished: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyVoiceSettings()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        onSpeakingFinished?.invoke()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {}
                })
            }
        }
    }

    fun applyVoiceSettings() {
        val t = tts ?: return
        val lang = settings.getVoiceLocale()
        val locale = when (lang) {
            "hi" -> Locale("hi", "IN")
            "ml" -> Locale("ml", "IN")
            "ta" -> Locale("ta", "IN")
            "te" -> Locale("te", "IN")
            "en-GB" -> Locale.UK
            "en-US" -> Locale.US
            else -> Locale.getDefault()
        }
        t.language = locale
        t.setSpeechRate(settings.getVoiceRate())
        t.setPitch(settings.getVoicePitch())
        // Prefer offline/network voice matching locale if available
        val preferred = settings.getVoiceName()
        if (preferred.isNotBlank()) {
            val match = t.voices?.firstOrNull { it.name == preferred }
            if (match != null) t.voice = match
        }
    }

    fun availableVoices(): List<Pair<String, String>> {
        val t = tts ?: return emptyList()
        return try {
            t.voices?.map { v -> v.name to "${v.locale.displayName} · ${v.name}" }?.sortedBy { it.second }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResult("")
            return
        }
        onResultCallback = onResult
        _partialText.value = ""
        _isListening.value = true

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(listener)
        }

        val lang = settings.getVoiceLocale()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            when (lang) {
                "hi" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                "ml" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ml-IN")
                "ta" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
                "te" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "te-IN")
                "en-GB" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB")
                "en-US" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                else -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            }
        }
        speechRecognizer?.startListening(intent)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { _isListening.value = false }
        override fun onError(error: Int) {
            _isListening.value = false
            onResultCallback?.invoke("")
        }
        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            onResultCallback?.invoke(text)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            _partialText.value = text
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        applyVoiceSettings()
        onSpeakingFinished = onFinished
        val id = UUID.randomUUID().toString()
        // Speak in chunks if very long
        val clean = text.take(3500)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
    }
}
