package com.example.data.guard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.db.AlertDao
import com.example.data.db.AlertEntity
import com.example.data.model.GuardMode
import com.example.data.model.GuardSensitivity
import com.example.data.model.GuardState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class AstraGuardManager(
    private val context: Context,
    private val alertDao: AlertDao,
    private val scope: CoroutineScope,
    private val onAlertTriggered: (type: String, message: String) -> Unit,
    private val onAlertDismissed: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _guardState = MutableStateFlow(GuardState.OFF)
    val guardState: StateFlow<GuardState> = _guardState.asStateFlow()

    private val _guardMode = MutableStateFlow(GuardMode.BOTH)
    val guardMode: StateFlow<GuardMode> = _guardMode.asStateFlow()

    private val _sensitivity = MutableStateFlow(GuardSensitivity.MEDIUM)
    val sensitivity: StateFlow<GuardSensitivity> = _sensitivity.asStateFlow()

    private val _armingSeconds = MutableStateFlow(5)
    val armingSeconds: StateFlow<Int> = _armingSeconds.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(0)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(true)
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private val _isMaxLoudAlarmEnabled = MutableStateFlow(true)
    val isMaxLoudAlarmEnabled: StateFlow<Boolean> = _isMaxLoudAlarmEnabled.asStateFlow()

    private val _activeAlertMessage = MutableStateFlow<String?>(null)
    val activeAlertMessage: StateFlow<String?> = _activeAlertMessage.asStateFlow()

    private val _activeAlertType = MutableStateFlow<String?>(null)
    val activeAlertType: StateFlow<String?> = _activeAlertType.asStateFlow()

    // Live verification telemetry
    private val _liveAccelX = MutableStateFlow(0f)
    val liveAccelX: StateFlow<Float> = _liveAccelX.asStateFlow()

    private val _liveAccelY = MutableStateFlow(0f)
    val liveAccelY: StateFlow<Float> = _liveAccelY.asStateFlow()

    private val _liveAccelZ = MutableStateFlow(9.8f)
    val liveAccelZ: StateFlow<Float> = _liveAccelZ.asStateFlow()

    private val _liveMovementDelta = MutableStateFlow(0f)
    val liveMovementDelta: StateFlow<Float> = _liveMovementDelta.asStateFlow()

    private val _verifiedTouchCount = MutableStateFlow(0)
    val verifiedTouchCount: StateFlow<Int> = _verifiedTouchCount.asStateFlow()

    private val _lastVerifiedTouchTime = MutableStateFlow<String?>("Awaiting touch...")
    val lastVerifiedTouchTime: StateFlow<String?> = _lastVerifiedTouchTime.asStateFlow()

    private val _lastVerifiedMovementTime = MutableStateFlow<String?>("Awaiting movement...")
    val lastVerifiedMovementTime: StateFlow<String?> = _lastVerifiedMovementTime.asStateFlow()

    private var countdownJob: Job? = null
    private var alarmToneGenerator: ToneGenerator? = null
    private var toneLoopJob: Job? = null

    // Baseline sensor calibration
    private var baselineX = 0f
    private var baselineY = 0f
    private var baselineZ = 9.8f
    private var hasBaseline = false

    private var lastAlertTimestamp = 0L
    private val alertCooldownMs = 3000L // 3 seconds cooldown between duplicate triggers
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        // Register sensor for continuous telemetry and movement verification
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun setGuardMode(mode: GuardMode) {
        _guardMode.value = mode
    }

    fun setSensitivity(sens: GuardSensitivity) {
        _sensitivity.value = sens
    }

    fun setArmingDuration(seconds: Int) {
        _armingSeconds.value = seconds
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _isVibrationEnabled.value = enabled
    }

    fun setMaxLoudAlarmEnabled(enabled: Boolean) {
        _isMaxLoudAlarmEnabled.value = enabled
    }

    fun startArmingSequence(mode: GuardMode = _guardMode.value, durationSeconds: Int = _armingSeconds.value) {
        if (_guardState.value == GuardState.ACTIVE || _guardState.value == GuardState.ARMING) return

        _guardMode.value = mode
        _armingSeconds.value = durationSeconds
        _guardState.value = GuardState.ARMING
        _countdownRemaining.value = durationSeconds

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (sec in durationSeconds downTo 1) {
                _countdownRemaining.value = sec
                playBeep(ToneGenerator.TONE_PROP_BEEP, 120)
                delay(1000L)
            }
            _countdownRemaining.value = 0
            activateGuard()
        }
    }

    fun cancelArming() {
        if (_guardState.value == GuardState.ARMING) {
            countdownJob?.cancel()
            _countdownRemaining.value = 0
            _guardState.value = GuardState.OFF
        }
    }

    private fun activateGuard() {
        hasBaseline = false
        _guardState.value = GuardState.ACTIVE
        playBeep(ToneGenerator.TONE_PROP_PROMPT, 300)

        // Switch to faster sensor sampling for security responsiveness
        if (_guardMode.value == GuardMode.MOVEMENT || _guardMode.value == GuardMode.BOTH) {
            accelerometer?.let {
                sensorManager.unregisterListener(this)
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun disarmGuard() {
        countdownJob?.cancel()
        toneLoopJob?.cancel()
        _guardState.value = GuardState.OFF
        _activeAlertMessage.value = null
        _activeAlertType.value = null

        // Return to standard UI sensor sampling
        accelerometer?.let {
            sensorManager.unregisterListener(this)
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        onAlertDismissed()
    }

    fun dismissCurrentAlert() {
        toneLoopJob?.cancel()
        if (_guardState.value == GuardState.ALERT) {
            _guardState.value = GuardState.ACTIVE
            _activeAlertMessage.value = null
            _activeAlertType.value = null
            // Re-establish baseline
            hasBaseline = false
            onAlertDismissed()
        }
    }

    fun onScreenTouched() {
        // Record touch verification for live testing monitor
        _verifiedTouchCount.value = _verifiedTouchCount.value + 1
        _lastVerifiedTouchTime.value = "${timeFormatter.format(Date())} (Touch #${_verifiedTouchCount.value})"

        // If Guard is actively armed, trigger security alert
        if (_guardState.value != GuardState.ACTIVE) return
        if (_guardMode.value != GuardMode.TOUCH && _guardMode.value != GuardMode.BOTH) return

        val now = System.currentTimeMillis()
        if (now - lastAlertTimestamp < alertCooldownMs) return
        lastAlertTimestamp = now

        triggerAlert(
            type = "TOUCH BREACH",
            message = "ASTRA GUARD ALERT: Unexpected touch detected on protected screen."
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        _liveAccelX.value = x
        _liveAccelY.value = y
        _liveAccelZ.value = z

        if (!hasBaseline) {
            baselineX = x
            baselineY = y
            baselineZ = z
            hasBaseline = true
            return
        }

        val dx = x - baselineX
        val dy = y - baselineY
        val dz = z - baselineZ
        val deltaAccel = sqrt(dx * dx + dy * dy + dz * dz)

        _liveMovementDelta.value = deltaAccel

        // Exponential smoothing for resting baseline drift
        baselineX = baselineX * 0.95f + x * 0.05f
        baselineY = baselineY * 0.95f + y * 0.05f
        baselineZ = baselineZ * 0.95f + z * 0.05f

        val threshold = _sensitivity.value.threshold
        if (deltaAccel > threshold) {
            _lastVerifiedMovementTime.value = "${timeFormatter.format(Date())} (${String.format(Locale.US, "%.2f", deltaAccel)} m/s²)"

            if (_guardState.value == GuardState.ACTIVE &&
                (_guardMode.value == GuardMode.MOVEMENT || _guardMode.value == GuardMode.BOTH)
            ) {
                val now = System.currentTimeMillis()
                if (now - lastAlertTimestamp < alertCooldownMs) return
                lastAlertTimestamp = now

                triggerAlert(
                    type = "MOVEMENT DETECTED",
                    message = "ASTRA GUARD ALERT: Unexpected device movement detected (${String.format(Locale.US, "%.1f", deltaAccel)} m/s²)."
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun triggerTestAlert() {
        triggerAlert(
            type = "TEST ALARM",
            message = "ASTRA GUARD VERIFICATION: Maximum volume alarm siren activated successfully."
        )
    }

    private fun triggerAlert(type: String, message: String) {
        _guardState.value = GuardState.ALERT
        _activeAlertType.value = type
        _activeAlertMessage.value = message

        // Execute very loud alarm siren
        if (_isSoundEnabled.value) {
            startAlarmSiren()
        }

        // Execute vibration
        if (_isVibrationEnabled.value) {
            vibrateAlert()
        }

        // Save to Database
        scope.launch(Dispatchers.IO) {
            alertDao.insertAlert(
                AlertEntity(
                    eventType = type,
                    detectionMode = _guardMode.value.name,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        onAlertTriggered(type, message)
    }

    private fun startAlarmSiren() {
        toneLoopJob?.cancel()
        toneLoopJob = scope.launch {
            try {
                // Maximize alarm audio stream volume if loud alarm is enabled
                if (_isMaxLoudAlarmEnabled.value) {
                    audioManager?.let { am ->
                        try {
                            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                            am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                        } catch (_: Exception) {}
                    }
                }

                if (alarmToneGenerator == null) {
                    // Maximum volume (100) on STREAM_ALARM
                    alarmToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                }

                var toggle = false
                while (_guardState.value == GuardState.ALERT) {
                    // Alternating dual-frequency emergency tones for maximum piercing alarm
                    val tone = if (toggle) ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK else ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                    alarmToneGenerator?.startTone(tone, 320)
                    toggle = !toggle
                    delay(380L)
                }
            } catch (_: Exception) {}
        }
    }

    private fun playBeep(toneType: Int, durationMs: Int) {
        if (!_isSoundEnabled.value) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tg.startTone(toneType, durationMs)
        } catch (_: Exception) {}
    }

    private fun vibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.vibrate(CombinedVibration.createParallel(
                    VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250, 100, 400), -1)
                ))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(longArrayOf(0, 250, 100, 250, 100, 400), -1)
            }
        } catch (_: Exception) {}
    }

    fun destroy() {
        disarmGuard()
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
        alarmToneGenerator?.release()
        alarmToneGenerator = null
    }
}
