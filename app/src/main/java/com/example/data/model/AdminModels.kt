package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector

enum class ReactorState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    GUARD_ARMED,
    SECURITY_ALERT,
    ERROR
}

enum class AdminNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard, "System overview & telemetry"),
    API_CONFIG("api_config", "API Configuration", Icons.Default.Key, "Endpoints & authentication credentials"),
    SECURITY("security", "Security Settings", Icons.Default.Security, "PIN, auto-lock & audit history"),
    APPEARANCE("appearance", "Appearance", Icons.Default.ColorLens, "Themes, animations & HUD visuals"),
    PERMISSIONS("permissions", "Permissions", Icons.Default.VerifiedUser, "System access & privileges manager"),
    DIAGNOSTICS("diagnostics", "Diagnostics", Icons.Default.Build, "Hardware telemetry & network latency"),
    BACKUP_RESTORE("backup_restore", "Backup & Restore", Icons.Default.Restore, "Export, import & factory reset"),
    ABOUT("about", "About", Icons.Default.Info, "System specifications & architecture"),
    LOCK_PANEL("lock_panel", "Lock Panel", Icons.Default.Lock, "Immediately secure console")
}

enum class AccentTheme(val title: String, val primaryHex: String) {
    CYAN("Electric Cyan", "#00F0FF"),
    BLUE("Plasma Blue", "#2979FF"),
    PURPLE("Neon Purple", "#B5179E")
}

enum class BackgroundIntensity(val title: String) {
    VOID_BLACK("Void Black (OLED)"),
    DEEP_NAVY("Deep Obsidian / Navy"),
    CYBER_GRID("Tactical Cyber Grid")
}

enum class CardDensity(val title: String) {
    COMPACT("Compact High-Density"),
    SPACIOUS("Spacious Tablet (Default)")
}

enum class InactivityTimeout(val minutes: Int, val label: String) {
    ONE_MINUTE(1, "1 Minute"),
    TWO_MINUTES(2, "2 Minutes"),
    FIVE_MINUTES(5, "5 Minutes"),
    FIFTEEN_MINUTES(15, "15 Minutes"),
    THIRTY_MINUTES(30, "30 Minutes"),
    NEVER(0, "Disabled (Never Lock)")
}

data class SecurityEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,
    val description: String,
    val severity: String = "INFO" // INFO, WARNING, CRITICAL
)

data class ApiConfiguration(
    val apiKey: String = "",
    val endpointUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    val providerName: String = "Gemini / Google AI Gateway",
    val lastTestedTime: Long = 0L,
    val lastStatus: String = "IDLE", // IDLE, SUCCESS, FAILED
    val lastLatencyMs: Long = 0L,
    val lastErrorMessage: String? = null
)

data class AppearancePreferences(
    val accentTheme: AccentTheme = AccentTheme.CYAN,
    val backgroundIntensity: BackgroundIntensity = BackgroundIntensity.DEEP_NAVY,
    val animationsEnabled: Boolean = true,
    val animationSpeedMultiplier: Float = 1.0f,
    val cardDensity: CardDensity = CardDensity.SPACIOUS,
    val showArcReactor: Boolean = true,
    val arcReactorSpeed: Float = 1.0f,
    val showHeaderClock: Boolean = true,
    val compactSidebar: Boolean = false
)

data class SystemOverviewData(
    val dateTimeString: String = "",
    val batteryPercentage: Int = 100,
    val isCharging: Boolean = false,
    val chargingStatusString: String = "Discharging",
    val networkConnected: Boolean = true,
    val networkType: String = "Wi-Fi",
    val deviceName: String = "",
    val appVersion: String = "1.0.0",
    val adminSecurityStatus: String = "SECURED",
    val configStatus: String = "SYNCHRONIZED"
)
