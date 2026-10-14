package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AccentTheme
import com.example.data.model.ApiConfiguration
import com.example.data.model.AppearancePreferences
import com.example.data.model.BackgroundIntensity
import com.example.data.model.CardDensity
import com.example.data.model.InactivityTimeout
import com.example.data.model.SecurityEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminPreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("astra_nexus_admin_prefs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val securityEventListType = Types.newParameterizedType(
        List::class.java,
        SecurityEvent::class.java
    )
    private val securityEventAdapter = moshi.adapter<List<SecurityEvent>>(securityEventListType)

    // Admin PIN
    private val _adminPin = MutableStateFlow(getSavedPin())
    val adminPin: StateFlow<String> = _adminPin.asStateFlow()

    // Auto-Lock Settings
    private val _autoLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_LOCK_ENABLED, true))
    val autoLockEnabled: StateFlow<Boolean> = _autoLockEnabled.asStateFlow()

    private val _inactivityTimeout = MutableStateFlow(getSavedTimeout())
    val inactivityTimeout: StateFlow<InactivityTimeout> = _inactivityTimeout.asStateFlow()

    // Appearance Preferences
    private val _appearancePreferences = MutableStateFlow(getSavedAppearance())
    val appearancePreferences: StateFlow<AppearancePreferences> = _appearancePreferences.asStateFlow()

    // API Configuration
    private val _apiConfig = MutableStateFlow(getSavedApiConfig())
    val apiConfig: StateFlow<ApiConfiguration> = _apiConfig.asStateFlow()

    // Security Events Log
    private val _securityEvents = MutableStateFlow(getSavedSecurityEvents())
    val securityEvents: StateFlow<List<SecurityEvent>> = _securityEvents.asStateFlow()

    init {
        // Log setup verification event if empty
        if (_securityEvents.value.isEmpty()) {
            addSecurityEvent(
                type = "SYSTEM_INITIALIZED",
                description = "ASTRA AI Command & Control initialized. System secured.",
                severity = "INFO"
            )
        }
    }

    private fun getSavedPin(): String {
        return prefs.getString(KEY_ADMIN_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun verifyPin(enteredPin: String): Boolean {
        val matches = enteredPin == _adminPin.value
        if (matches) {
            addSecurityEvent(
                type = "AUTH_SUCCESS",
                description = "Admin authenticated successfully via numeric PIN.",
                severity = "INFO"
            )
        } else {
            addSecurityEvent(
                type = "AUTH_FAILED",
                description = "Invalid PIN authentication attempt.",
                severity = "WARNING"
            )
        }
        return matches
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (oldPin != _adminPin.value) {
            addSecurityEvent(
                type = "PIN_CHANGE_FAILED",
                description = "PIN change rejected: Current PIN verification failed.",
                severity = "WARNING"
            )
            return false
        }
        if (newPin.length < 3) {
            return false
        }

        prefs.edit().putString(KEY_ADMIN_PIN, newPin).apply()
        _adminPin.value = newPin
        addSecurityEvent(
            type = "PIN_UPDATED",
            description = "Admin security PIN updated successfully.",
            severity = "CRITICAL"
        )
        return true
    }

    fun isDefaultPin(): Boolean {
        return _adminPin.value == DEFAULT_PIN
    }

    fun setAutoLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOCK_ENABLED, enabled).apply()
        _autoLockEnabled.value = enabled
        addSecurityEvent(
            type = "SECURITY_CONFIG_CHANGED",
            description = "Auto-lock state changed to: ${if (enabled) "ENABLED" else "DISABLED"}",
            severity = "INFO"
        )
    }

    fun setInactivityTimeout(timeout: InactivityTimeout) {
        prefs.edit().putString(KEY_INACTIVITY_TIMEOUT, timeout.name).apply()
        _inactivityTimeout.value = timeout
        addSecurityEvent(
            type = "SECURITY_CONFIG_CHANGED",
            description = "Inactivity timeout configured to: ${timeout.label}",
            severity = "INFO"
        )
    }

    private fun getSavedTimeout(): InactivityTimeout {
        val name = prefs.getString(KEY_INACTIVITY_TIMEOUT, InactivityTimeout.FIVE_MINUTES.name)
        return try {
            InactivityTimeout.valueOf(name ?: InactivityTimeout.FIVE_MINUTES.name)
        } catch (e: Exception) {
            InactivityTimeout.FIVE_MINUTES
        }
    }

    // Appearance
    fun updateAppearance(updated: AppearancePreferences) {
        prefs.edit()
            .putString(KEY_ACCENT_THEME, updated.accentTheme.name)
            .putString(KEY_BACKGROUND_INTENSITY, updated.backgroundIntensity.name)
            .putBoolean(KEY_ANIMATIONS_ENABLED, updated.animationsEnabled)
            .putFloat(KEY_ANIMATION_SPEED, updated.animationSpeedMultiplier)
            .putString(KEY_CARD_DENSITY, updated.cardDensity.name)
            .putBoolean(KEY_SHOW_ARC_REACTOR, updated.showArcReactor)
            .putFloat(KEY_ARC_REACTOR_SPEED, updated.arcReactorSpeed)
            .putBoolean(KEY_SHOW_HEADER_CLOCK, updated.showHeaderClock)
            .putBoolean(KEY_COMPACT_SIDEBAR, updated.compactSidebar)
            .apply()
        _appearancePreferences.value = updated
    }

    private fun getSavedAppearance(): AppearancePreferences {
        val accentStr = prefs.getString(KEY_ACCENT_THEME, AccentTheme.CYAN.name)
        val bgStr = prefs.getString(KEY_BACKGROUND_INTENSITY, BackgroundIntensity.DEEP_NAVY.name)
        val densityStr = prefs.getString(KEY_CARD_DENSITY, CardDensity.SPACIOUS.name)

        val accent = try { AccentTheme.valueOf(accentStr ?: AccentTheme.CYAN.name) } catch (e: Exception) { AccentTheme.CYAN }
        val bg = try { BackgroundIntensity.valueOf(bgStr ?: BackgroundIntensity.DEEP_NAVY.name) } catch (e: Exception) { BackgroundIntensity.DEEP_NAVY }
        val density = try { CardDensity.valueOf(densityStr ?: CardDensity.SPACIOUS.name) } catch (e: Exception) { CardDensity.SPACIOUS }

        return AppearancePreferences(
            accentTheme = accent,
            backgroundIntensity = bg,
            animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS_ENABLED, true),
            animationSpeedMultiplier = prefs.getFloat(KEY_ANIMATION_SPEED, 1.0f),
            cardDensity = density,
            showArcReactor = prefs.getBoolean(KEY_SHOW_ARC_REACTOR, true),
            arcReactorSpeed = prefs.getFloat(KEY_ARC_REACTOR_SPEED, 1.0f),
            showHeaderClock = prefs.getBoolean(KEY_SHOW_HEADER_CLOCK, true),
            compactSidebar = prefs.getBoolean(KEY_COMPACT_SIDEBAR, false)
        )
    }

    // API Config
    fun updateApiConfig(config: ApiConfiguration) {
        prefs.edit()
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_ENDPOINT_URL, config.endpointUrl)
            .putString(KEY_PROVIDER_NAME, config.providerName)
            .putLong(KEY_LAST_TESTED_TIME, config.lastTestedTime)
            .putString(KEY_LAST_STATUS, config.lastStatus)
            .putLong(KEY_LAST_LATENCY, config.lastLatencyMs)
            .putString(KEY_LAST_ERROR, config.lastErrorMessage ?: "")
            .apply()
        _apiConfig.value = config
        addSecurityEvent(
            type = "API_CONFIG_UPDATED",
            description = "API Endpoint credentials modified for ${config.providerName}.",
            severity = "WARNING"
        )
    }

    fun deleteApiKey() {
        val current = _apiConfig.value
        val cleared = current.copy(
            apiKey = "",
            lastStatus = "IDLE",
            lastErrorMessage = null
        )
        updateApiConfig(cleared)
        addSecurityEvent(
            type = "API_KEY_DELETED",
            description = "API key was removed from secure storage.",
            severity = "WARNING"
        )
    }

    private fun getSavedApiConfig(): ApiConfiguration {
        return ApiConfiguration(
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            endpointUrl = prefs.getString(KEY_ENDPOINT_URL, "https://generativelanguage.googleapis.com/v1beta") ?: "https://generativelanguage.googleapis.com/v1beta",
            providerName = prefs.getString(KEY_PROVIDER_NAME, "Gemini / Google AI Gateway") ?: "Gemini / Google AI Gateway",
            lastTestedTime = prefs.getLong(KEY_LAST_TESTED_TIME, 0L),
            lastStatus = prefs.getString(KEY_LAST_STATUS, "IDLE") ?: "IDLE",
            lastLatencyMs = prefs.getLong(KEY_LAST_LATENCY, 0L),
            lastErrorMessage = prefs.getString(KEY_LAST_ERROR, null)?.ifEmpty { null }
        )
    }

    // Security Events
    fun addSecurityEvent(type: String, description: String, severity: String = "INFO") {
        val current = _securityEvents.value.toMutableList()
        current.add(0, SecurityEvent(type = type, description = description, severity = severity))
        // Cap at 50
        val capped = if (current.size > 50) current.take(50) else current
        _securityEvents.value = capped
        saveSecurityEvents(capped)
    }

    fun clearSecurityEvents() {
        val cleared = listOf(
            SecurityEvent(
                type = "AUDIT_LOG_CLEARED",
                description = "Security event log cleared by administrator.",
                severity = "WARNING"
            )
        )
        _securityEvents.value = cleared
        saveSecurityEvents(cleared)
    }

    private fun saveSecurityEvents(events: List<SecurityEvent>) {
        try {
            val json = securityEventAdapter.toJson(events)
            prefs.edit().putString(KEY_SECURITY_EVENTS_JSON, json).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun getSavedSecurityEvents(): List<SecurityEvent> {
        val json = prefs.getString(KEY_SECURITY_EVENTS_JSON, null) ?: return emptyList()
        return try {
            securityEventAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Backup & Restore
    fun exportConfigJson(): String {
        val appearance = _appearancePreferences.value
        val api = _apiConfig.value
        val timeout = _inactivityTimeout.value

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val exportTimestamp = sdf.format(Date())

        return buildString {
            append("{\n")
            append("  \"version\": \"1.0.0\",\n")
            append("  \"export_timestamp\": \"$exportTimestamp\",\n")
            append("  \"app\": \"ASTRA AI\",\n")
            append("  \"auto_lock_enabled\": ${_autoLockEnabled.value},\n")
            append("  \"inactivity_timeout_minutes\": ${timeout.minutes},\n")
            append("  \"appearance\": {\n")
            append("    \"accent_theme\": \"${appearance.accentTheme.name}\",\n")
            append("    \"background_intensity\": \"${appearance.backgroundIntensity.name}\",\n")
            append("    \"animations_enabled\": ${appearance.animationsEnabled},\n")
            append("    \"animation_speed\": ${appearance.animationSpeedMultiplier},\n")
            append("    \"card_density\": \"${appearance.cardDensity.name}\",\n")
            append("    \"show_arc_reactor\": ${appearance.showArcReactor},\n")
            append("    \"arc_reactor_speed\": ${appearance.arcReactorSpeed},\n")
            append("    \"show_header_clock\": ${appearance.showHeaderClock},\n")
            append("    \"compact_sidebar\": ${appearance.compactSidebar}\n")
            append("  },\n")
            append("  \"api_configuration\": {\n")
            append("    \"endpoint_url\": \"${api.endpointUrl}\",\n")
            append("    \"provider_name\": \"${api.providerName}\",\n")
            append("    \"has_api_key\": ${api.apiKey.isNotEmpty()}\n")
            append("  }\n")
            append("}")
        }
    }

    fun importConfigJson(jsonString: String): Result<String> {
        return try {
            if (!jsonString.contains("ASTRA") && !jsonString.contains("auto_lock_enabled")) {
                return Result.failure(IllegalArgumentException("Invalid JSON format for ASTRA AI config."))
            }

            // Quick extraction of key fields
            if (jsonString.contains("\"auto_lock_enabled\": false")) {
                setAutoLockEnabled(false)
            } else if (jsonString.contains("\"auto_lock_enabled\": true")) {
                setAutoLockEnabled(true)
            }

            for (theme in AccentTheme.values()) {
                if (jsonString.contains("\"accent_theme\": \"${theme.name}\"")) {
                    updateAppearance(_appearancePreferences.value.copy(accentTheme = theme))
                    break
                }
            }

            for (bg in BackgroundIntensity.values()) {
                if (jsonString.contains("\"background_intensity\": \"${bg.name}\"")) {
                    updateAppearance(_appearancePreferences.value.copy(backgroundIntensity = bg))
                    break
                }
            }

            for (density in CardDensity.values()) {
                if (jsonString.contains("\"card_density\": \"${density.name}\"")) {
                    updateAppearance(_appearancePreferences.value.copy(cardDensity = density))
                    break
                }
            }

            addSecurityEvent(
                type = "CONFIG_RESTORED",
                description = "Configuration restored successfully from imported JSON.",
                severity = "INFO"
            )
            Result.success("Configuration imported and applied successfully.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun factoryReset(): Boolean {
        prefs.edit().clear().apply()
        _adminPin.value = DEFAULT_PIN
        _autoLockEnabled.value = true
        _inactivityTimeout.value = InactivityTimeout.FIVE_MINUTES
        _appearancePreferences.value = AppearancePreferences()
        _apiConfig.value = ApiConfiguration()
        _securityEvents.value = listOf(
            SecurityEvent(
                type = "FACTORY_RESET",
                description = "All preferences, API credentials, and security configurations reset to factory defaults.",
                severity = "CRITICAL"
            )
        )
        return true
    }

    companion object {
        const val DEFAULT_PIN = "4000"
        private const val KEY_ADMIN_PIN = "key_admin_pin"
        private const val KEY_AUTO_LOCK_ENABLED = "key_auto_lock_enabled"
        private const val KEY_INACTIVITY_TIMEOUT = "key_inactivity_timeout"
        private const val KEY_ACCENT_THEME = "key_accent_theme"
        private const val KEY_BACKGROUND_INTENSITY = "key_background_intensity"
        private const val KEY_ANIMATIONS_ENABLED = "key_animations_enabled"
        private const val KEY_ANIMATION_SPEED = "key_animation_speed"
        private const val KEY_CARD_DENSITY = "key_card_density"
        private const val KEY_SHOW_ARC_REACTOR = "key_show_arc_reactor"
        private const val KEY_ARC_REACTOR_SPEED = "key_arc_reactor_speed"
        private const val KEY_SHOW_HEADER_CLOCK = "key_show_header_clock"
        private const val KEY_COMPACT_SIDEBAR = "key_compact_sidebar"

        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_ENDPOINT_URL = "key_endpoint_url"
        private const val KEY_PROVIDER_NAME = "key_provider_name"
        private const val KEY_LAST_TESTED_TIME = "key_last_tested_time"
        private const val KEY_LAST_STATUS = "key_last_status"
        private const val KEY_LAST_LATENCY = "key_last_latency"
        private const val KEY_LAST_ERROR = "key_last_error"

        private const val KEY_SECURITY_EVENTS_JSON = "key_security_events_json"
    }
}
