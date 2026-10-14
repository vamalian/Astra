package com.example.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReactorState
import com.example.ui.components.AdminGlassCard
import com.example.ui.components.ArcReactor
import com.example.ui.components.HudButton
import com.example.ui.components.PinKeypad
import com.example.ui.theme.LocalNexusPalette
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedAlert
import kotlinx.coroutines.delay

@Composable
fun AdminLoginScreen(
    onUnlockSuccessful: () -> Unit,
    onBackToAssistant: () -> Unit,
    verifyPin: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToAssistant()
    }

    val palette = LocalNexusPalette.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutSecondsRemaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(lockoutSecondsRemaining) {
        if (lockoutSecondsRemaining > 0) {
            delay(1000)
            lockoutSecondsRemaining--
            if (lockoutSecondsRemaining == 0) {
                errorMessage = null
            }
        }
    }

    fun submitPin() {
        if (lockoutSecondsRemaining > 0) return
        if (enteredPin.isEmpty()) return

        val valid = verifyPin(enteredPin)
        if (valid) {
            errorMessage = null
            failedAttempts = 0
            enteredPin = ""
            onUnlockSuccessful()
        } else {
            failedAttempts++
            enteredPin = ""
            if (failedAttempts >= 5) {
                lockoutSecondsRemaining = 30
                errorMessage = "LOCKOUT ACTIVE // TOO MANY ATTEMPTS: Please wait 30s"
            } else {
                errorMessage = "Incorrect PIN // Attempt $failedAttempts of 5"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("admin_login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ARC Reactor Status
            ArcReactor(
                state = if (errorMessage != null || lockoutSecondsRemaining > 0) ReactorState.SECURITY_ALERT else ReactorState.IDLE,
                reactorSize = 130.dp,
                customStatusText = if (lockoutSecondsRemaining > 0) "SECURITY LOCKOUT // $lockoutSecondsRemaining SECONDS" else "ADMIN SECURITY // AUTHENTICATION REQUIRED"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ASTRA AI",
                color = PureWhite,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Text(
                text = "ADMINISTRATIVE CONTROL GATEWAY",
                color = palette.primary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error notice
            AnimatedVisibility(visible = errorMessage != null) {
                AdminGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    borderColor = RedAlert,
                    contentPadding = 10.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RedAlert,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = RedAlert,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Masked PIN display circles (NEVER shows numbers, only masked dots)
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .testTag("pin_dots_display"),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayCount = maxOf(4, enteredPin.length)
                for (i in 0 until displayCount) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) palette.primary else palette.surfaceElevated)
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    if (isFilled) palette.primaryLight else palette.border
                                ),
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Numeric PIN Keypad
            PinKeypad(
                onDigitClick = { digit ->
                    if (lockoutSecondsRemaining == 0 && enteredPin.length < 8) {
                        enteredPin += digit
                        errorMessage = null
                    }
                },
                onBackspaceClick = {
                    if (lockoutSecondsRemaining == 0 && enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                },
                onClearClick = {
                    if (lockoutSecondsRemaining == 0) {
                        enteredPin = ""
                        errorMessage = null
                    }
                },
                onSubmitClick = { submitPin() },
                submitEnabled = lockoutSecondsRemaining == 0 && enteredPin.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Return to ASTRA button
            HudButton(
                text = "RETURN TO ASTRA ASSISTANT",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBackToAssistant,
                modifier = Modifier.fillMaxWidth(0.85f),
                testTag = "return_to_assistant_button"
            )
        }
    }
}
