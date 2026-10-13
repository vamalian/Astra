package com.example.ui.diagnostics

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.components.AdminActionButton
import com.example.ui.components.AdminGlassCard
import com.example.ui.components.AdminSectionHeader
import com.example.ui.components.AdminStatusBadge
import com.example.ui.components.MetricGaugeCard
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    report: SystemDiagnosticsReport,
    onRefresh: () -> Unit,
    onRunLatencyTest: () -> Unit,
    isTestingLatency: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    val ramUsedMb = report.usedRamBytes / (1024 * 1024)
    val ramTotalMb = report.totalRamBytes / (1024 * 1024)
    val ramAvailMb = report.availableRamBytes / (1024 * 1024)

    val storageFreeGb = report.freeStorageBytes / (1024 * 1024 * 1024)
    val storageTotalGb = report.totalStorageBytes / (1024 * 1024 * 1024)
    val storageUsedGb = report.usedStorageBytes / (1024 * 1024 * 1024)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("diagnostics_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminSectionHeader(
                    title = "System Diagnostics // Hardware Telemetry",
                    subtitle = "Real-time inspection of memory, storage, battery, and network",
                    icon = Icons.Default.Build
                )

                AdminActionButton(
                    text = "Poll Sensors",
                    onClick = onRefresh,
                    icon = Icons.Default.Refresh,
                    testTag = "diag_poll_btn"
                )
            }
        }

        // Live Metric Gauges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricGaugeCard(
                    title = "RAM Allocated",
                    value = "${ramUsedMb} MB",
                    subtitle = "${ramAvailMb} MB Free (${report.ramPercentUsed.toInt()}% Used)",
                    progress = report.ramPercentUsed / 100f,
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f)
                )

                MetricGaugeCard(
                    title = "Internal Disk",
                    value = "${storageUsedGb} GB",
                    subtitle = "${storageFreeGb} GB Available (${report.storagePercentUsed.toInt()}% Used)",
                    progress = report.storagePercentUsed / 100f,
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )

                MetricGaugeCard(
                    title = "Battery Fuel",
                    value = "${report.batteryLevel}%",
                    subtitle = "${report.chargingSource} // ${report.batteryHealth}",
                    progress = report.batteryLevel / 100f,
                    icon = Icons.Default.BatteryChargingFull,
                    accentColor = if (report.batteryLevel < 20) RedAlert else EmeraldActive,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Network Latency & Connectivity Test Card
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "REAL-TIME NETWORK LATENCY & PING BENCHMARK",
                                color = palette.primary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Probes remote gateway socket to evaluate connection quality",
                                color = MetallicSilver,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        AdminActionButton(
                            text = if (isTestingLatency) "Pinging..." else "Execute Ping Test",
                            onClick = onRunLatencyTest,
                            enabled = !isTestingLatency,
                            icon = Icons.Default.NetworkCheck,
                            isPrimary = true,
                            testTag = "diag_ping_test_btn"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiagnosticMetricBox(
                            label = "Transport Link",
                            value = report.networkType,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticMetricBox(
                            label = "Connection State",
                            value = if (report.networkConnected) "Active / Validated" else "Disconnected",
                            statusColor = if (report.networkConnected) EmeraldActive else RedAlert,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticMetricBox(
                            label = "Round-Trip Latency",
                            value = if (report.pingLatencyMs != null) "${report.pingLatencyMs} ms" else "Idle (Unmeasured)",
                            statusColor = if (report.pingLatencyMs != null) palette.primary else MetallicSilver,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isTestingLatency) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = palette.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Measuring socket round-trip time...",
                                color = palette.primary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Detailed Hardware & Platform Specifications
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PLATFORM & ARCHITECTURAL PROFILE",
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            DiagRow("Device Manufacturer", report.deviceManufacturer)
                            DiagRow("Product Model", report.deviceModel)
                            DiagRow("Android OS Version", "Android ${report.androidVersion} (API ${report.sdkInt})")
                            DiagRow("Security Patch", report.securityPatch)
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            DiagRow("Instruction ABI", report.cpuAbi)
                            DiagRow("CPU Multi-Core", "${report.cpuCores} Virtual Cores")
                            DiagRow("Battery Temperature", String.format("%.1f °C", report.batteryTemperatureC))
                            DiagRow("Battery Voltage", "${report.batteryVoltageMv} mV (${report.batteryTechnology})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMetricBox(
    label: String,
    value: String,
    statusColor: androidx.compose.ui.graphics.Color? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    AdminGlassCard(
        modifier = modifier,
        contentPadding = 10.dp
    ) {
        Column {
            Text(
                text = label.uppercase(),
                color = MetallicSilver,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = statusColor ?: PureWhite,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MetallicSilver,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = PureWhite,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
