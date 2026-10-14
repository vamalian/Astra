package com.example.ui.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.diagnostics.SystemDiagnosticsReport
import com.example.data.model.AdminNavDestination
import com.example.data.model.AppearancePreferences
import com.example.data.model.ReactorState
import com.example.data.model.SystemOverviewData
import com.example.ui.components.AdminActionButton
import com.example.ui.components.AdminGlassCard
import com.example.ui.components.AdminSectionHeader
import com.example.ui.components.AdminStatusBadge
import com.example.ui.components.ArcReactor
import com.example.ui.components.MetricGaugeCard
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite

@Composable
fun AdminDashboardScreen(
    overview: SystemOverviewData,
    diagnostics: SystemDiagnosticsReport,
    appearance: AppearancePreferences,
    onNavigate: (AdminNavDestination) -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onRunLatencyTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Section: Title & Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminSectionHeader(
                    title = "System Overview // Command Core",
                    subtitle = "Real-time tablet telemetry & administrative parameters",
                    icon = Icons.Default.Security
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminStatusBadge(
                        text = overview.adminSecurityStatus,
                        color = EmeraldActive
                    )
                    AdminActionButton(
                        text = "Refresh",
                        onClick = onRefreshDiagnostics,
                        icon = Icons.Default.Refresh,
                        testTag = "dashboard_refresh_btn"
                    )
                }
            }
        }

        // ARC Reactor & High-Level System Status Hero Card
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowEffect = true,
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Column: Telemetry Specs
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = "ASTRA AI HOST: ${overview.deviceName.ifEmpty { diagnostics.deviceManufacturer + " " + diagnostics.deviceModel }}",
                            color = PureWhite,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "DATE / TIME: ${overview.dateTimeString}",
                            color = palette.primary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OS VERSION: Android ${diagnostics.androidVersion} (API ${diagnostics.sdkInt}) // Patch: ${diagnostics.securityPatch}",
                            color = MetallicSilver,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CONFIGURATION: ${overview.configStatus} // Build v${overview.appVersion}",
                            color = MetallicSilver,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick action shortcuts
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminActionButton(
                                text = "API Config",
                                onClick = { onNavigate(AdminNavDestination.API_CONFIG) },
                                isPrimary = false,
                                testTag = "dash_nav_api_btn"
                            )
                            AdminActionButton(
                                text = "Diagnostics",
                                onClick = { onNavigate(AdminNavDestination.DIAGNOSTICS) },
                                isPrimary = false,
                                testTag = "dash_nav_diag_btn"
                            )
                            AdminActionButton(
                                text = "Ping Test",
                                onClick = onRunLatencyTest,
                                isPrimary = true,
                                icon = Icons.Default.NetworkCheck,
                                testTag = "dash_ping_test_btn"
                            )
                        }
                    }

                    // Right: Circular ARC Reactor Graphic
                    if (appearance.showArcReactor) {
                        Box(
                            modifier = Modifier.padding(start = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ArcReactor(
                                state = ReactorState.IDLE,
                                reactorSize = 160.dp,
                                customStatusText = "CORE SYNCHRONIZED"
                            )
                        }
                    }
                }
            }
        }

        // Live Telemetry Metric Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // RAM Gauge
                val ramUsedMb = diagnostics.usedRamBytes / (1024 * 1024)
                val ramTotalMb = diagnostics.totalRamBytes / (1024 * 1024)
                MetricGaugeCard(
                    title = "System RAM",
                    value = "${ramUsedMb}M / ${ramTotalMb}M",
                    subtitle = "Usage: ${diagnostics.ramPercentUsed.toInt()}%",
                    progress = diagnostics.ramPercentUsed / 100f,
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f),
                    testTag = "dash_metric_ram"
                )

                // Storage Gauge
                val storageFreeGb = diagnostics.freeStorageBytes / (1024 * 1024 * 1024)
                val storageTotalGb = diagnostics.totalStorageBytes / (1024 * 1024 * 1024)
                MetricGaugeCard(
                    title = "Storage",
                    value = "${storageFreeGb}G Free",
                    subtitle = "Total: ${storageTotalGb}GB (${diagnostics.storagePercentUsed.toInt()}% used)",
                    progress = diagnostics.storagePercentUsed / 100f,
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f),
                    testTag = "dash_metric_storage"
                )

                // Battery Gauge
                MetricGaugeCard(
                    title = "Battery",
                    value = "${diagnostics.batteryLevel}%",
                    subtitle = "${diagnostics.chargingSource} (${if (diagnostics.isCharging) "Charging" else "Discharging"})",
                    progress = diagnostics.batteryLevel / 100f,
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = if (diagnostics.batteryLevel < 20) com.example.ui.theme.RedAlert else EmeraldActive,
                    modifier = Modifier.weight(1f),
                    testTag = "dash_metric_battery"
                )

                // Network Gauge
                MetricGaugeCard(
                    title = "Network",
                    value = if (diagnostics.networkConnected) diagnostics.networkType.take(10) else "Offline",
                    subtitle = if (diagnostics.pingLatencyMs != null) "Latency: ${diagnostics.pingLatencyMs}ms" else "Status: Connected",
                    progress = if (diagnostics.networkConnected) 1.0f else 0f,
                    icon = Icons.Default.Wifi,
                    accentColor = palette.primary,
                    modifier = Modifier.weight(1f),
                    testTag = "dash_metric_network"
                )
            }
        }

        // System Overview Specifications Card
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column {
                    Text(
                        text = "HARDWARE & RUNTIME TELEMETRY",
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SpecRow("Device Model", "${diagnostics.deviceManufacturer} ${diagnostics.deviceModel}")
                            SpecRow("Architecture / ABI", diagnostics.cpuAbi)
                            SpecRow("CPU Processor Cores", "${diagnostics.cpuCores} Physical Cores")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SpecRow("Battery Voltage", "${diagnostics.batteryVoltageMv} mV")
                            SpecRow("Battery Temperature", String.format("%.1f °C", diagnostics.batteryTemperatureC))
                            SpecRow("Battery Chemistry", diagnostics.batteryTechnology)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MetallicSilver,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = PureWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
