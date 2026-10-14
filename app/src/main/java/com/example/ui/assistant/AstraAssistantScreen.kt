package com.example.ui.assistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AstraChatMessage
import com.example.data.model.ReactorState
import com.example.ui.AstraAdminViewModel
import com.example.ui.components.AdminGlassCard
import com.example.ui.components.AdminStatusBadge
import com.example.ui.components.ArcReactor
import com.example.ui.components.HudCard
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TitaniumBorder
import com.example.ui.theme.TitaniumSurface

@Composable
fun AstraAssistantScreen(
    viewModel: AstraAdminViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalNexusPalette.current

    val reactorState by viewModel.reactorState.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val systemOverview by viewModel.systemOverview.collectAsState()
    val weather by viewModel.weatherTelemetry.collectAsState()
    val apiConfig by viewModel.apiConfig.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Permission launcher for microphone
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleVoiceInput()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("astra_assistant_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Status Bar
            AstraAssistantTopBar(
                dateTimeString = systemOverview.dateTimeString,
                batteryPercentage = systemOverview.batteryPercentage,
                isCharging = systemOverview.isCharging,
                isNetworkConnected = systemOverview.networkConnected,
                weatherTemp = "${weather.temperatureCelsius}°C",
                weatherCondition = weather.condition,
                isGeminiConfigured = apiConfig.apiKey.isNotBlank(),
                onOpenAdmin = { viewModel.openAdminPinGate() }
            )

            // Content Area (Adaptive Landscape / Portrait)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val isWide = maxWidth >= 760.dp

                if (isWide) {
                    // Tablet Landscape Split Pane
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Arc Reactor & Subsystem Status (42% width)
                        Column(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Animated Arc Reactor
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                ArcReactor(
                                    state = reactorState,
                                    audioRms = audioRms,
                                    reactorSize = 250.dp,
                                    customStatusText = when (reactorState) {
                                        ReactorState.LISTENING -> "AUDIO CAPTURE // LISTENING..."
                                        ReactorState.THINKING -> "NEURAL CORE // GEMINI PROCESSING..."
                                        ReactorState.SPEAKING -> "VOCAL SYNTHESIS // ACTIVE"
                                        ReactorState.SECURITY_ALERT -> "SECURITY ALERT // BREACH"
                                        else -> "ASTRA ONLINE // SYSTEM READY"
                                    }
                                )
                            }

                            // Weather & Atmosphere Gauge Card
                            AdminGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentPadding = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "ATMOSPHERIC SENSORS",
                                                color = MetallicSilver,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${weather.temperatureCelsius}°C  //  ${weather.condition.uppercase()}",
                                                color = PureWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "HUMIDITY ${weather.humidityPercent}%",
                                            color = MetallicSilver,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${weather.pressureHpa} hPa // ${weather.atmosphericQuality}",
                                            color = ElectricCyan,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Subsystem Diagnostics Badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AdminStatusBadge(
                                    text = if (apiConfig.apiKey.isNotBlank()) "GEMINI: READY" else "GEMINI: STANDBY",
                                    color = if (apiConfig.apiKey.isNotBlank()) EmeraldActive else palette.primary
                                )
                                AdminStatusBadge(
                                    text = "VOICE: MALE JARVIS",
                                    color = EmeraldActive
                                )
                                AdminStatusBadge(
                                    text = "CORE: STABLE",
                                    color = palette.primary
                                )
                            }
                        }

                        // Right Pane: Holographic Command Feed & Controls (58% width)
                        Column(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        ) {
                            // Holographic Chat Feed
                            AdminGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = 12.dp
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "COMMUNICATIONS & COMMAND FEED",
                                            color = palette.primary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )

                                        Row {
                                            if (isSpeaking) {
                                                IconButton(
                                                    onClick = { viewModel.stopSpeaking() },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                                        contentDescription = "Stop Speech",
                                                        tint = ElectricCyan,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.clearChatHistory() },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteSweep,
                                                    contentDescription = "Clear Feed",
                                                    tint = MetallicSilver,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(chatMessages, key = { it.id }) { msg ->
                                            AstraChatMessageItem(message = msg)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Action Command Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickChip(
                                    label = "Status Report",
                                    onClick = { viewModel.submitAssistantQuery("Give me a full system status report, ASTRA.") }
                                )
                                QuickChip(
                                    label = "Weather Details",
                                    onClick = { viewModel.submitAssistantQuery("What is the current atmospheric weather telemetry?") }
                                )
                                QuickChip(
                                    label = "Diagnostics",
                                    onClick = { viewModel.submitAssistantQuery("Run system diagnostics and verify core buffers.") }
                                )
                                QuickChip(
                                    label = "Security Protocol",
                                    onClick = { viewModel.submitAssistantQuery("Report current security protocols and perimeter status.") }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Input Bar
                            AstraInputBar(
                                text = textInput,
                                onTextChanged = { textInput = it },
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        viewModel.submitAssistantQuery(textInput)
                                        textInput = ""
                                    }
                                },
                                isListening = isListening,
                                onMicClick = {
                                    if (hasMicPermission) {
                                        viewModel.toggleVoiceInput()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Portrait Layout
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Compact Arc Reactor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ArcReactor(
                                state = reactorState,
                                audioRms = audioRms,
                                reactorSize = 170.dp,
                                customStatusText = when (reactorState) {
                                    ReactorState.LISTENING -> "AUDIO INPUT ACTIVE"
                                    ReactorState.THINKING -> "QUANTUM LOGIC // THINKING"
                                    ReactorState.SPEAKING -> "VOCALIZING"
                                    else -> "ASTRA ONLINE"
                                }
                            )
                        }

                        // Holographic Chat Feed
                        AdminGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = 10.dp
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chatMessages, key = { it.id }) { msg ->
                                    AstraChatMessageItem(message = msg)
                                }
                            }
                        }

                        // Bottom Input Bar
                        AstraInputBar(
                            text = textInput,
                            onTextChanged = { textInput = it },
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.submitAssistantQuery(textInput)
                                    textInput = ""
                                }
                            },
                            isListening = isListening,
                            onMicClick = {
                                if (hasMicPermission) {
                                    viewModel.toggleVoiceInput()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AstraAssistantTopBar(
    dateTimeString: String,
    batteryPercentage: Int,
    isCharging: Boolean,
    isNetworkConnected: Boolean,
    weatherTemp: String,
    weatherCondition: String,
    isGeminiConfigured: Boolean,
    onOpenAdmin: () -> Unit
) {
    val palette = LocalNexusPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkGraphite.copy(alpha = 0.95f))
            .border(
                BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
                RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Tagline
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette.primary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ASTRA AI",
                    color = PureWhite,
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "JARVIS EXECUTIVE TELEMETRY CONSOLE",
                    color = palette.primary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Live Clock & Weather
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$weatherTemp $weatherCondition",
                    color = MetallicSilver,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = dateTimeString,
                color = PureWhite,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        // Battery, Network & Admin Shield Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Network Status
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = if (isNetworkConnected) "Network Connected" else "Offline",
                tint = if (isNetworkConnected) ElectricCyan else Color(0xFFFF5252),
                modifier = Modifier.size(18.dp)
            )

            // Battery Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = if (isCharging) ElectricCyan else PureWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$batteryPercentage%",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Admin Shield Button (Glows & opens PIN Gate)
            AdminShieldButton(onClick = onOpenAdmin)
        }
    }
}

@Composable
private fun AdminShieldButton(
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        palette.primary.copy(alpha = 0.2f),
                        palette.surfaceElevated
                    )
                )
            )
            .border(
                BorderStroke(1.dp, palette.primary.copy(alpha = 0.8f)),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("admin_shield_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin Console",
                tint = palette.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ADMIN CONSOLE",
                color = PureWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.75.sp
            )
        }
    }
}

@Composable
private fun AstraChatMessageItem(message: AstraChatMessage) {
    val palette = LocalNexusPalette.current
    val isUser = message.isUser

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) {
        palette.primary.copy(alpha = 0.15f)
    } else {
        DarkGraphite.copy(alpha = 0.85f)
    }
    val borderColor = if (isUser) palette.primary.copy(alpha = 0.5f) else TitaniumBorder.copy(alpha = 0.7f)
    val senderLabel = if (isUser) "OPERATOR // COMMAND" else "ASTRA AI // RESPONSE"
    val labelColor = if (isUser) IceBlue else palette.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = senderLabel,
                    color = labelColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TitaniumSurface.copy(alpha = 0.6f))
            .border(BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = palette.primaryLight,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AstraInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    onMicClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Voice Mic Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .scale(if (isListening) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    if (isListening) {
                        Brush.radialGradient(listOf(Color(0xFFFF3366), DarkGraphite))
                    } else {
                        Brush.radialGradient(listOf(palette.primary.copy(alpha = 0.4f), DarkGraphite))
                    }
                )
                .border(
                    BorderStroke(
                        2.dp,
                        if (isListening) Color(0xFFFF3366) else palette.primary
                    ),
                    CircleShape
                )
                .clickable(onClick = onMicClick)
                .testTag("voice_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Listening" else "Start Voice Input",
                tint = if (isListening) Color.White else palette.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Text Input Field
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .weight(1f)
                .testTag("assistant_query_input"),
            placeholder = {
                Text(
                    text = if (isListening) "Listening to your voice..." else "Enter tablet command or ask ASTRA...",
                    color = MetallicSilver.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkGraphite,
                unfocusedContainerColor = DarkGraphite.copy(alpha = 0.85f),
                focusedBorderColor = palette.primary,
                unfocusedBorderColor = TitaniumBorder,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite
            )
        )

        // Send Button
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (text.isNotBlank()) palette.primary else TitaniumSurface)
                .border(BorderStroke(1.dp, palette.primary), RoundedCornerShape(8.dp))
                .clickable(enabled = text.isNotBlank(), onClick = onSend)
                .testTag("send_command_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Command",
                tint = if (text.isNotBlank()) Color.Black else MetallicSilver,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
