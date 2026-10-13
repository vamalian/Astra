package com.example.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InactivityTimeout
import com.example.data.model.SecurityEvent
import com.example.ui.components.AdminActionButton
import com.example.ui.components.AdminGlassCard
import com.example.ui.components.AdminSectionHeader
import com.example.ui.components.AdminStatusBadge
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecuritySettingsScreen(
    autoLockEnabled: Boolean,
    onToggleAutoLock: (Boolean) -> Unit,
    inactivityTimeout: InactivityTimeout,
    onSelectTimeout: (InactivityTimeout) -> Unit,
    onChangePin: (oldPin: String, newPin: String) -> Boolean,
    onLockImmediately: () -> Unit,
    securityEvents: List<SecurityEvent>,
    onClearSecurityEvents: () -> Unit,
    isDefaultPinActive: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    // Change PIN State
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf(false) }

    // Clear confirmation dialog
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("security_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminSectionHeader(
                    title = "Security Settings // Vault",
                    subtitle = "Admin authorization, auto-locking, and audit logging",
                    icon = Icons.Default.Security
                )

                AdminActionButton(
                    text = "Lock Console Now",
                    onClick = onLockImmediately,
                    isDestructive = true,
                    icon = Icons.Default.Lock,
                    testTag = "lock_now_btn"
                )
            }
        }

        // Default PIN Warning Card
        if (isDefaultPinActive) {
            item {
                AdminGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = AmberAlert,
                    contentPadding = 14.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberAlert,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DEFAULT SETUP PIN (600) ACTIVE",
                                color = AmberAlert,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "The initial setup PIN (600) is intended only for first boot. Please change this below to safeguard system settings.",
                                color = MetallicSilver,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Change Admin PIN Card
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CHANGE ADMIN ACCESS PIN",
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { oldPin = it },
                        label = { Text("Current PIN") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_old_pin"),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedLabelColor = palette.primary,
                            unfocusedLabelColor = MetallicSilver
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = { Text("New PIN (min 3 digits)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_new_pin"),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.border,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedLabelColor = palette.primary,
                                unfocusedLabelColor = MetallicSilver
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it },
                            label = { Text("Confirm New PIN") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_confirm_pin"),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.border,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedLabelColor = palette.primary,
                                unfocusedLabelColor = MetallicSilver
                            ),
                            singleLine = true
                        )
                    }

                    AnimatedVisibility(visible = pinMessage != null) {
                        Text(
                            text = pinMessage ?: "",
                            color = if (pinError) RedAlert else EmeraldActive,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AdminActionButton(
                        text = "Update PIN",
                        onClick = {
                            if (newPin != confirmPin) {
                                pinError = true
                                pinMessage = "New PIN and Confirmation PIN do not match."
                            } else if (newPin.length < 3) {
                                pinError = true
                                pinMessage = "PIN must be at least 3 digits."
                            } else {
                                val success = onChangePin(oldPin, newPin)
                                if (success) {
                                    pinError = false
                                    pinMessage = "Admin PIN updated successfully."
                                    oldPin = ""
                                    newPin = ""
                                    confirmPin = ""
                                } else {
                                    pinError = true
                                    pinMessage = "Current PIN verification failed."
                                }
                            }
                        },
                        isPrimary = true,
                        icon = Icons.Default.VpnKey,
                        enabled = oldPin.isNotBlank() && newPin.isNotBlank() && confirmPin.isNotBlank(),
                        testTag = "update_pin_btn"
                    )
                }
            }
        }

        // Automatic Lock & Inactivity Timeout Card
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AUTOMATIC INACTIVITY LOCK",
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lock the admin console if no interaction occurs on the tablet display",
                                color = MetallicSilver,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Switch(
                            checked = autoLockEnabled,
                            onCheckedChange = onToggleAutoLock,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = palette.primary,
                                uncheckedThumbColor = MetallicSilver,
                                uncheckedTrackColor = palette.border
                            )
                        )
                    }

                    if (autoLockEnabled) {
                        Text(
                            text = "INACTIVITY TIMEOUT DURATION",
                            color = palette.primary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            InactivityTimeout.values().forEach { timeout ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onSelectTimeout(timeout) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = inactivityTimeout == timeout,
                                        onClick = { onSelectTimeout(timeout) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = palette.primary,
                                            unselectedColor = palette.border
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = timeout.label,
                                        color = if (inactivityTimeout == timeout) PureWhite else MetallicSilver,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Security Audit Log Header & Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminSectionHeader(
                    title = "Security Event Audit Trail",
                    subtitle = "Chronological record of authentication attempts and parameter changes",
                    icon = Icons.Default.Security
                )

                AdminActionButton(
                    text = "Clear Audit Log",
                    onClick = { showClearDialog = true },
                    isDestructive = true,
                    icon = Icons.Default.DeleteSweep,
                    testTag = "clear_audit_btn"
                )
            }
        }

        // Events List
        if (securityEvents.isEmpty()) {
            item {
                AdminGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No security events recorded.",
                        color = MetallicSilver,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            items(securityEvents, key = { it.id }) { event ->
                val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                AdminGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = when (event.severity) {
                        "CRITICAL" -> RedAlert.copy(alpha = 0.5f)
                        "WARNING" -> AmberAlert.copy(alpha = 0.5f)
                        else -> palette.border
                    },
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AdminStatusBadge(
                                    text = event.type,
                                    color = when (event.severity) {
                                        "CRITICAL" -> RedAlert
                                        "WARNING" -> AmberAlert
                                        else -> palette.primary
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = sdf.format(Date(event.timestamp)),
                                    color = MetallicSilver,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = event.description,
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "CLEAR AUDIT TRAIL?",
                    color = RedAlert,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to purge all recorded security logs? This action will reset the audit log and record a log-cleared event.",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                AdminActionButton(
                    text = "CONFIRM CLEAR",
                    onClick = {
                        onClearSecurityEvents()
                        showClearDialog = false
                    },
                    isDestructive = true
                )
            },
            dismissButton = {
                AdminActionButton(
                    text = "CANCEL",
                    onClick = { showClearDialog = false }
                )
            },
            containerColor = palette.surfaceElevated
        )
    }
}
