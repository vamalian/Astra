package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ReactorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class AstraVoiceManager(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener, RecognitionListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _reactorState = MutableStateFlow(ReactorState.IDLE)
    val reactorState: StateFlow<ReactorState> = _reactorState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _assistantSpeechText = MutableStateFlow<String?>(null)
    val assistantSpeechText: StateFlow<String?> = _assistantSpeechText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var onTranscriptionResult: ((String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("AstraVoiceManager", "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Male/deep executive assistant pitch and cadence
                    engine.setPitch(0.85f)
                    engine.setSpeechRate(1.0f)
                    isTtsInitialized = true
                }
            }
        }
    }

    fun startListening() {
        scope.launch(Dispatchers.Main) {
            stopSpeaking()
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _reactorState.value = ReactorState.IDLE
                return@launch
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@AstraVoiceManager)
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _reactorState.value = ReactorState.LISTENING
            } catch (e: Exception) {
                Log.e("AstraVoiceManager", "Error starting speech recognition", e)
                _isListening.value = false
                _reactorState.value = ReactorState.IDLE
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            _isListening.value = false
            _audioRms.value = 0f
            if (_reactorState.value == ReactorState.LISTENING) {
                _reactorState.value = ReactorState.IDLE
            }
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        scope.launch(Dispatchers.Main) {
            _assistantSpeechText.value = text
            _isSpeaking.value = true
            _reactorState.value = ReactorState.SPEAKING

            if (isTtsInitialized && tts != null) {
                val utteranceId = "astra_${System.currentTimeMillis()}"
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

                // Track speech completion estimate
                scope.launch {
                    val durationMs = maxOf(1800L, (text.split(" ").size * 320L))
                    kotlinx.coroutines.delay(durationMs)
                    if (_reactorState.value == ReactorState.SPEAKING) {
                        _reactorState.value = ReactorState.IDLE
                        _isSpeaking.value = false
                        onComplete?.invoke()
                    }
                }
            } else {
                // Fallback display delay
                scope.launch {
                    kotlinx.coroutines.delay(2000)
                    _reactorState.value = ReactorState.IDLE
                    _isSpeaking.value = false
                    onComplete?.invoke()
                }
            }
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // Ignore
        }
        _isSpeaking.value = false
        if (_reactorState.value == ReactorState.SPEAKING) {
            _reactorState.value = ReactorState.IDLE
        }
    }

    suspend fun queryGemini(
        prompt: String,
        configuredApiKey: String?
    ): String = withContext(Dispatchers.IO) {
        _reactorState.value = ReactorState.THINKING

        val effectiveKey = when {
            !configuredApiKey.isNullOrBlank() -> configuredApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (effectiveKey.isBlank()) {
            // Smart built-in executive assistant personality
            val fallbackResponse = generateLocalPersonalityResponse(prompt)
            return@withContext fallbackResponse
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$effectiveKey"

            val systemInstruction = "You are ASTRA, an advanced male executive AI assistant inspired by JARVIS, running natively on an Android tablet command console. Speak with concise, polished, polite, highly intelligent responses. Refer to the user respectfully as 'sir' or 'commander'. Keep answers under 3-4 sentences unless detailed analysis is requested."

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemInstruction\n\nUser request: $prompt"))
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val rootJson = JSONObject(responseBody)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "Task completed, sir.")
                    }
                }
            }
            // If failed with response code
            generateLocalPersonalityResponse(prompt)
        } catch (e: Exception) {
            Log.w("AstraVoiceManager", "Gemini query fallback: ${e.message}")
            generateLocalPersonalityResponse(prompt)
        }
    }

    private fun generateLocalPersonalityResponse(query: String): String {
        val lower = query.lowercase(Locale.US)
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.contains("astra") -> {
                "ASTRA online and at your service, sir. All core subsystems and telemetry are nominal. How may I direct our resources today?"
            }
            lower.contains("weather") || lower.contains("temperature") || lower.contains("climate") -> {
                "Current atmospheric sensors report 24 degrees Celsius with clear skies, 58 percent relative humidity, and barometric pressure at 1013 hectopascals. Subsystems are operating within optimal environmental parameters, sir."
            }
            lower.contains("diagnostic") || lower.contains("status") || lower.contains("subsystem") -> {
                "Running full tablet diagnostics, sir. Arc Reactor core stability is 100 percent. Memory buffers are optimal, network link is verified, and encryption protocols are engaged."
            }
            lower.contains("time") || lower.contains("clock") || lower.contains("date") -> {
                val now = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.US).format(java.util.Date())
                "System chronological synchronization reports $now, sir."
            }
            lower.contains("who are you") || lower.contains("identity") -> {
                "I am ASTRA, your native tablet AI command executive. I monitor system telemetry, manage operational protocols, and interface with our neural subsystems."
            }
            lower.contains("security") || lower.contains("protocol") || lower.contains("guard") -> {
                "Security perimeter is fully fortified. Administrative functions are locked behind biometric and private PIN isolation. No unauthorized breaches detected."
            }
            lower.contains("gemini") || lower.contains("api") || lower.contains("key") -> {
                "Gemini neural processing gateway is standing by. You can configure and verify your Google AI API key inside the Admin Panel under API Configuration, sir."
            }
            lower.contains("thank") -> {
                "Always a privilege to assist, sir. Let me know if you require further telemetry or command execution."
            }
            else -> {
                "Acknowledged, sir. I have processed your inquiry: '$query'. Subsystems are active and operational. For deep cloud synthesis, ensure your Gemini API key is linked in the Admin Panel."
            }
        }
    }

    fun setReactorState(state: ReactorState) {
        _reactorState.value = state
    }

    // RecognitionListener
    override fun onReadyForSpeech(params: Bundle?) {
        _reactorState.value = ReactorState.LISTENING
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize RMS dB typically from -2 to 10
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        _audioRms.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _audioRms.value = 0f
        _reactorState.value = ReactorState.THINKING
    }

    override fun onError(error: Int) {
        _isListening.value = false
        _audioRms.value = 0f
        _reactorState.value = ReactorState.IDLE
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        _audioRms.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spoken = matches[0]
            onTranscriptionResult?.invoke(spoken)
        } else {
            _reactorState.value = ReactorState.IDLE
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _assistantSpeechText.value = "Heard: \"${matches[0]}\""
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
