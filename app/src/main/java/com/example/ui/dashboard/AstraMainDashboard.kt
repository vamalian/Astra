package com.example.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AlertEntity
import com.example.data.model.ChatMessage
import com.example.data.model.GuardMode
import com.example.data.model.GuardSensitivity
import com.example.data.model.GuardState
import com.example.data.model.InstalledApp
import com.example.data.model.ReactorState
import com.example.data.model.SystemStatus
import com.example.ui.components.ArcReactor
import com.example.ui.components.HudButton
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedAlert
import com.example.ui.theme.TitaniumBorder
import com.example.ui.theme.TitaniumSurface

@Composable
fun AstraMainDashboard(
    reactorState: ReactorState,
    audioRms: Float,
    systemStatus: SystemStatus,
    guardState: GuardState,
    guardMode: GuardMode,
    guardSensitivity: GuardSensitivity,
    armingCountdown: Int,
    activeAlertMessage: String?,
    alerts: List<AlertEntity>,
    messages: List<ChatMessage>,
    installedApps: List<InstalledApp>,
    isListening: Boolean,
    isSpeaking: Boolean,
    onSendMessage: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onReplaySpeech: (String) -> Unit,
    onClearChat: () -> Unit,
    onStartArming: (mode: GuardMode, seconds: Int) -> Unit,
    onCancelArming: () -> Unit,
    onDisarm: () -> Unit,
    onDismissAlert: () -> Unit,
    onTestAlert: () -> Unit,
    onClearAlerts: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenDialer: (String) -> Unit,
    onOpenMessaging: (String, String) -> Unit,
    onOpenWifi: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onScreenTouched: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .pointerInput(guardState) {
                if (guardState == GuardState.ACTIVE) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press) {
                                onScreenTouched()
                            }
                        }
                    }
                }
            }
            .testTag("astra_dashboard_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Top Status Bar
            TopStatusBar(
                systemStatus = systemStatus,
                guardState = guardState,
                onSettingsClick = onOpenSettings
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Body Area (Responsive Landscape or Portrait)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLandscape) {
                    // Landscape Tablet Layout
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Left Column: ARC Reactor HUD (42-48% width)
                        Column(
                            modifier = Modifier
                                .weight(0.44f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkGraphite.copy(alpha = 0.85f))
                                .border(1.dp, TitaniumBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ArcReactor(
                                state = reactorState,
                                audioRms = audioRms,
                                reactorSize = 250.dp,
                                customStatusText = if (guardState == GuardState.ALERT)
                                    "ASTRA GUARD // BREACH ALERT" else null
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Reactor Quick Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HudButton(
                                    text = if (isListening) "STOP VOICE" else "VOICE MIC",
                                    onClick = {
                                        if (isListening) onStopListening() else onStartListening()
                                    },
                                    icon = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    alertStyle = isListening,
                                    isPrimary = !isListening,
                                    modifier = Modifier.weight(1f),
                                    testTag = "reactor_voice_mic_button"
                                )

                                if (isSpeaking) {
                                    HudButton(
                                        text = "STOP SPEECH",
                                        onClick = onStopSpeaking,
                                        icon = Icons.Default.StopCircle,
                                        alertStyle = true,
                                        modifier = Modifier.weight(1f),
                                        testTag = "reactor_stop_speech_button"
                                    )
                                }
                            }
                        }

                        // Right Column: Side Control Center with Tabs
                        Column(
                            modifier = Modifier
                                .weight(0.56f)
                                .fillMaxHeight()
                        ) {
                            // Tab Selector Bar
                            DashboardTabBar(
                                activeTab = activeTab,
                                onTabSelected = { activeTab = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Tab Content View
                            Box(modifier = Modifier.weight(1f)) {
                                when (activeTab) {
                                    0 -> AIAssistantView(
                                        messages = messages,
                                        reactorState = reactorState,
                                        isListening = isListening,
                                        onSendMessage = onSendMessage,
                                        onStartVoiceInput = onStartListening,
                                        onStopVoiceInput = onStopListening,
                                        onReplaySpeech = onReplaySpeech,
                                        onClearChat = onClearChat
                                    )
                                    1 -> AstraGuardView(
                                        guardState = guardState,
                                        guardMode = guardMode,
                                        sensitivity = guardSensitivity,
                                        armingCountdown = armingCountdown,
                                        activeAlertMessage = activeAlertMessage,
                                        alerts = alerts,
                                        onStartArming = onStartArming,
                                        onCancelArming = onCancelArming,
                                        onDisarm = onDisarm,
                                        onDismissAlert = onDismissAlert,
                                        onTestAlert = onTestAlert,
                                        onClearAlerts = onClearAlerts
                                    )
                                    2 -> DeviceControlsView(
                                        installedApps = installedApps,
                                        onLaunchApp = onLaunchApp,
                                        onOpenDialer = onOpenDialer,
                                        onOpenMessaging = onOpenMessaging,
                                        onOpenWifi = onOpenWifi,
                                        onOpenBluetooth = onOpenBluetooth,
                                        onOpenDisplay = onOpenDisplay,
                                        onOpenSound = onOpenSound,
                                        onOpenAppSettings = onOpenAppSettings
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Mode (Responsive Phone & Vertical Tablet)
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Compact Top Reactor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkGraphite.copy(alpha = 0.85f))
                                .border(1.dp, TitaniumBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ArcReactor(
                                state = reactorState,
                                audioRms = audioRms,
                                reactorSize = 180.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tab Selector Bar
                        DashboardTabBar(
                            activeTab = activeTab,
                            onTabSelected = { activeTab = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tab Content View
                        Box(modifier = Modifier.weight(1f)) {
                            when (activeTab) {
                                0 -> AIAssistantView(
                                    messages = messages,
                                    reactorState = reactorState,
                                    isListening = isListening,
                                    onSendMessage = onSendMessage,
                                    onStartVoiceInput = onStartListening,
                                    onStopVoiceInput = onStopListening,
                                    onReplaySpeech = onReplaySpeech,
                                    onClearChat = onClearChat
                                )
                                1 -> AstraGuardView(
                                    guardState = guardState,
                                    guardMode = guardMode,
                                    sensitivity = guardSensitivity,
                                    armingCountdown = armingCountdown,
                                    activeAlertMessage = activeAlertMessage,
                                    alerts = alerts,
                                    onStartArming = onStartArming,
                                    onCancelArming = onCancelArming,
                                    onDisarm = onDisarm,
                                    onDismissAlert = onDismissAlert,
                                    onTestAlert = onTestAlert,
                                    onClearAlerts = onClearAlerts
                                )
                                2 -> DeviceControlsView(
                                    installedApps = installedApps,
                                    onLaunchApp = onLaunchApp,
                                    onOpenDialer = onOpenDialer,
                                    onOpenMessaging = onOpenMessaging,
                                    onOpenWifi = onOpenWifi,
                                    onOpenBluetooth = onOpenBluetooth,
                                    onOpenDisplay = onOpenDisplay,
                                    onOpenSound = onOpenSound,
                                    onOpenAppSettings = onOpenAppSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTabBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkGraphite)
            .border(1.dp, TitaniumBorder, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TabPill(
            label = "NEURAL AI",
            icon = Icons.Default.Psychology,
            isSelected = activeTab == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f),
            testTag = "tab_ai"
        )
        TabPill(
            label = "ASTRA GUARD",
            icon = Icons.Default.Security,
            isSelected = activeTab == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f),
            testTag = "tab_guard"
        )
        TabPill(
            label = "DEVICE MATRIX",
            icon = Icons.Default.Widgets,
            isSelected = activeTab == 2,
            onClick = { onTabSelected(2) },
            modifier = Modifier.weight(1f),
            testTag = "tab_device"
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "tab_pill"
) {
    val bg = if (isSelected) TitaniumSurface else Color.Transparent
    val border = if (isSelected) ElectricCyan else Color.Transparent
    val contentColor = if (isSelected) ElectricCyan else MetallicSilver

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = ElectricCyan),
                onClick = onClick
            )
            .padding(vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
