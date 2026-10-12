package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onSpeechStateChanged: (isSpeaking: Boolean) -> Unit,
    private val onRmsLevelChanged: (rms: Float) -> Unit
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _spokenResponsesEnabled = MutableStateFlow(true)
    val spokenResponsesEnabled: StateFlow<Boolean> = _spokenResponsesEnabled.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            _isTtsReady.value = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                tts.language = Locale.US
                tts.setSpeechRate(_speechRate.value)
                tts.setPitch(_speechPitch.value)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeechStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeechStateChanged(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeechStateChanged(false)
                    }
                })

                val voices = try {
                    tts.voices?.filter { !it.isNetworkConnectionRequired }?.toList()
                        ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
                _availableVoices.value = voices
                _selectedVoiceName.value = tts.voice?.name
                _isTtsReady.value = true
            }
        } else {
            _isTtsReady.value = false
        }
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        textToSpeech?.setSpeechRate(rate)
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        textToSpeech?.setPitch(pitch)
    }

    fun setVoice(voiceName: String) {
        textToSpeech?.let { tts ->
            val targetVoice = _availableVoices.value.firstOrNull { it.name == voiceName }
            if (targetVoice != null) {
                tts.voice = targetVoice
                _selectedVoiceName.value = targetVoice.name
            }
        }
    }

    fun setSpokenResponsesEnabled(enabled: Boolean) {
        _spokenResponsesEnabled.value = enabled
        if (!enabled) {
            stopSpeaking()
        }
    }

    fun speak(text: String, utteranceId: String = "ASTRA_REPLY_${System.currentTimeMillis()}") {
        if (!_spokenResponsesEnabled.value || textToSpeech == null || !_isTtsReady.value) return
        stopSpeaking()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        onSpeechStateChanged(false)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service is unavailable on this device.")
            return
        }

        stopSpeaking()

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            _isListening.value = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            onError("Failed to initiate voice capture: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
        onRmsLevelChanged(0f)
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _isListening.value = false
        onRmsLevelChanged(0f)
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                if (_isListening.value) {
                    onRmsLevelChanged(rmsdB)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                onRmsLevelChanged(0f)
            }

            override fun onError(error: Int) {
                _isListening.value = false
                onRmsLevelChanged(0f)
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (no audio detected)"
                    else -> "Speech recognition error code: $error"
                }
                // Avoid noisy alerts for benign timeouts/no matches
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    onError(message)
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                onRmsLevelChanged(0f)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim()
                if (!spokenText.isNullOrEmpty()) {
                    onSpeechRecognized(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun destroy() {
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null

        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
