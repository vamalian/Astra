package com.example.ui.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AdminActionButton
import com.example.ui.components.AdminGlassCard
import com.example.ui.components.AdminSectionHeader
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedAlert

@Composable
fun BackupRestoreScreen(
    onExportConfig: () -> String,
    onImportConfig: (String) -> Result<String>,
    onFactoryReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalNexusPalette.current

    var exportedJson by remember { mutableStateOf("") }
    var importInputJson by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var importSuccess by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("backup_restore_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AdminSectionHeader(
                title = "Backup & Restore // Data Persistence",
                subtitle = "Export JSON manifests, import configuration profiles, or reset defaults",
                icon = Icons.Default.Restore
            )
        }

        // Export Configuration Section
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "EXPORT CONFIGURATION PROFILE",
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Generates a complete JSON manifest of your appearance settings, security timeout preferences, and API endpoints.",
                        color = MetallicSilver,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminActionButton(
                            text = "Generate JSON Export",
                            onClick = {
                                exportedJson = onExportConfig()
                            },
                            isPrimary = true,
                            icon = Icons.Default.Download,
                            testTag = "export_json_btn"
                        )

                        if (exportedJson.isNotEmpty()) {
                            AdminActionButton(
                                text = "Copy to Clipboard",
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ASTRA NEXUS Config", exportedJson)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "JSON manifest copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                icon = Icons.Default.ContentCopy,
                                testTag = "copy_json_btn"
                            )

                            AdminActionButton(
                                text = "Share",
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, exportedJson)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share ASTRA NEXUS Config"))
                                },
                                icon = Icons.Default.Share,
                                testTag = "share_json_btn"
                            )
                        }
                    }

                    if (exportedJson.isNotEmpty()) {
                        OutlinedTextField(
                            value = exportedJson,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = palette.border,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Import Configuration Section
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "IMPORT & RESTORE CONFIGURATION",
                        color = palette.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Paste a valid JSON manifest string to overwrite current parameters and restore console configuration.",
                        color = MetallicSilver,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedTextField(
                        value = importInputJson,
                        onValueChange = {
                            importInputJson = it
                            importStatusMessage = null
                        },
                        placeholder = { Text("Paste JSON config here...", color = MetallicSilver) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("import_json_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    AnimatedVisibility(visible = importStatusMessage != null) {
                        Text(
                            text = importStatusMessage ?: "",
                            color = if (importSuccess) EmeraldActive else RedAlert,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AdminActionButton(
                        text = "Validate & Apply Config",
                        onClick = {
                            val result = onImportConfig(importInputJson)
                            result.onSuccess {
                                importSuccess = true
                                importStatusMessage = it
                                importInputJson = ""
                            }.onFailure {
                                importSuccess = false
                                importStatusMessage = "Import Failed: ${it.localizedMessage ?: "Invalid JSON syntax"}"
                            }
                        },
                        enabled = importInputJson.isNotBlank(),
                        isPrimary = true,
                        icon = Icons.Default.Upload,
                        testTag = "import_apply_btn"
                    )
                }
            }
        }

        // Factory Reset Section
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = RedAlert.copy(alpha = 0.5f),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "DANGER ZONE // FACTORY RESET",
                        color = RedAlert,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Permanently resets all admin credentials, PIN (restores default 600), appearance preferences, and API configs to factory state.",
                        color = MetallicSilver,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    AdminActionButton(
                        text = "Reset All Settings to Defaults",
                        onClick = { showResetDialog = true },
                        isDestructive = true,
                        icon = Icons.Default.DeleteForever,
                        testTag = "factory_reset_btn"
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "CONFIRM FACTORY RESET",
                    color = RedAlert,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you absolutely certain? This will wipe all saved API keys, reset your PIN back to 600, clear audit history, and restore default appearance.",
                    color = PureWhite,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                AdminActionButton(
                    text = "YES, RESET EVERYTHING",
                    onClick = {
                        onFactoryReset()
                        showResetDialog = false
                        Toast.makeText(context, "System reset to factory defaults. Default PIN is 600.", Toast.LENGTH_LONG).show()
                    },
                    isDestructive = true
                )
            },
            dismissButton = {
                AdminActionButton(
                    text = "CANCEL",
                    onClick = { showResetDialog = false }
                )
            },
            containerColor = palette.surfaceElevated
        )
    }
}
