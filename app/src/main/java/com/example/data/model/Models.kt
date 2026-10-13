package com.example.data.model

import android.graphics.drawable.Drawable

enum class ReactorState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    GUARD_ARMED,
    SECURITY_ALERT,
    ERROR
}

enum class GuardState {
    OFF,
    ARMING,
    ACTIVE,
    ALERT
}

enum class GuardMode {
    TOUCH,
    MOVEMENT,
    BOTH
}

enum class GuardSensitivity(val displayName: String, val threshold: Float) {
    LOW("LOW", 4.5f),
    MEDIUM("MEDIUM", 2.6f),
    HIGH("HIGH", 1.3f),
    ULTRA("ULTRA", 0.5f)
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "ASTRA"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null
)

data class SystemStatus(
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val isOnline: Boolean = true,
    val connectionType: String = "Wi-Fi",
    val weatherCondition: String = "STANDBY",
    val weatherTemp: String = "--"
)
