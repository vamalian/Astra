package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.diagnostics.SystemDiagnosticsManager
import com.example.data.diagnostics.SystemDiagnosticsReport
import com.example.data.model.AdminNavDestination
import com.example.data.model.ApiConfiguration
import com.example.data.model.AppearancePreferences
import com.example.data.model.AppScreen
import com.example.data.model.AstraChatMessage
import com.example.data.model.InactivityTimeout
import com.example.data.model.ReactorState
import com.example.data.model.SecurityEvent
import com.example.data.model.SystemOverviewData
import com.example.data.model.WeatherTelemetry
import com.example.data.pref.AdminPreferencesManager
import com.example.data.voice.AstraVoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AstraAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = AdminPreferencesManager(application)
    private val diagnosticsManager = SystemDiagnosticsManager(application)
    private val voiceManager = AstraVoiceManager(application, viewModelScope)

    // Current Screen Root (Defaults to Main ASTRA AI Assistant screen!)
    private val _currentAppScreen = MutableStateFlow(AppScreen.ASSISTANT)
    val currentAppScreen: StateFlow<AppScreen> = _currentAppScreen.asStateFlow()

    // Unlock Status for Admin Panel
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Navigation State inside Admin Panel
    private val _currentDestination = MutableStateFlow(AdminNavDestination.DASHBOARD)
    val currentDestination: StateFlow<AdminNavDestination> = _currentDestination.asStateFlow()

    // Voice & Arc Reactor Telemetry
    val reactorState: StateFlow<ReactorState> = voiceManager.reactorState
    val audioRms: StateFlow<Float> = voiceManager.audioRms
    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking

    // Chat Conversation with ASTRA AI
    private val _chatMessages = MutableStateFlow<List<AstraChatMessage>>(
        listOf(
            AstraChatMessage(
                text = "ASTRA online and standing by, sir. All core subsystems nominal. Voice and neural gateways ready. How may I assist you today?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<AstraChatMessage>> = _chatMessages.asStateFlow()

    // Weather Subsystem Telemetry
    private val _weatherTelemetry = MutableStateFlow(WeatherTelemetry())
    val weatherTelemetry: StateFlow<WeatherTelemetry> = _weatherTelemetry.asStateFlow()

    // Preferences Delegations
    val autoLockEnabled: StateFlow<Boolean> = prefsManager.autoLockEnabled
    val inactivityTimeout: StateFlow<InactivityTimeout> = prefsManager.inactivityTimeout
    val appearancePreferences: StateFlow<AppearancePreferences> = prefsManager.appearancePreferences
    val apiConfig: StateFlow<ApiConfiguration> = prefsManager.apiConfig
    val securityEvents: StateFlow<List<SecurityEvent>> = prefsManager.securityEvents

    // Diagnostics Telemetry
    private val _diagnosticsReport = MutableStateFlow(diagnosticsManager.gatherSystemReport())
    val diagnosticsReport: StateFlow<SystemDiagnosticsReport> = _diagnosticsReport.asStateFlow()

    // Live Date/Time & System Overview
    private val _currentTimeString = MutableStateFlow(formatCurrentTime())
    val currentTimeString: StateFlow<String> = _currentTimeString.asStateFlow()

    // Latency & API Test States
    private val _isTestingLatency = MutableStateFlow(false)
    val isTestingLatency: StateFlow<Boolean> = _isTestingLatency.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    private val _apiTestResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val apiTestResult: StateFlow<Pair<Boolean, String>?> = _apiTestResult.asStateFlow()

    // Inactivity tracking
    private var lastInteractionTimeMillis = System.currentTimeMillis()
    private var inactivityCheckJob: Job? = null

    // Combined System Overview
    val systemOverview: StateFlow<SystemOverviewData> = combine(
        _currentTimeString,
        _diagnosticsReport,
        _isUnlocked
    ) { timeStr, diag, unlocked ->
        SystemOverviewData(
            dateTimeString = timeStr,
            batteryPercentage = diag.batteryLevel,
            isCharging = diag.isCharging,
            chargingStatusString = diag.chargingSource,
            networkConnected = diag.networkConnected,
            networkType = diag.networkType,
            deviceName = "${diag.deviceManufacturer} ${diag.deviceModel}".trim(),
            appVersion = "1.0.0",
            adminSecurityStatus = if (unlocked) "ADMIN AUTHENTICATED" else "CONSOLE LOCKED",
            configStatus = "SYNCHRONIZED"
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SystemOverviewData()
    )

    init {
        startInactivityMonitor()
        startTimeTicker()

        voiceManager.onTranscriptionResult = { recognizedText ->
            submitAssistantQuery(recognizedText)
        }
    }

    private fun formatCurrentTime(): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy // HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun startTimeTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _currentTimeString.value = formatCurrentTime()
            }
        }
    }

    private fun startInactivityMonitor() {
        inactivityCheckJob?.cancel()
        inactivityCheckJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                if (_isUnlocked.value && autoLockEnabled.value) {
                    val timeoutMinutes = inactivityTimeout.value.minutes
                    if (timeoutMinutes > 0) {
                        val idleDurationMillis = System.currentTimeMillis() - lastInteractionTimeMillis
                        val maxAllowedMillis = timeoutMinutes * 60 * 1000L
                        if (idleDurationMillis >= maxAllowedMillis) {
                            lockConsole("Inactivity timeout ($timeoutMinutes min) expired.")
                        }
                    }
                }
            }
        }
    }

    fun onUserInteraction() {
        lastInteractionTimeMillis = System.currentTimeMillis()
    }

    // App Screen Navigation
    fun openAdminPinGate() {
        onUserInteraction()
        voiceManager.stopSpeaking()
        voiceManager.stopListening()
        _currentAppScreen.value = AppScreen.ADMIN_PIN
    }

    fun returnToAssistant() {
        onUserInteraction()
        _currentAppScreen.value = AppScreen.ASSISTANT
    }

    // PIN Authentication
    fun verifyPin(enteredPin: String): Boolean {
        onUserInteraction()
        val success = prefsManager.verifyPin(enteredPin)
        if (success) {
            _isUnlocked.value = true
            _currentAppScreen.value = AppScreen.ADMIN_PANEL
        }
        return success
    }

    fun lockConsole(reason: String = "Manual lock by administrator.") {
        _isUnlocked.value = false
        _currentAppScreen.value = AppScreen.ASSISTANT
        prefsManager.addSecurityEvent(
            type = "CONSOLE_LOCKED",
            description = reason,
            severity = "INFO"
        )
    }

    fun isDefaultPinActive(): Boolean {
        return prefsManager.isDefaultPin()
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        onUserInteraction()
        return prefsManager.changePin(oldPin, newPin)
    }

    fun navigateTo(destination: AdminNavDestination) {
        onUserInteraction()
        if (destination == AdminNavDestination.LOCK_PANEL) {
            lockConsole("Panel locked via navigation drawer.")
        } else {
            _currentDestination.value = destination
        }
    }

    // Voice & AI Interaction
    fun toggleVoiceInput() {
        onUserInteraction()
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun submitAssistantQuery(query: String) {
        if (query.isBlank()) return
        onUserInteraction()

        // Append user query to chat history
        val userMsg = AstraChatMessage(text = query.trim(), isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            val key = apiConfig.value.apiKey
            val answer = voiceManager.queryGemini(query, key)

            val astraMsg = AstraChatMessage(text = answer, isUser = false)
            _chatMessages.value = _chatMessages.value + astraMsg

            // Vocalize through male TextToSpeech
            voiceManager.speak(answer)
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            AstraChatMessage(
                text = "Command history cleared, sir. Subsystems standing by.",
                isUser = false
            )
        )
    }

    // Preferences Actions
    fun setAutoLockEnabled(enabled: Boolean) {
        onUserInteraction()
        prefsManager.setAutoLockEnabled(enabled)
    }

    fun setInactivityTimeout(timeout: InactivityTimeout) {
        onUserInteraction()
        prefsManager.setInactivityTimeout(timeout)
    }

    fun updateAppearance(appearance: AppearancePreferences) {
        onUserInteraction()
        prefsManager.updateAppearance(appearance)
    }

    fun updateApiConfig(config: ApiConfiguration) {
        onUserInteraction()
        prefsManager.updateApiConfig(config)
    }

    fun deleteApiKey() {
        onUserInteraction()
        prefsManager.deleteApiKey()
    }

    fun clearSecurityEvents() {
        onUserInteraction()
        prefsManager.clearSecurityEvents()
    }

    fun exportConfigJson(): String {
        onUserInteraction()
        return prefsManager.exportConfigJson()
    }

    fun importConfigJson(jsonString: String): Result<String> {
        onUserInteraction()
        return prefsManager.importConfigJson(jsonString)
    }

    fun factoryReset() {
        onUserInteraction()
        prefsManager.factoryReset()
        _isUnlocked.value = false
        _currentAppScreen.value = AppScreen.ASSISTANT
        _currentDestination.value = AdminNavDestination.DASHBOARD
    }

    fun refreshDiagnostics() {
        onUserInteraction()
        _diagnosticsReport.value = diagnosticsManager.gatherSystemReport()
    }

    fun runLatencyPingTest() {
        onUserInteraction()
        viewModelScope.launch {
            _isTestingLatency.value = true
            val (success, latency) = diagnosticsManager.runNetworkPingTest()
            _isTestingLatency.value = false
            val current = _diagnosticsReport.value
            _diagnosticsReport.value = current.copy(
                pingLatencyMs = if (success) latency else null,
                pingStatus = if (success) "SUCCESS (${latency}ms)" else "FAILED"
            )
            prefsManager.addSecurityEvent(
                type = "PING_BENCHMARK",
                description = if (success) "Network ping benchmark completed: ${latency}ms latency." else "Network ping benchmark failed.",
                severity = "INFO"
            )
        }
    }

    fun testApiEndpoint(url: String, key: String) {
        onUserInteraction()
        viewModelScope.launch {
            _isTestingApi.value = true
            _apiTestResult.value = null
            val (success, latency, message) = diagnosticsManager.testApiEndpoint(url, key)
            _isTestingApi.value = false
            _apiTestResult.value = success to message

            val updatedConfig = apiConfig.value.copy(
                lastTestedTime = System.currentTimeMillis(),
                lastStatus = if (success) "SUCCESS" else "FAILED",
                lastLatencyMs = latency,
                lastErrorMessage = if (success) null else message
            )
            prefsManager.updateApiConfig(updatedConfig)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
