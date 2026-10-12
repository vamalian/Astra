package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstalledApp
import com.example.ui.components.HudButton
import com.example.ui.components.HudCard
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TitaniumBorder
import com.example.ui.theme.TitaniumSurface

@Composable
fun DeviceControlsView(
    installedApps: List<InstalledApp>,
    onLaunchApp: (packageName: String) -> Unit,
    onOpenDialer: (number: String) -> Unit,
    onOpenMessaging: (number: String, message: String) -> Unit,
    onOpenWifi: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialerDialog by remember { mutableStateOf(false) }
    var dialNumber by remember { mutableStateOf("") }

    var showMessageDialog by remember { mutableStateOf(false) }
    var msgNumber by remember { mutableStateOf("") }
    var msgBody by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_controls_view")
    ) {
        // Quick Action Shortcuts Card
        HudCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SYSTEM INTERACTION MATRIX",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HudButton(
                        text = "CALL",
                        onClick = { showDialerDialog = true },
                        icon = Icons.Default.Call,
                        modifier = Modifier.weight(1f),
                        testTag = "open_dialer_action"
                    )
                    HudButton(
                        text = "MESSAGE",
                        onClick = { showMessageDialog = true },
                        icon = Icons.AutoMirrored.Filled.Message,
                        modifier = Modifier.weight(1f),
                        testTag = "open_message_action"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // System Settings Shortcuts
                Text(
                    text = "HARDWARE & OS SETTINGS SHORTCUTS",
                    color = MetallicSilver,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SettingShortcutChip(
                        icon = Icons.Default.Wifi,
                        label = "Wi-Fi",
                        onClick = onOpenWifi,
                        modifier = Modifier.weight(1f)
                    )
                    SettingShortcutChip(
                        icon = Icons.Default.Bluetooth,
                        label = "Bluetooth",
                        onClick = onOpenBluetooth,
                        modifier = Modifier.weight(1f)
                    )
                    SettingShortcutChip(
                        icon = Icons.Default.BrightnessMedium,
                        label = "Display",
                        onClick = onOpenDisplay,
                        modifier = Modifier.weight(1f)
                    )
                    SettingShortcutChip(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = "Audio",
                        onClick = onOpenSound,
                        modifier = Modifier.weight(1f)
                    )
                    SettingShortcutChip(
                        icon = Icons.Default.Settings,
                        label = "Apps",
                        onClick = onOpenAppSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Application Launcher Grid Card
        HudCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Installed Applications",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TABLET APP LAUNCHER (${installedApps.size})",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(
                    color = TitaniumBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (installedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading installed applications...",
                            color = MetallicSilver,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(installedApps, key = { it.packageName }) { app ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TitaniumSurface)
                                    .border(1.dp, TitaniumBorder, RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = ElectricCyan),
                                        onClick = { onLaunchApp(app.packageName) }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                                    .testTag("app_item_${app.packageName}")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = app.name,
                                        tint = IceBlue,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = app.name,
                                        color = PureWhite,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Call Dialer Dialog
    if (showDialerDialog) {
        AlertDialog(
            onDismissRequest = { showDialerDialog = false },
            title = {
                Text(
                    text = "INITIATE TELEPHONY DIAL",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter destination telephone number. This will open the system dialer for review before calling.",
                        color = MetallicSilver,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = dialNumber,
                        onValueChange = { dialNumber = it },
                        label = { Text("Phone Number", color = MetallicSilver) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = TitaniumBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                HudButton(
                    text = "OPEN DIALER",
                    onClick = {
                        if (dialNumber.isNotBlank()) {
                            onOpenDialer(dialNumber)
                            showDialerDialog = false
                        }
                    },
                    isPrimary = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showDialerDialog = false }) {
                    Text("CANCEL", color = MetallicSilver, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = DarkGraphite
        )
    }

    // Send Message Dialog
    if (showMessageDialog) {
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            title = {
                Text(
                    text = "PREPARE DIRECTIVE MESSAGE",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter recipient and message payload. You will review and send via the messaging interface.",
                        color = MetallicSilver,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = msgNumber,
                        onValueChange = { msgNumber = it },
                        label = { Text("Recipient Number", color = MetallicSilver) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = TitaniumBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = msgBody,
                        onValueChange = { msgBody = it },
                        label = { Text("Message Payload", color = MetallicSilver) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = TitaniumBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                HudButton(
                    text = "OPEN MESSAGING",
                    onClick = {
                        if (msgNumber.isNotBlank()) {
                            onOpenMessaging(msgNumber, msgBody)
                            showMessageDialog = false
                        }
                    },
                    isPrimary = true
                )
            },
            dismissButton = {
                TextButton(onClick = { showMessageDialog = false }) {
                    Text("CANCEL", color = MetallicSilver, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = DarkGraphite
        )
    }
}

@Composable
private fun SettingShortcutChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(TitaniumSurface)
            .border(1.dp, TitaniumBorder, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = ElectricCyan),
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = IceBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = PureWhite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
