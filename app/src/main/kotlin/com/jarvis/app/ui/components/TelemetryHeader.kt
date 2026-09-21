package com.jarvis.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.ui.theme.*

/**
 * High-tech HUD Telemetry Ribbon replacing clunky static cards with
 * movie-grade cybernetic status pills and protocol headers.
 */
@Composable
fun TelemetryHeader(
    aiProviderName: String = "GEMINI",
    isAccessibilityEnabled: Boolean = true,
    isMicReady: Boolean = true,
    isMemoryReady: Boolean = true,
    isOwnerVerified: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TelemetryBlink")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top Banner: Title & Protocol Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Glowing cyan diamond
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ArcCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "J.A.R.V.I.S.",
                    color = ArcCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "// created by ( OmX Infinity - Omkar )",
                    color = TextSecondary,
                    fontSize = 10.5.sp,
                    fontFamily = HudMonospace,
                    letterSpacing = 0.5.sp
                )
            }

            // Real-time protocol status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x3300F0FF))
                    .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyberGreen.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CORE ONLINE",
                        color = CyberGreen,
                        fontFamily = HudMonospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HudStatusPill(
    label: String,
    value: String,
    active: Boolean,
    activeColor: Color
) {
    val borderColor = if (active) activeColor.copy(alpha = 0.5f) else Color(0x334C6688)
    val bgColor = if (active) activeColor.copy(alpha = 0.12f) else Color(0x1A091224)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (active) activeColor else Color.Gray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: ",
                color = TextMuted,
                fontFamily = HudMonospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = if (active) TextPrimary else TextMuted,
                fontFamily = HudMonospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
