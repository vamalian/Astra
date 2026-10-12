package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReactorState
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RedAlert
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactor(
    state: ReactorState,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    reactorSize: Dp = 260.dp,
    customStatusText: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    // Outer slow rotation
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == ReactorState.THINKING) 8000 else 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    // Inner counter-rotation
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == ReactorState.THINKING) 4000 else 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    // Pulse factor (speaking/listening/idle)
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    ReactorState.SPEAKING -> 450
                    ReactorState.LISTENING -> 600
                    ReactorState.THINKING -> 350
                    ReactorState.SECURITY_ALERT -> 280
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Primary energy tint based on state
    val (primaryEnergyColor, glowEnergyColor) = when (state) {
        ReactorState.SECURITY_ALERT -> AmberAlert to RedAlert
        ReactorState.ERROR -> RedAlert to AmberAlert
        ReactorState.GUARD_ARMED -> IceBlue to ElectricCyan
        else -> ElectricCyan to IceBlue
    }

    Column(
        modifier = modifier.testTag("arc_reactor_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(reactorSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("arc_reactor_canvas")
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.92f

                drawArcReactorCanvas(
                    center = center,
                    baseRadius = baseRadius,
                    state = state,
                    outerAngle = outerAngle,
                    innerAngle = innerAngle,
                    pulseFactor = pulseFactor,
                    audioRms = audioRms,
                    primaryColor = primaryEnergyColor,
                    glowColor = glowEnergyColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // HUD Status Text below Reactor
        Text(
            text = "ASTRA CORE",
            color = MetallicSilver,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        val statusLabel = customStatusText ?: when (state) {
            ReactorState.IDLE -> "ONLINE // SYSTEM READY"
            ReactorState.LISTENING -> "AUDIO INPUT // LISTENING..."
            ReactorState.THINKING -> "QUANTUM LOGIC // PROCESSING..."
            ReactorState.SPEAKING -> "SYNTHESIZING // VOCAL OUTPUT"
            ReactorState.GUARD_ARMED -> "ASTRA GUARD // SYSTEM ARMED"
            ReactorState.SECURITY_ALERT -> "SECURITY ALERT // BREACH DETECTED"
            ReactorState.ERROR -> "SYSTEM NOTICE // RECOVERY MODE"
        }

        Text(
            text = statusLabel,
            color = when (state) {
                ReactorState.SECURITY_ALERT -> AmberAlert
                ReactorState.ERROR -> RedAlert
                ReactorState.GUARD_ARMED -> IceBlue
                else -> ElectricCyan
            },
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

private fun DrawScope.drawArcReactorCanvas(
    center: Offset,
    baseRadius: Float,
    state: ReactorState,
    outerAngle: Float,
    innerAngle: Float,
    pulseFactor: Float,
    audioRms: Float,
    primaryColor: Color,
    glowColor: Color
) {
    // Audio-reactive scale boost when listening
    val rmsScale = if (state == ReactorState.LISTENING && audioRms > 0) {
        (audioRms / 12f).coerceIn(0f, 0.45f)
    } else 0f

    // 1. Atmospheric ambient glow halo
    val haloRadius = baseRadius * (1.0f + rmsScale * 0.2f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = if (state == ReactorState.SECURITY_ALERT) 0.35f else 0.18f),
                glowColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )

    // 2. Outermost Segmented Ring (Clockwise)
    val outerRingRadius = baseRadius * 0.94f
    drawCircle(
        color = primaryColor.copy(alpha = 0.3f),
        radius = outerRingRadius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // Outer notched segments
    val outerSegments = 12
    for (i in 0 until outerSegments) {
        val sweepAngle = 20f
        val startAngle = outerAngle + (i * 360f / outerSegments)
        drawArc(
            color = primaryColor.copy(alpha = 0.85f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - outerRingRadius, center.y - outerRingRadius),
            size = androidx.compose.ui.geometry.Size(outerRingRadius * 2, outerRingRadius * 2),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Outer technical nodes / dots
    val nodeRadius = outerRingRadius * 1.03f
    val nodeCount = 24
    for (i in 0 until nodeCount) {
        val angleRad = Math.toRadians((outerAngle * 0.5 + i * (360f / nodeCount)).toDouble())
        val nodeX = center.x + (nodeRadius * cos(angleRad)).toFloat()
        val nodeY = center.y + (nodeRadius * sin(angleRad)).toFloat()
        val dotAlpha = if (i % 2 == 0) 0.9f else 0.4f
        drawCircle(
            color = if (i % 4 == 0) PureWhite else primaryColor.copy(alpha = dotAlpha),
            radius = (if (i % 4 == 0) 2.5f else 1.5f).dp.toPx(),
            center = Offset(nodeX, nodeY)
        )
    }

    // 3. Middle Mechanical Gear / Ring with Dashes (Counter-Clockwise)
    val midRingRadius = baseRadius * 0.74f
    drawCircle(
        color = glowColor.copy(alpha = 0.4f),
        radius = midRingRadius,
        center = center,
        style = Stroke(
            width = 2.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
        )
    )

    // Middle tick marks radiating inward
    val tickCount = 36
    val tickInnerRadius = midRingRadius * 0.90f
    for (i in 0 until tickCount) {
        val angleRad = Math.toRadians((innerAngle + i * (360f / tickCount)).toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        val startPt = Offset(center.x + midRingRadius * cosA, center.y + midRingRadius * sinA)
        val endPt = Offset(center.x + tickInnerRadius * cosA, center.y + tickInnerRadius * sinA)
        val isMajor = i % 3 == 0
        drawLine(
            color = if (isMajor) PureWhite.copy(alpha = 0.8f) else primaryColor.copy(alpha = 0.4f),
            start = startPt,
            end = endPt,
            strokeWidth = (if (isMajor) 2f else 1f).dp.toPx()
        )
    }

    // 4. Inner High-Energy Ring
    val innerRingRadius = baseRadius * 0.52f
    drawCircle(
        color = primaryColor.copy(alpha = 0.6f),
        radius = innerRingRadius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )

    // Inner Arc Segments rotating forward
    val innerSegments = 6
    for (i in 0 until innerSegments) {
        val sweepAngle = 40f
        val startAngle = (outerAngle * 1.5f) + (i * 360f / innerSegments)
        drawArc(
            color = PureWhite.copy(alpha = 0.9f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius),
            size = androidx.compose.ui.geometry.Size(innerRingRadius * 2, innerRingRadius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // 5. Radial Energy Spoke Lines from Inner Ring to Core
    val spokeCount = 8
    val coreBoundaryRadius = baseRadius * 0.32f
    for (i in 0 until spokeCount) {
        val angleRad = Math.toRadians((innerAngle * 0.8f + i * (360f / spokeCount)).toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        drawLine(
            color = primaryColor.copy(alpha = 0.75f),
            start = Offset(center.x + coreBoundaryRadius * cosA, center.y + coreBoundaryRadius * sinA),
            end = Offset(center.x + innerRingRadius * cosA, center.y + innerRingRadius * sinA),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 6. Central Energy Core (Brilliant White with Radial Electric Cyan/Amber Glow)
    val dynamicCoreRadius = (baseRadius * 0.28f) * (pulseFactor + rmsScale)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                PureWhite,
                primaryColor,
                glowColor.copy(alpha = 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = dynamicCoreRadius * 1.4f
        ),
        radius = dynamicCoreRadius * 1.4f,
        center = center
    )

    // Solid brilliant white center
    drawCircle(
        color = PureWhite,
        radius = dynamicCoreRadius * 0.55f,
        center = center
    )

    // 7. Security Alert or Guard Armed specific overlays
    if (state == ReactorState.GUARD_ARMED) {
        // Draw triangular shield markers at cardinal points
        val cardinalAngles = floatArrayOf(0f, 90f, 180f, 270f)
        for (ang in cardinalAngles) {
            val rad = Math.toRadians(ang.toDouble())
            val markerDist = baseRadius * 0.85f
            val mx = center.x + (markerDist * cos(rad)).toFloat()
            val my = center.y + (markerDist * sin(rad)).toFloat()
            drawCircle(
                color = IceBlue,
                radius = 4.dp.toPx(),
                center = Offset(mx, my)
            )
        }
    } else if (state == ReactorState.SECURITY_ALERT) {
        // Warning hazard strobe ring
        drawCircle(
            color = AmberAlert.copy(alpha = (pulseFactor - 0.7f).coerceIn(0.2f, 0.9f)),
            radius = baseRadius * 0.82f,
            center = center,
            style = Stroke(
                width = 5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), outerAngle * 2f)
            )
        )
    }
}
