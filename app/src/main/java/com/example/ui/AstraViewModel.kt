package com.example.ui

import android.app.Application
import android.speech.tts.Voice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AlertEntity
import com.example.data.db.AstraDatabase
import com.example.data.device.DeviceActionManager
import com.example.data.gemini.GeminiService
import com.example.data.guard.AstraGuardManager
import com.example.data.model.ChatMessage
import com.example.data.model.GuardMode
import com.example.data.model.GuardSensitivity
import com.example.data.model.GuardState
import com.example.data.model.InstalledApp
import com.example.data.model.ReactorState
import com.example.data.model.SystemStatus
import com.example.data.voice.VoiceAssistantManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AstraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AstraDatabase.getDatabase(application)
    private val alertDao = db.alertDao()

    val geminiService = GeminiService(application)
    val deviceActionManager = DeviceActionManager(application)

    private val _reactorState = MutableStateFlow(ReactorState.IDLE)
    val reactorState: StateFlow<ReactorState> = _reactorState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "ASTRA",
                content = "ASTRA neural core initialized. Tactical and intelligence systems online. Standing by for directives."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    val alerts: StateFlow<List<AlertEntity>> = alertDao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Manager
    val voiceManager = VoiceAssistantManager(
        context = application,
        onSpeechRecognized = { text ->
            viewModelScope.launch {
                sendMessage(text)
            }
        },
        onError = { errMsg ->
            viewModelScope.launch {
                addAstraMessage("Speech recognition notice: $errMsg")
                updateReactorState()
            }
        },
        onSpeechStateChanged = { speaking ->
            viewModelScope.launch {
                if (speaking) {
                    _reactorState.value = ReactorState.SPEAKING
                } else {
                    updateReactorState()
                }
            }
        },
        onRmsLevelChanged = { rms ->
            _audioRms.value = rms
        }
    )

    // Guard Manager
    val guardManager = AstraGuardManager(
        context = application,
        alertDao = alertDao,
        scope = viewModelScope,
        onAlertTriggered = { type, message ->
            viewModelScope.launch {
                _reactorState.value = ReactorState.SECURITY_ALERT
                addAstraMessage("SECURITY BREACH DETECTED: $type — $message")
            }
        },
        onAlertDismissed = {
            viewModelScope.launch {
                updateReactorState()
            }
        }
    )

    val liveMovementDelta = guardManager.liveMovementDelta
    val liveAccelX = guardManager.liveAccelX
    val liveAccelY = guardManager.liveAccelY
    val liveAccelZ = guardManager.liveAccelZ
    val isSensorHardwareActive = guardManager.isSensorHardwareActive
    val verifiedMovementCount = guardManager.verifiedMovementCount
    val lastVerifiedMovementTime = guardManager.lastVerifiedMovementTime
    val verifiedTouchCount = guardManager.verifiedTouchCount
    val lastVerifiedTouchTime = guardManager.lastVerifiedTouchTime
    val lastTouchLocation = guardManager.lastTouchLocation
    val isMaxLoudAlarmEnabled = guardManager.isMaxLoudAlarmEnabled

    fun setMaxLoudAlarmEnabled(enabled: Boolean) {
        guardManager.setMaxLoudAlarmEnabled(enabled)
    }

    fun simulateMovementSpike() {
        guardManager.simulateMovementSpike()
    }

    init {
        refreshSystemStatus()
        loadInstalledApps()

        // Periodically refresh telemetry
        viewModelScope.launch {
            while (true) {
                delay(15000L)
                refreshSystemStatus()
            }
        }
    }

    private fun refreshSystemStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _systemStatus.value = deviceActionManager.getSystemStatus()
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = deviceActionManager.getInstalledLaunchableApps()
        }
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    private fun updateReactorState() {
        _reactorState.value = when {
            guardManager.guardState.value == GuardState.ALERT -> ReactorState.SECURITY_ALERT
            guardManager.guardState.value == GuardState.ACTIVE -> ReactorState.GUARD_ARMED
            guardManager.guardState.value == GuardState.ARMING -> ReactorState.GUARD_ARMED
            voiceManager.isListening.value -> ReactorState.LISTENING
            else -> ReactorState.IDLE
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // 1. Add user message
        val updatedList = _messages.value + ChatMessage(sender = "USER", content = userText.trim())
        _messages.value = updatedList

        // 2. Set reactor to THINKING
        _reactorState.value = ReactorState.THINKING

        // 3. Prepare history
        val historyTurns = updatedList.dropLast(1).map { it.sender to it.content }

        viewModelScope.launch {
            val result = geminiService.generateResponse(userText, historyTurns)
            if (result.isSuccess) {
                val reply = result.getOrNull() ?: "No response generated."
                addAstraMessage(reply)
                // Read response aloud if TTS enabled
                voiceManager.speak(reply)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Communication anomaly."
                addAstraMessage("Neural link notice: $errorMsg")
                _reactorState.value = ReactorState.ERROR
                delay(2000L)
                updateReactorState()
            }
        }
    }

    private fun addAstraMessage(text: String) {
        _messages.value = _messages.value + ChatMessage(sender = "ASTRA", content = text)
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                sender = "ASTRA",
                content = "Neural conversation log reset. Systems operational."
            )
        )
    }

    fun startListening() {
        _reactorState.value = ReactorState.LISTENING
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
        updateReactorState()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
        updateReactorState()
    }

    fun replaySpeech(text: String) {
        voiceManager.speak(text)
    }

    // ASTRA GUARD Actions
    fun startArming(mode: GuardMode, durationSeconds: Int) {
        guardManager.startArmingSequence(mode, durationSeconds)
        _reactorState.value = ReactorState.GUARD_ARMED
    }

    fun cancelArming() {
        guardManager.cancelArming()
        updateReactorState()
    }

    fun disarmGuard() {
        guardManager.disarmGuard()
        updateReactorState()
    }

    fun dismissAlert() {
        guardManager.dismissCurrentAlert()
        updateReactorState()
    }

    fun testAlert() {
        guardManager.triggerTestAlert()
    }

    fun clearAlerts() {
        viewModelScope.launch(Dispatchers.IO) {
            alertDao.clearAllAlerts()
        }
    }

    fun onScreenTouched(x: Float = 0f, y: Float = 0f) {
        guardManager.onScreenTouched(x, y)
    }

    // Device Actions
    fun launchApp(packageName: String) {
        val success = deviceActionManager.launchApp(packageName)
        if (!success) {
            addAstraMessage("Unable to launch package: $packageName")
        }
    }

    fun openDialer(phoneNumber: String) {
        deviceActionManager.openDialer(phoneNumber)
    }

    fun openMessaging(phoneNumber: String, message: String) {
        deviceActionManager.openMessaging(phoneNumber, message)
    }

    fun openWifi() { deviceActionManager.openWifiSettings() }
    fun openBluetooth() { deviceActionManager.openBluetoothSettings() }
    fun openDisplay() { deviceActionManager.openDisplaySettings() }
    fun openSound() { deviceActionManager.openSoundSettings() }
    fun openAppSettings() { deviceActionManager.openApplicationSettings() }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
        guardManager.destroy()
    }
}
