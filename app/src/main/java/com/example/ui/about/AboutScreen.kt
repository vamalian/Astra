package com.example.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReactorState
import com.example.ui.components.AdminGlassCard
import com.example.ui.components.AdminSectionHeader
import com.example.ui.components.AdminStatusBadge
import com.example.ui.components.ArcReactor
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("about_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AdminSectionHeader(
                title = "About // System Architecture",
                subtitle = "Product profile, security specifications, and build manifest",
                icon = Icons.Default.Info
            )
        }

        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowEffect = true,
                contentPadding = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ASTRA AI",
                            color = PureWhite,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "EXECUTIVE AI ASSISTANT & INTEGRATED ADMIN PANEL",
                            color = palette.primary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "An advanced JARVIS-inspired executive AI assistant with native voice synthesis, real-time Gemini AI integration, multi-ring animated Arc Reactor, and an integrated private administration console.",
                            color = MetallicSilver,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminStatusBadge(text = "VERSION 1.0.0", color = palette.primary)
                            AdminStatusBadge(text = "RELEASE STABLE", color = EmeraldActive)
                            AdminStatusBadge(text = "TABLET READY", color = palette.primaryLight)
                        }
                    }

                    Box(
                        modifier = Modifier.padding(start = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ArcReactor(
                            state = ReactorState.IDLE,
                            reactorSize = 130.dp,
                            customStatusText = "CORE V1.0.0"
                        )
                    }
                }
            }
        }

        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ARCHITECTURAL BLUEPRINT",
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    AboutRow("User Interface Engine", "Jetpack Compose 1.7+ with Material Design 3")
                    AboutRow("Programming Language", "Kotlin 2.0+ (Strict Type-Safety)")
                    AboutRow("State Management", "MVVM Architecture with StateFlow & Coroutines")
                    AboutRow("Persistent Storage", "Encrypted SharedPreferences + Moshi JSON Serializer")
                    AboutRow("Network & Diagnostics", "OkHttp Client with Direct Socket Ping Telemetry")
                    AboutRow("Hardware Telemetry", "ActivityManager, BatteryManager & StatFs APIs")
                    AboutRow("Session Security", "PIN Protection with Configurable Window Inactivity Auto-Lock")
                }
            }
        }

        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ADMINISTRATIVE COMPLIANCE & PRIVACY",
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "ASTRA NEXUS functions as a dedicated administration cockpit. It does not collect tracking analytics, nor does it broadcast sensitive keys to third parties. All administrative modifications remain isolated to your host tablet environment.",
                        color = MetallicSilver,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
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
