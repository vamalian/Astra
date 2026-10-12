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

    private val _activeAlertMessage = MutableStateFlow<String?>(null)
    val activeAlertMessage: StateFlow<String?> = _activeAlertMessage.asStateFlow()

    private val _activeAlertType = MutableStateFlow<String?>(null)
    val activeAlertType: StateFlow<String?> = _activeAlertType.asStateFlow()

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
                playBeep(ToneGenerator.TONE_PROP_BEEP, 100)
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
        playBeep(ToneGenerator.TONE_PROP_PROMPT, 250)

        // Register sensor if mode includes MOVEMENT
        if (_guardMode.value == GuardMode.MOVEMENT || _guardMode.value == GuardMode.BOTH) {
            accelerometer?.let {
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
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
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
        if (_guardState.value != GuardState.ACTIVE) return
        if (_guardMode.value != GuardMode.TOUCH && _guardMode.value != GuardMode.BOTH) return

        val now = System.currentTimeMillis()
        if (now - lastAlertTimestamp < alertCooldownMs) return
        lastAlertTimestamp = now

        triggerAlert(
            type = "TOUCH BREACH",
            message = "ASTRA GUARD ALERT: Unexpected touch detected."
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || _guardState.value != GuardState.ACTIVE) return
        if (_guardMode.value != GuardMode.MOVEMENT && _guardMode.value != GuardMode.BOTH) return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

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

        // Exponential smoothing for baseline drift
        baselineX = baselineX * 0.95f + x * 0.05f
        baselineY = baselineY * 0.95f + y * 0.05f
        baselineZ = baselineZ * 0.95f + z * 0.05f

        if (deltaAccel > _sensitivity.value.threshold) {
            val now = System.currentTimeMillis()
            if (now - lastAlertTimestamp < alertCooldownMs) return
            lastAlertTimestamp = now

            triggerAlert(
                type = "MOVEMENT DETECTED",
                message = "ASTRA GUARD ALERT: Unexpected device movement detected."
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun triggerTestAlert() {
        triggerAlert(
            type = "TEST ALERT",
            message = "ASTRA GUARD SIMULATION: Verification alarm executed successfully."
        )
    }

    private fun triggerAlert(type: String, message: String) {
        _guardState.value = GuardState.ALERT
        _activeAlertType.value = type
        _activeAlertMessage.value = message

        // Execute sound alarm
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
                if (alarmToneGenerator == null) {
                    alarmToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 95)
                }
                while (_guardState.value == GuardState.ALERT) {
                    alarmToneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 300)
                    delay(450L)
                }
            } catch (_: Exception) {}
        }
    }

    private fun playBeep(toneType: Int, durationMs: Int) {
        if (!_isSoundEnabled.value) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tg.startTone(toneType, durationMs)
        } catch (_: Exception) {}
    }

    private fun vibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.vibrate(CombinedVibration.createParallel(
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 300), -1)
                ))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(longArrayOf(0, 200, 100, 200, 100, 300), -1)
            }
        } catch (_: Exception) {}
    }

    fun destroy() {
        disarmGuard()
        alarmToneGenerator?.release()
        alarmToneGenerator = null
    }
}
