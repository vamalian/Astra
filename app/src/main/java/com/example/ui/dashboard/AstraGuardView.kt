package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AlertEntity
import com.example.data.model.GuardMode
import com.example.data.model.GuardSensitivity
import com.example.data.model.GuardState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AstraGuardView(
    guardState: GuardState,
    guardMode: GuardMode,
    sensitivity: GuardSensitivity,
    armingCountdown: Int,
    activeAlertMessage: String?,
    alerts: List<AlertEntity>,
    liveMovementDelta: Float = 0f,
    liveAccelX: Float = 0f,
    liveAccelY: Float = 0f,
    liveAccelZ: Float = 9.8f,
    verifiedTouchCount: Int = 0,
    lastVerifiedTouchTime: String? = null,
    lastVerifiedMovementTime: String? = null,
    isMaxLoudAlarmEnabled: Boolean = true,
    onToggleMaxLoudAlarm: (Boolean) -> Unit = {},
    onTouchVerificationTap: () -> Unit = {},
    onStartArming: (mode: GuardMode, seconds: Int) -> Unit,
    onCancelArming: () -> Unit,
    onDisarm: () -> Unit,
    onDismissAlert: () -> Unit,
    onTestAlert: () -> Unit,
    onClearAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showArmDialog by remember { mutableStateOf(false) }
    var selectedArmMode by remember { mutableStateOf(guardMode) }
    var selectedArmDuration by remember { mutableStateOf(5) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("astra_guard_view")
    ) {
        // Active Alert Warning Banner if security alert is triggered
        if (guardState == GuardState.ALERT && activeAlertMessage != null) {
            HudCard(
                borderColor = AmberAlert,
                backgroundColor = Color(0xFF261805),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("security_alert_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert Warning",
                            tint = AmberAlert,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ASTRA GUARD ALERT // ALARM ACTIVE",
                                color = AmberAlert,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeAlertMessage,
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    HudButton(
                        text = "DISMISS",
                        onClick = onDismissAlert,
                        alertStyle = true,
                        testTag = "dismiss_alert_button"
                    )
                }
            }
        }

        // Arming Countdown Overlay if in ARMING state
        if (guardState == GuardState.ARMING) {
            HudCard(
                borderColor = AmberAlert,
                backgroundColor = Color(0xFF1C1A0E),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("arming_countdown_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberAlert.copy(alpha = 0.2f))
                                .border(1.dp, AmberAlert, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$armingCountdown",
                                color = AmberAlert,
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CALIBRATING SENSORS...",
                                color = AmberAlert,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Place tablet in position before countdown ends",
                                color = MetallicSilver,
                                fontSize = 10.sp
                            )
                        }
                    }
                    HudButton(
                        text = "CANCEL",
                        onClick = onCancelArming,
                        testTag = "cancel_arming_button"
                    )
                }
            }
        }

        // Main Guard Status HUD & Arming Card
        HudCard(
            borderColor = if (guardState == GuardState.ACTIVE) EmeraldActive else TitaniumBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Guard Security",
                            tint = if (guardState == GuardState.ACTIVE) EmeraldActive else ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ASTRA GUARD TELEMETRY",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val statusText = when (guardState) {
                        GuardState.OFF -> "GUARD DISARMED"
                        GuardState.ARMING -> "ARMING ($armingCountdown s)"
                        GuardState.ACTIVE -> "GUARD ACTIVE // ARMED"
                        GuardState.ALERT -> "BREACH DETECTED"
                    }
                    val statusColor = when (guardState) {
                        GuardState.OFF -> MetallicSilver
                        GuardState.ARMING -> AmberAlert
                        GuardState.ACTIVE -> EmeraldActive
                        GuardState.ALERT -> RedAlert
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row: Arm/Disarm & Loud Test Alarm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (guardState == GuardState.OFF) {
                        HudButton(
                            text = "ARM GUARD",
                            onClick = { showArmDialog = true },
                            icon = Icons.Default.Security,
                            isPrimary = true,
                            modifier = Modifier.weight(1f),
                            testTag = "arm_guard_button"
                        )
                    } else {
                        HudButton(
                            text = "DISARM GUARD",
                            onClick = onDisarm,
                            icon = Icons.Default.Security,
                            alertStyle = true,
                            modifier = Modifier.weight(1f),
                            testTag = "disarm_guard_button"
                        )
                    }

                    HudButton(
                        text = "TEST LOUD ALARM",
                        onClick = onTestAlert,
                        icon = Icons.Default.NotificationsActive,
                        alertStyle = true,
                        modifier = Modifier.weight(1.2f),
                        testTag = "test_alert_button"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LIVE VERIFICATION MONITOR CARD (Touches & Movements Real-time)
        HudCard(
            borderColor = ElectricCyan.copy(alpha = 0.6f),
            backgroundColor = DarkGraphite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldActive)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE SENSOR & TOUCH VERIFICATION",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Max Volume Boost switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Loud Volume",
                            tint = if (isMaxLoudAlarmEnabled) AmberAlert else MetallicSilver,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MAX LOUD ALARM",
                            color = if (isMaxLoudAlarmEnabled) AmberAlert else MetallicSilver,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isMaxLoudAlarmEnabled,
                            onCheckedChange = onToggleMaxLoudAlarm,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmberAlert,
                                checkedTrackColor = TitaniumSurface
                            ),
                            modifier = Modifier.size(width = 36.dp, height = 24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Movement Verification Gauge
                val movementRatio = (liveMovementDelta / sensitivity.threshold).coerceIn(0f, 1.5f)
                val isThresholdExceeded = liveMovementDelta > sensitivity.threshold
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TitaniumSurface)
                        .border(
                            1.dp,
                            if (isThresholdExceeded) RedAlert else TitaniumBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Movement Sensor",
                                tint = if (isThresholdExceeded) RedAlert else IceBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACCELEROMETER MOTION DELTA",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "Δ %.2f m/s² (Limit: %.1f)", liveMovementDelta, sensitivity.threshold),
                            color = if (isThresholdExceeded) RedAlert else ElectricCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (movementRatio / 1.5f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isThresholdExceeded) RedAlert else if (movementRatio > 0.6f) AmberAlert else ElectricCyan,
                        trackColor = DarkGraphite
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.US, "X: %.1f  Y: %.1f  Z: %.1f", liveAccelX, liveAccelY, liveAccelZ),
                            color = MetallicSilver,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isThresholdExceeded) "THRESHOLD EXCEEDED!" else "STATUS: STABLE",
                            color = if (isThresholdExceeded) RedAlert else EmeraldActive,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Touch Verification Pad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TitaniumSurface)
                        .border(1.dp, IceBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onTouchVerificationTap() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("touch_verification_pad")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Touch Verification",
                                tint = IceBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TOUCH VERIFICATION TEST PAD",
                                    color = PureWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap here or anywhere on screen to test touch reception",
                                    color = MetallicSilver,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "VERIFIED: $verifiedTouchCount TAPS",
                                color = EmeraldActive,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = lastVerifiedTouchTime ?: "--",
                                color = IceBlue,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Alert History Card
        HudCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECURITY AUDIT LOG (${alerts.size})",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (alerts.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAlerts,
                            modifier = Modifier.testTag("clear_alerts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear History",
                                tint = MetallicSilver,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CLEAR LOG",
                                color = MetallicSilver,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = TitaniumBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                if (alerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No security events recorded. System secure.",
                            color = MetallicSilver,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("HH:mm:ss · dd MMM", Locale.getDefault()) }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(alerts, key = { it.id }) { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TitaniumSurface)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (alert.eventType.contains("BREACH") || alert.eventType.contains("MOVEMENT"))
                                                    AmberAlert else IceBlue
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = alert.eventType,
                                            color = PureWhite,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = alert.message,
                                            color = MetallicSilver,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Text(
                                    text = dateFormat.format(Date(alert.timestamp)),
                                    color = IceBlue,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Arming Configuration Dialog
    if (showArmDialog) {
        AlertDialog(
            onDismissRequest = { showArmDialog = false },
            title = {
                Text(
                    text = "ARM ASTRA GUARD",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "SELECT DETECTION MODE",
                        color = MetallicSilver,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GuardMode.entries.forEach { mode ->
                            HudButton(
                                text = mode.name,
                                onClick = { selectedArmMode = mode },
                                isPrimary = selectedArmMode == mode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "ARMING DELAY COUNTDOWN",
                        color = MetallicSilver,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15).forEach { sec ->
                            HudButton(
                                text = "${sec}s",
                                onClick = { selectedArmDuration = sec },
                                isPrimary = selectedArmDuration == sec,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                HudButton(
                    text = "START ARMING",
                    onClick = {
                        showArmDialog = false
                        onStartArming(selectedArmMode, selectedArmDuration)
                    },
                    isPrimary = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showArmDialog = false }) {
                    Text("CANCEL", color = MetallicSilver, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = DarkGraphite
        )
    }
}
