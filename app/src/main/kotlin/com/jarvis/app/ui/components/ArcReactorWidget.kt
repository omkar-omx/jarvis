package com.jarvis.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.agent.AgentState
import com.jarvis.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated OmX Infinity Arc Core Component.
 * Acts as the premier visual indicator of J.A.R.V.I.S.'s operational status,
 * voice listening state, and thinking/automation activity.
 */
@Composable
fun ArcReactorWidget(
    agentState: AgentState,
    isListening: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorInfinite")

    // Outer ring rotation (Clockwise)
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRingRotation"
    )

    // Inner mechanical ring rotation (Counter-Clockwise)
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (agentState == AgentState.EXECUTING) 3000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRingRotation"
    )

    // Core breathing pulse animation
    val corePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulseScale"
    )

    // Core alpha breathing
    val coreGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreGlowAlpha"
    )

    // Outer acoustic ripple when listening
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    // Dynamic State Theme Colors
    val primaryColor = when {
        agentState == AgentState.EMERGENCY_STOPPED -> CyberCrimson
        isListening -> CyberGreen
        agentState == AgentState.EXECUTING || agentState == AgentState.PLANNING -> OmxGold
        else -> ArcCyan
    }

    val secondaryColor = when {
        agentState == AgentState.EMERGENCY_STOPPED -> Color(0xFFFF5252)
        isListening -> NeonCyanBright
        agentState == AgentState.EXECUTING || agentState == AgentState.PLANNING -> CoreAmber
        else -> ElectricBlue
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val maxRadius = size.toPx() / 2f

            // 0. Audio acoustic ripple if listening
            if (isListening) {
                drawCircle(
                    color = primaryColor.copy(alpha = rippleAlpha),
                    radius = maxRadius * 0.95f * rippleScale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 1. Outermost subtle ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f * coreGlowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // 2. Outer segmented tech ring (Clockwise rotation)
            rotate(outerRotation, pivot = center) {
                val outerRadius = maxRadius * 0.90f
                val segments = 12
                val sweepAngle = 360f / segments
                val gap = 8f

                for (i in 0 until segments) {
                    val startAngle = i * sweepAngle + gap / 2f
                    drawArc(
                        color = primaryColor.copy(alpha = if (i % 3 == 0) 0.9f else 0.4f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - gap,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Tiny tick marks on outer ring
                val tickRadius = maxRadius * 0.82f
                val numTicks = 36
                for (t in 0 until numTicks) {
                    val rad = Math.toRadians((t * (360.0 / numTicks)))
                    val p1 = Offset(
                        center.x + (tickRadius * cos(rad)).toFloat(),
                        center.y + (tickRadius * sin(rad)).toFloat()
                    )
                    val p2 = Offset(
                        center.x + ((tickRadius - 4.dp.toPx()) * cos(rad)).toFloat(),
                        center.y + ((tickRadius - 4.dp.toPx()) * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = secondaryColor.copy(alpha = 0.35f),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // 3. Middle circular boundary
            val midRadius = maxRadius * 0.72f
            drawCircle(
                color = primaryColor.copy(alpha = 0.5f),
                radius = midRadius,
                center = center,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )
            )

            // 4. Inner rotating segmented armature (Counter-Clockwise rotation)
            rotate(innerRotation, pivot = center) {
                val innerRadius = maxRadius * 0.56f
                val innerSegments = 6
                val inSweep = 360f / innerSegments
                val inGap = 16f

                for (j in 0 until innerSegments) {
                    val st = j * inSweep + inGap / 2f
                    drawArc(
                        color = secondaryColor.copy(alpha = 0.85f),
                        startAngle = st,
                        sweepAngle = inSweep - inGap,
                        useCenter = false,
                        topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                        size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Square)
                    )
                }
            }

            // 5. Central Reactor Core Orb (Pulsing Glow)
            val coreBaseRadius = maxRadius * 0.38f * corePulseScale
            
            // Outer core halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.8f * coreGlowAlpha),
                        secondaryColor.copy(alpha = 0.4f * coreGlowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreBaseRadius * 1.5f
                ),
                radius = coreBaseRadius * 1.5f,
                center = center
            )

            // Inner intense nuclear core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor
                    ),
                    center = center,
                    radius = coreBaseRadius
                ),
                radius = coreBaseRadius,
                center = center
            )

            // Core containment rim
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = coreBaseRadius * 0.55f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 6. Central Arc Core Identifier
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    agentState == AgentState.EMERGENCY_STOPPED -> "HALT"
                    isListening -> "LISTEN"
                    agentState == AgentState.EXECUTING -> "EXEC"
                    agentState == AgentState.PLANNING -> "PLAN"
                    else -> "JARVIS"
                },
                color = Color.White,
                fontFamily = HudMonospace,
                fontWeight = FontWeight.Black,
                fontSize = if (size < 160.dp) 11.sp else 13.sp,
                letterSpacing = 1.5.sp
            )
        }
    }
}
