package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminNavDestination
import com.example.ui.about.AboutScreen
import com.example.ui.api.ApiConfigScreen
import com.example.ui.appearance.AppearanceSettingsScreen
import com.example.ui.backup.BackupRestoreScreen
import com.example.ui.components.AdminActionButton
import com.example.ui.components.AdminStatusBadge
import com.example.ui.dashboard.AdminDashboardScreen
import com.example.ui.diagnostics.DiagnosticsScreen
import com.example.ui.login.AdminLoginScreen
import com.example.ui.permissions.PermissionsScreen
import com.example.ui.security.SecuritySettingsScreen
import com.example.ui.theme.AstraNexusTheme
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedAlert
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstraAdminApp(
    viewModel: AstraAdminViewModel,
    modifier: Modifier = Modifier
) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val appearance by viewModel.appearancePreferences.collectAsState()
    val currentDestination by viewModel.currentDestination.collectAsState()
    val systemOverview by viewModel.systemOverview.collectAsState()
    val diagnostics by viewModel.diagnosticsReport.collectAsState()
    val apiConfig by viewModel.apiConfig.collectAsState()
    val securityEvents by viewModel.securityEvents.collectAsState()
    val autoLockEnabled by viewModel.autoLockEnabled.collectAsState()
    val inactivityTimeout by viewModel.inactivityTimeout.collectAsState()
    val isTestingLatency by viewModel.isTestingLatency.collectAsState()
    val isTestingApi by viewModel.isTestingApi.collectAsState()
    val apiTestResult by viewModel.apiTestResult.collectAsState()

    AstraNexusTheme(
        accentTheme = appearance.accentTheme,
        backgroundIntensity = appearance.backgroundIntensity
    ) {
        val palette = LocalNexusPalette.current

        if (!isUnlocked) {
            AdminLoginScreen(
                onUnlockSuccessful = { viewModel.navigateTo(AdminNavDestination.DASHBOARD) },
                verifyPin = { pin -> viewModel.verifyPin(pin) },
                isDefaultPinActive = viewModel.isDefaultPinActive()
            )
        } else {
            // Handle system back button to return to Dashboard if in sub-screen
            BackHandler(enabled = currentDestination != AdminNavDestination.DASHBOARD) {
                viewModel.navigateTo(AdminNavDestination.DASHBOARD)
            }

            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 700.dp
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                if (isWideScreen) {
                    // Tablet Two-Pane Layout with Fixed Sidebar
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.background)
                    ) {
                        // Left Sidebar
                        SidebarContent(
                            currentDestination = currentDestination,
                            onSelectDestination = { dest -> viewModel.navigateTo(dest) },
                            compact = appearance.compactSidebar,
                            modifier = Modifier
                                .width(if (appearance.compactSidebar) 80.dp else 260.dp)
                                .fillMaxHeight()
                        )

                        // Main Content Area
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            TopAdminHeader(
                                title = "ASTRA NEXUS // ADMIN CONSOLE",
                                timeString = if (appearance.showHeaderClock) systemOverview.dateTimeString else null,
                                batteryPct = diagnostics.batteryLevel,
                                isCharging = diagnostics.isCharging,
                                onLockClick = { viewModel.lockConsole() },
                                onMenuClick = null
                            )

                            // Main Destination View
                            AnimatedContent(
                                targetState = currentDestination,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "nav_content"
                            ) { dest ->
                                DestinationRouter(
                                    destination = dest,
                                    viewModel = viewModel,
                                    overview = systemOverview,
                                    diagnostics = diagnostics,
                                    appearance = appearance,
                                    apiConfig = apiConfig,
                                    securityEvents = securityEvents,
                                    autoLockEnabled = autoLockEnabled,
                                    inactivityTimeout = inactivityTimeout,
                                    isTestingLatency = isTestingLatency,
                                    isTestingApi = isTestingApi,
                                    apiTestResult = apiTestResult
                                )
                            }
                        }
                    }
                } else {
                    // Portrait / Compact Screen Layout with Drawer
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            SidebarContent(
                                currentDestination = currentDestination,
                                onSelectDestination = { dest ->
                                    viewModel.navigateTo(dest)
                                    scope.launch { drawerState.close() }
                                },
                                compact = false,
                                modifier = Modifier
                                    .width(280.dp)
                                    .fillMaxHeight()
                            )
                        }
                    ) {
                        Scaffold(
                            containerColor = palette.background,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            topBar = {
                                TopAdminHeader(
                                    title = "ASTRA NEXUS",
                                    timeString = if (appearance.showHeaderClock) systemOverview.dateTimeString else null,
                                    batteryPct = diagnostics.batteryLevel,
                                    isCharging = diagnostics.isCharging,
                                    onLockClick = { viewModel.lockConsole() },
                                    onMenuClick = { scope.launch { drawerState.open() } }
                                )
                            }
                        ) { paddingValues ->
                            Box(modifier = Modifier.padding(paddingValues)) {
                                DestinationRouter(
                                    destination = currentDestination,
                                    viewModel = viewModel,
                                    overview = systemOverview,
                                    diagnostics = diagnostics,
                                    appearance = appearance,
                                    apiConfig = apiConfig,
                                    securityEvents = securityEvents,
                                    autoLockEnabled = autoLockEnabled,
                                    inactivityTimeout = inactivityTimeout,
                                    isTestingLatency = isTestingLatency,
                                    isTestingApi = isTestingApi,
                                    apiTestResult = apiTestResult
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
private fun TopAdminHeader(
    title: String,
    timeString: String?,
    batteryPct: Int,
    isCharging: Boolean,
    onLockClick: () -> Unit,
    onMenuClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(palette.surface.copy(alpha = 0.96f))
            .border(BorderStroke(1.dp, palette.border))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = palette.primary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                color = PureWhite,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            AdminStatusBadge(text = "ARMED", color = EmeraldActive)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (timeString != null) {
                Text(
                    text = timeString,
                    color = palette.primary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Battery telemetry badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (batteryPct < 20) RedAlert else EmeraldActive,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$batteryPct%${if (isCharging) " (CHG)" else ""}",
                    color = PureWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            AdminActionButton(
                text = "Lock",
                onClick = onLockClick,
                icon = Icons.Default.Lock,
                isDestructive = true,
                testTag = "header_lock_btn"
            )
        }
    }
}

@Composable
private fun SidebarContent(
    currentDestination: AdminNavDestination,
    onSelectDestination: (AdminNavDestination) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    Column(
        modifier = modifier
            .background(palette.surfaceElevated.copy(alpha = 0.98f))
            .border(BorderStroke(1.dp, palette.border))
            .padding(12.dp)
            .testTag("admin_navigation_sidebar")
    ) {
        // Console Header
        if (!compact) {
            Column(modifier = Modifier.padding(bottom = 16.dp, start = 8.dp, top = 8.dp)) {
                Text(
                    text = "ASTRA NEXUS",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "ADMIN COMMAND CORE",
                    color = palette.primary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Destinations List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(AdminNavDestination.values()) { dest ->
                val isSelected = currentDestination == dest
                val isLock = dest == AdminNavDestination.LOCK_PANEL

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isSelected -> palette.primary.copy(alpha = 0.2f)
                                isLock -> RedAlert.copy(alpha = 0.1f)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isSelected -> palette.primary
                                isLock -> RedAlert.copy(alpha = 0.4f)
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectDestination(dest) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("nav_${dest.route}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.title,
                        tint = when {
                            isLock -> RedAlert
                            isSelected -> palette.primary
                            else -> MetallicSilver
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    if (!compact) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = dest.title,
                            color = when {
                                isLock -> RedAlert
                                isSelected -> PureWhite
                                else -> MetallicSilver
                            },
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationRouter(
    destination: AdminNavDestination,
    viewModel: AstraAdminViewModel,
    overview: com.example.data.model.SystemOverviewData,
    diagnostics: com.example.data.diagnostics.SystemDiagnosticsReport,
    appearance: com.example.data.model.AppearancePreferences,
    apiConfig: com.example.data.model.ApiConfiguration,
    securityEvents: List<com.example.data.model.SecurityEvent>,
    autoLockEnabled: Boolean,
    inactivityTimeout: com.example.data.model.InactivityTimeout,
    isTestingLatency: Boolean,
    isTestingApi: Boolean,
    apiTestResult: Pair<Boolean, String>?
) {
    when (destination) {
        AdminNavDestination.DASHBOARD -> AdminDashboardScreen(
            overview = overview,
            diagnostics = diagnostics,
            appearance = appearance,
            onNavigate = { viewModel.navigateTo(it) },
            onRefreshDiagnostics = { viewModel.refreshDiagnostics() },
            onRunLatencyTest = { viewModel.runLatencyPingTest() }
        )
        AdminNavDestination.API_CONFIG -> ApiConfigScreen(
            apiConfig = apiConfig,
            onSaveConfig = { viewModel.updateApiConfig(it) },
            onDeleteKey = { viewModel.deleteApiKey() },
            onTestConnection = { url, key -> viewModel.testApiEndpoint(url, key) },
            isTesting = isTestingApi,
            testResult = apiTestResult
        )
        AdminNavDestination.SECURITY -> SecuritySettingsScreen(
            autoLockEnabled = autoLockEnabled,
            onToggleAutoLock = { viewModel.setAutoLockEnabled(it) },
            inactivityTimeout = inactivityTimeout,
            onSelectTimeout = { viewModel.setInactivityTimeout(it) },
            onChangePin = { old, new -> viewModel.changePin(old, new) },
            onLockImmediately = { viewModel.lockConsole("Manual lock from security settings.") },
            securityEvents = securityEvents,
            onClearSecurityEvents = { viewModel.clearSecurityEvents() },
            isDefaultPinActive = viewModel.isDefaultPinActive()
        )
        AdminNavDestination.APPEARANCE -> AppearanceSettingsScreen(
            preferences = appearance,
            onUpdatePreferences = { viewModel.updateAppearance(it) }
        )
        AdminNavDestination.PERMISSIONS -> PermissionsScreen()
        AdminNavDestination.DIAGNOSTICS -> DiagnosticsScreen(
            report = diagnostics,
            onRefresh = { viewModel.refreshDiagnostics() },
            onRunLatencyTest = { viewModel.runLatencyPingTest() },
            isTestingLatency = isTestingLatency
        )
        AdminNavDestination.BACKUP_RESTORE -> BackupRestoreScreen(
            onExportConfig = { viewModel.exportConfigJson() },
            onImportConfig = { viewModel.importConfigJson(it) },
            onFactoryReset = { viewModel.factoryReset() }
        )
        AdminNavDestination.ABOUT -> AboutScreen()
        AdminNavDestination.LOCK_PANEL -> {
            viewModel.lockConsole()
        }
    }
}
