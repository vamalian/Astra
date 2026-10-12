package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TitaniumBorder
import com.example.ui.theme.TitaniumSurface

@Composable
fun HudCard(
    modifier: Modifier = Modifier,
    borderColor: Color = TitaniumBorder,
    backgroundColor: Color = DarkGraphite.copy(alpha = 0.92f),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .border(
                border = BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        content()
    }
}

@Composable
fun HudButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    alertStyle: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "hud_button"
) {
    val borderColor = when {
        !enabled -> TitaniumBorder.copy(alpha = 0.4f)
        alertStyle -> Color(0xFFFFB020)
        isPrimary -> ElectricCyan
        else -> TitaniumBorder
    }

    val bgBrush = when {
        !enabled -> Brush.verticalGradient(listOf(DarkGraphite.copy(alpha = 0.4f), TitaniumSurface.copy(alpha = 0.4f)))
        alertStyle -> Brush.verticalGradient(listOf(Color(0x33FFB020), DarkGraphite))
        isPrimary -> Brush.verticalGradient(listOf(Color(0x3300F0FF), TitaniumSurface))
        else -> Brush.verticalGradient(listOf(TitaniumSurface, DarkGraphite))
    }

    val contentColor = when {
        !enabled -> Color(0xFF64748B)
        alertStyle -> Color(0xFFFFB020)
        isPrimary -> ElectricCyan
        else -> PureWhite
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = contentColor),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.75.sp
            )
        }
    }
}

@Composable
fun HudBadge(
    label: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "hud_badge"
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TitaniumSurface.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = PureWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
