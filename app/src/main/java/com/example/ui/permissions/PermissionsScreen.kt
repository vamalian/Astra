package com.example.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.AdminActionButton
import com.example.ui.components.AdminGlassCard
import com.example.ui.components.AdminSectionHeader
import com.example.ui.components.AdminStatusBadge
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite

data class PermissionItem(
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val icon: ImageVector,
    val permissionTag: String,
    val settingsAction: () -> Unit
)

@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalNexusPalette.current

    // Check Notification Permission
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    // Check Network Access
    val hasNetworkStatePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_NETWORK_STATE
    ) == PackageManager.PERMISSION_GRANTED

    // Check Battery Optimization
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

    // Check Vibration
    val hasVibratePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.VIBRATE
    ) == PackageManager.PERMISSION_GRANTED

    val permissionItems = listOf(
        PermissionItem(
            title = "Network Access & Telemetry",
            description = "Allows live socket latency ping testing, API communication, and network state evaluation.",
            isGranted = hasNetworkStatePermission,
            icon = Icons.Default.NetworkCheck,
            permissionTag = "ACCESS_NETWORK_STATE",
            settingsAction = {
                openAppSettings(context)
            }
        ),
        PermissionItem(
            title = "System Alerts & Notifications",
            description = "Enables posting urgent security alerts and administrative audit notifications.",
            isGranted = hasNotificationPermission,
            icon = Icons.Default.Notifications,
            permissionTag = "POST_NOTIFICATIONS",
            settingsAction = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                runCatching { context.startActivity(intent) }
                    .onFailure { openAppSettings(context) }
            }
        ),
        PermissionItem(
            title = "Background Battery Optimization Exemption",
            description = "Allows persistent system monitoring and uninterrupted administration telemetry.",
            isGranted = isIgnoringBatteryOptimizations,
            icon = Icons.Default.BatteryAlert,
            permissionTag = "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            settingsAction = {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                runCatching { context.startActivity(intent) }
                    .onFailure { openAppSettings(context) }
            }
        ),
        PermissionItem(
            title = "Haptic Vibration Feedback",
            description = "Enables hardware tactile vibrations for PIN keypad presses and critical alerts.",
            isGranted = hasVibratePermission,
            icon = Icons.Default.Vibration,
            permissionTag = "VIBRATE",
            settingsAction = {
                openAppSettings(context)
            }
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("permissions_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AdminSectionHeader(
                title = "Permissions Manager // System Privileges",
                subtitle = "Active Android runtime permissions and system security status",
                icon = Icons.Default.VerifiedUser
            )
        }

        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = palette.primary.copy(alpha = 0.5f),
                contentPadding = 12.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "LEAST PRIVILEGE ARCHITECTURE: ASTRA NEXUS requests only strictly required operational permissions without excessive device control privileges.",
                        color = palette.primaryLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        items(permissionItems.size) { index ->
            val item = permissionItems[index]
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (item.isGranted) EmeraldActive.copy(alpha = 0.4f) else AmberAlert.copy(alpha = 0.4f),
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (item.isGranted) EmeraldActive else AmberAlert,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    color = PureWhite,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                AdminStatusBadge(
                                    text = if (item.isGranted) "GRANTED" else "NOT GRANTED",
                                    color = if (item.isGranted) EmeraldActive else AmberAlert
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                color = MetallicSilver,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Permission Tag: ${item.permissionTag}",
                                color = palette.primary.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    AdminActionButton(
                        text = "Settings",
                        onClick = item.settingsAction,
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        isPrimary = false,
                        testTag = "perm_btn_${item.permissionTag}"
                    )
                }
            }
        }

        item {
            AdminActionButton(
                text = "Open Application Settings",
                onClick = { openAppSettings(context) },
                icon = Icons.Default.Security,
                isPrimary = true,
                modifier = Modifier.fillMaxWidth(),
                testTag = "open_all_settings_btn"
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
