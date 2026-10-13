package com.example.ui.api

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApiConfiguration
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
fun ApiConfigScreen(
    apiConfig: ApiConfiguration,
    onSaveConfig: (ApiConfiguration) -> Unit,
    onDeleteKey: () -> Unit,
    onTestConnection: (url: String, key: String) -> Unit,
    isTesting: Boolean,
    testResult: Pair<Boolean, String>?,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    var editedApiKey by remember(apiConfig.apiKey) { mutableStateOf(apiConfig.apiKey) }
    var editedEndpoint by remember(apiConfig.endpointUrl) { mutableStateOf(apiConfig.endpointUrl) }
    var editedProvider by remember(apiConfig.providerName) { mutableStateOf(apiConfig.providerName) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("api_config_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AdminSectionHeader(
                title = "API Configuration // Credentials",
                subtitle = "Manage external endpoints and authenticated service keys",
                icon = Icons.Default.Key
            )
        }

        // Connection Status Banner
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = when (apiConfig.lastStatus) {
                    "SUCCESS" -> EmeraldActive
                    "FAILED" -> RedAlert
                    else -> palette.border
                },
                contentPadding = 14.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ENDPOINT STATUS: ${apiConfig.lastStatus}",
                            color = when (apiConfig.lastStatus) {
                                "SUCCESS" -> EmeraldActive
                                "FAILED" -> RedAlert
                                else -> AmberAlert
                            },
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (apiConfig.lastTestedTime > 0) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            Text(
                                text = "Last Tested: ${sdf.format(Date(apiConfig.lastTestedTime))} // Latency: ${apiConfig.lastLatencyMs}ms",
                                color = MetallicSilver,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "No connection test performed yet.",
                                color = MetallicSilver,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    AdminStatusBadge(
                        text = if (apiConfig.apiKey.isNotEmpty()) "KEY STORED" else "UNCONFIGURED",
                        color = if (apiConfig.apiKey.isNotEmpty()) EmeraldActive else AmberAlert
                    )
                }
            }
        }

        // Credentials Configuration Form
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "SERVICE PARAMETERS",
                        color = palette.primary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Provider Name
                    OutlinedTextField(
                        value = editedProvider,
                        onValueChange = { editedProvider = it },
                        label = { Text("Provider Name / Label") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_provider_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedLabelColor = palette.primary,
                            unfocusedLabelColor = MetallicSilver
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Endpoint URL
                    OutlinedTextField(
                        value = editedEndpoint,
                        onValueChange = { editedEndpoint = it },
                        label = { Text("Endpoint URL (REST / Gateway)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_endpoint_url"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedLabelColor = palette.primary,
                            unfocusedLabelColor = MetallicSilver
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Secret API Key Input
                    OutlinedTextField(
                        value = editedApiKey,
                        onValueChange = { editedApiKey = it },
                        label = { Text("API Key / Bearer Token") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_api_key"),
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isKeyVisible) "Hide Key" else "Reveal Key",
                                    tint = palette.primary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.primary,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedLabelColor = palette.primary,
                            unfocusedLabelColor = MetallicSilver
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Feedback Message
                    AnimatedVisibility(visible = saveMessage != null) {
                        Text(
                            text = saveMessage ?: "",
                            color = EmeraldActive,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Test Result Feedback
                    AnimatedVisibility(visible = testResult != null) {
                        val (success, msg) = testResult ?: (false to "")
                        Text(
                            text = "TEST RESULT: $msg",
                            color = if (success) EmeraldActive else RedAlert,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AdminActionButton(
                            text = "Save Credentials",
                            onClick = {
                                val updated = apiConfig.copy(
                                    apiKey = editedApiKey,
                                    endpointUrl = editedEndpoint,
                                    providerName = editedProvider
                                )
                                onSaveConfig(updated)
                                saveMessage = "Credentials updated and secured in persistent storage."
                            },
                            isPrimary = true,
                            icon = Icons.Default.Save,
                            testTag = "api_save_btn"
                        )

                        AdminActionButton(
                            text = if (isTesting) "Testing..." else "Test Connectivity",
                            onClick = {
                                onTestConnection(editedEndpoint, editedApiKey)
                            },
                            enabled = !isTesting && editedEndpoint.isNotBlank(),
                            icon = Icons.Default.NetworkCheck,
                            testTag = "api_test_btn"
                        )

                        if (apiConfig.apiKey.isNotEmpty()) {
                            AdminActionButton(
                                text = "Delete Key",
                                onClick = {
                                    editedApiKey = ""
                                    onDeleteKey()
                                    saveMessage = "API key has been deleted."
                                },
                                isDestructive = true,
                                icon = Icons.Default.Delete,
                                testTag = "api_delete_btn"
                            )
                        }

                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = palette.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        // Security Notice
        item {
            AdminGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AmberAlert.copy(alpha = 0.4f),
                contentPadding = 12.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberAlert,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CREDENTIAL ISOLATION: API tokens are kept in encrypted local storage and never printed in logs or broadcast over insecure channels.",
                        color = AmberAlert,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
