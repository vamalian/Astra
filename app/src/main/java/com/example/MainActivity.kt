package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.AstraViewModel
import com.example.ui.dashboard.AstraMainDashboard
import com.example.ui.dashboard.SettingsDialog
import com.example.ui.theme.AstraTheme
import com.example.ui.theme.ObsidianBlack

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AstraViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AstraViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            AstraTheme {
                AstraAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.actionMasked == MotionEvent.ACTION_DOWN) {
            viewModel.onScreenTouched(ev.x, ev.y)
        }
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
fun AstraAppRoot(
    viewModel: AstraViewModel
) {
    val reactorState by viewModel.reactorState.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()

    // Voice states
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val availableVoices by viewModel.voiceManager.availableVoices.collectAsState()
    val selectedVoiceName by viewModel.voiceManager.selectedVoiceName.collectAsState()
    val speechRate by viewModel.voiceManager.speechRate.collectAsState()
    val speechPitch by viewModel.voiceManager.speechPitch.collectAsState()
    val spokenResponsesEnabled by viewModel.voiceManager.spokenResponsesEnabled.collectAsState()

    // Guard states
    val guardState by viewModel.guardManager.guardState.collectAsState()
    val guardMode by viewModel.guardManager.guardMode.collectAsState()
    val guardSensitivity by viewModel.guardManager.sensitivity.collectAsState()
    val armingCountdown by viewModel.guardManager.countdownRemaining.collectAsState()
    val isSoundEnabled by viewModel.guardManager.isSoundEnabled.collectAsState()
    val isVibrationEnabled by viewModel.guardManager.isVibrationEnabled.collectAsState()
    val activeAlertMessage by viewModel.guardManager.activeAlertMessage.collectAsState()

    // Live Telemetry Verification states
    val liveMovementDelta by viewModel.liveMovementDelta.collectAsState()
    val liveAccelX by viewModel.liveAccelX.collectAsState()
    val liveAccelY by viewModel.liveAccelY.collectAsState()
    val liveAccelZ by viewModel.liveAccelZ.collectAsState()
    val verifiedTouchCount by viewModel.verifiedTouchCount.collectAsState()
    val lastVerifiedTouchTime by viewModel.lastVerifiedTouchTime.collectAsState()
    val lastVerifiedMovementTime by viewModel.lastVerifiedMovementTime.collectAsState()
    val isMaxLoudAlarmEnabled by viewModel.isMaxLoudAlarmEnabled.collectAsState()

    // Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val onMicClick: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            viewModel.getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBlack
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            AstraMainDashboard(
                reactorState = reactorState,
                audioRms = audioRms,
                systemStatus = systemStatus,
                guardState = guardState,
                guardMode = guardMode,
                guardSensitivity = guardSensitivity,
                armingCountdown = armingCountdown,
                activeAlertMessage = activeAlertMessage,
                alerts = alerts,
                messages = messages,
                installedApps = installedApps,
                isListening = isListening,
                isSpeaking = reactorState == com.example.data.model.ReactorState.SPEAKING,
                liveMovementDelta = liveMovementDelta,
                liveAccelX = liveAccelX,
                liveAccelY = liveAccelY,
                liveAccelZ = liveAccelZ,
                verifiedTouchCount = verifiedTouchCount,
                lastVerifiedTouchTime = lastVerifiedTouchTime,
                lastVerifiedMovementTime = lastVerifiedMovementTime,
                isMaxLoudAlarmEnabled = isMaxLoudAlarmEnabled,
                onToggleMaxLoudAlarm = { enabled -> viewModel.setMaxLoudAlarmEnabled(enabled) },
                onSimulateMovement = { viewModel.simulateMovementSpike() },
                onChangeSensitivity = { sens -> viewModel.guardManager.setSensitivity(sens) },
                onSendMessage = { prompt -> viewModel.sendMessage(prompt) },
                onStartListening = onMicClick,
                onStopListening = { viewModel.stopListening() },
                onStopSpeaking = { viewModel.stopSpeaking() },
                onReplaySpeech = { text -> viewModel.replaySpeech(text) },
                onClearChat = { viewModel.clearChat() },
                onStartArming = { mode, sec -> viewModel.startArming(mode, sec) },
                onCancelArming = { viewModel.cancelArming() },
                onDisarm = { viewModel.disarmGuard() },
                onDismissAlert = { viewModel.dismissAlert() },
                onTestAlert = { viewModel.testAlert() },
                onClearAlerts = { viewModel.clearAlerts() },
                onLaunchApp = { pkg -> viewModel.launchApp(pkg) },
                onOpenDialer = { num -> viewModel.openDialer(num) },
                onOpenMessaging = { num, body -> viewModel.openMessaging(num, body) },
                onOpenWifi = { viewModel.openWifi() },
                onOpenBluetooth = { viewModel.openBluetooth() },
                onOpenDisplay = { viewModel.openDisplay() },
                onOpenSound = { viewModel.openSound() },
                onOpenAppSettings = { viewModel.openAppSettings() },
                onOpenSettings = { viewModel.openSettings() },
                onScreenTouched = { x, y -> viewModel.onScreenTouched(x, y) }
            )

            if (isSettingsOpen) {
                SettingsDialog(
                    initialApiKey = viewModel.geminiService.getEffectiveApiKey(),
                    onSaveApiKey = { key -> viewModel.geminiService.saveCustomApiKey(key) },
                    onClearApiKey = { viewModel.geminiService.clearCustomApiKey() },
                    onTestApiKey = { viewModel.geminiService.testConnection() },
                    availableVoices = availableVoices,
                    currentVoiceName = selectedVoiceName,
                    onSelectVoice = { voiceName -> viewModel.voiceManager.setVoice(voiceName) },
                    speechRate = speechRate,
                    onSpeechRateChange = { rate -> viewModel.voiceManager.setSpeechRate(rate) },
                    speechPitch = speechPitch,
                    onSpeechPitchChange = { pitch -> viewModel.voiceManager.setSpeechPitch(pitch) },
                    spokenResponsesEnabled = spokenResponsesEnabled,
                    onSpokenResponsesChange = { enabled -> viewModel.voiceManager.setSpokenResponsesEnabled(enabled) },
                    onTestVoice = {
                        viewModel.voiceManager.speak("ASTRA vocal synthesis online and operating at maximum fidelity.")
                    },
                    guardSensitivity = guardSensitivity,
                    onGuardSensitivityChange = { sens -> viewModel.guardManager.setSensitivity(sens) },
                    isSoundEnabled = isSoundEnabled,
                    onSoundEnabledChange = { snd -> viewModel.guardManager.setSoundEnabled(snd) },
                    isVibrationEnabled = isVibrationEnabled,
                    onVibrationEnabledChange = { vib -> viewModel.guardManager.setVibrationEnabled(vib) },
                    onDismissRequest = { viewModel.closeSettings() }
                )
            }
        }
    }
}
