package com.example.ui.dashboard

import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.model.GuardSensitivity
import com.example.ui.components.HudButton
import com.example.ui.components.HudCard
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TitaniumBorder
import com.example.ui.theme.TitaniumSurface

@Composable
fun SettingsDialog(
    initialApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onTestApiKey: (suspend () -> Result<Long>),
    availableVoices: List<Voice>,
    currentVoiceName: String?,
    onSelectVoice: (String) -> Unit,
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChange: (Float) -> Unit,
    spokenResponsesEnabled: Boolean,
    onSpokenResponsesChange: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    guardSensitivity: GuardSensitivity,
    onGuardSensitivityChange: (GuardSensitivity) -> Unit,
    isSoundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    isVibrationEnabled: Boolean,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(initialApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var testStatusText by remember { mutableStateOf<String?>(null) }
    var testStatusColor by remember { mutableStateOf(MetallicSilver) }
    var isTestingConnection by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkGraphite)
                .border(1.dp, TitaniumBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "System Settings",
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ASTRA CONTROL SYSTEM SETTINGS",
                            color = PureWhite,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MetallicSilver
                        )
                    }
                }

                HorizontalDivider(
                    color = TitaniumBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // SECTION 1: GEMINI AI CONFIGURATION
                Text(
                    text = "1. GEMINI NEURAL INTELLIGENCE CONFIGURATION",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure your Google Gemini API Key. Keys entered here are securely saved in private application storage.",
                    color = MetallicSilver,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field"),
                    label = { Text("Gemini API Key", color = MetallicSilver) },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle API Key Visibility",
                                tint = MetallicSilver
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = TitaniumBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HudButton(
                        text = "SAVE KEY",
                        onClick = {
                            onSaveApiKey(apiKeyText)
                            testStatusText = "Key saved successfully"
                            testStatusColor = EmeraldActive
                        },
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        testTag = "save_key_button"
                    )

                    HudButton(
                        text = "TEST CONNECTION",
                        onClick = {
                            isTestingConnection = true
                            testStatusText = "Pinging Gemini API endpoint..."
                            testStatusColor = IceBlue
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                val result = onTestApiKey()
                                isTestingConnection = false
                                if (result.isSuccess) {
                                    testStatusText = "Connection verified! Latency: ${result.getOrNull()} ms"
                                    testStatusColor = EmeraldActive
                                } else {
                                    testStatusText = "Connection failed: ${result.exceptionOrNull()?.message}"
                                    testStatusColor = RedAlert
                                }
                            }
                        },
                        enabled = !isTestingConnection && apiKeyText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        testTag = "test_key_button"
                    )

                    HudButton(
                        text = "CLEAR",
                        onClick = {
                            apiKeyText = ""
                            onClearApiKey()
                            testStatusText = "Key removed from local storage"
                            testStatusColor = MetallicSilver
                        },
                        modifier = Modifier.weight(0.7f),
                        testTag = "clear_key_button"
                    )
                }

                if (testStatusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = testStatusText ?: "",
                        color = testStatusColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HorizontalDivider(
                    color = TitaniumBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // SECTION 2: VOICE & TTS SETTINGS
                Text(
                    text = "2. NATIVE ANDROID TEXT-TO-SPEECH (TTS)",
                    color = IceBlue,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Spoken Audio Responses",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "ASTRA automatically reads out responses aloud",
                            color = MetallicSilver,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = spokenResponsesEnabled,
                        onCheckedChange = onSpokenResponsesChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricCyan,
                            checkedTrackColor = TitaniumSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Speech Rate Slider
                Text(
                    text = "Speech Rate: ${String.format(java.util.Locale.US, "%.1fx", speechRate)}",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = speechRate,
                    onValueChange = onSpeechRateChange,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = TitaniumSurface
                    )
                )

                // Speech Pitch Slider
                Text(
                    text = "Speech Pitch: ${String.format(java.util.Locale.US, "%.1fx", speechPitch)}",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = speechPitch,
                    onValueChange = onSpeechPitchChange,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = IceBlue,
                        activeTrackColor = IceBlue,
                        inactiveTrackColor = TitaniumSurface
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                HudButton(
                    text = "TEST VOCAL SYNTHESIS",
                    onClick = onTestVoice,
                    icon = Icons.Default.RecordVoiceOver,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "test_voice_button"
                )

                HorizontalDivider(
                    color = TitaniumBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // SECTION 3: ASTRA GUARD CONFIGURATION
                Text(
                    text = "3. ASTRA GUARD SECURITY PARAMETERS",
                    color = AmberAlert,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Sensitivity selector
                Text(
                    text = "Motion Detector Sensitivity: ${guardSensitivity.displayName}",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GuardSensitivity.entries.forEach { sens ->
                        HudButton(
                            text = sens.displayName,
                            onClick = { onGuardSensitivityChange(sens) },
                            isPrimary = guardSensitivity == sens,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Warning Sound Sirens",
                        color = PureWhite,
                        fontSize = 12.sp
                    )
                    Switch(
                        checked = isSoundEnabled,
                        onCheckedChange = onSoundEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmberAlert,
                            checkedTrackColor = TitaniumSurface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Haptic Vibration Warning",
                        color = PureWhite,
                        fontSize = 12.sp
                    )
                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = onVibrationEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AmberAlert,
                            checkedTrackColor = TitaniumSurface
                        )
                    )
                }

                HorizontalDivider(
                    color = TitaniumBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // SECTION 4: GENERAL & PRIVACY
                Text(
                    text = "4. SYSTEM PRIVACY & ABOUT",
                    color = MetallicSilver,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ASTRA AI Version 1.0.0 (Production Release)\n" +
                            "Private local processing: Speech synthesis runs entirely on-device. " +
                            "ASTRA GUARD runs strictly on hardware sensors. No telemetry or alert records are uploaded to external clouds.",
                    color = MetallicSilver,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                HudButton(
                    text = "DONE",
                    onClick = onDismissRequest,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "done_settings_button"
                )
            }
        }
    }
}
