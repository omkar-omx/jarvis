package com.jarvis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.ui.theme.*

/**
 * Holographic Dialogue Terminal displaying conversations between
 * the Operator (User) and J.A.R.V.I.S. with movie-style telemetry styling.
 */
@Composable
fun ConversationPanel(
    messages: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onQuickActionClick: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Quick Action Chips Row
        QuickActionRibbon(onQuickActionClick = onQuickActionClick)

        Spacer(modifier = Modifier.height(4.dp))

        // Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyDialogueState()
                }
            } else {
                items(messages) { (role, content) ->
                    val isUser = role.equals("user", ignoreCase = true)
                    val isSystem = role.equals("system", ignoreCase = true)

                    DialogueMessageBubble(
                        role = role,
                        content = content,
                        isUser = isUser,
                        isSystem = isSystem
                    )
                }
            }
        }
    }
}

@Composable
fun DialogueMessageBubble(
    role: String,
    content: String,
    isUser: Boolean,
    isSystem: Boolean
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = when {
            isUser -> Alignment.CenterEnd
            isSystem -> Alignment.Center
            else -> Alignment.CenterStart
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(if (isSystem) 0.95f else 0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Speaker Tag + Status Marker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (!isUser) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(ArcCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = when {
                        isUser -> "OPERATOR // OMKAR"
                        isSystem -> "SYS // BROADCAST"
                        else -> "J.A.R.V.I.S. // NEURAL CORE"
                    },
                    fontFamily = HudMonospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isUser -> ElectricBlue
                        isSystem -> OmxGold
                        else -> ArcCyan
                    },
                    letterSpacing = 1.sp
                )
                if (isUser) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(ElectricBlue)
                    )
                }
            }

            // Glassmorphic Message Container
            val bubbleBg = when {
                isUser -> Brush.linearGradient(listOf(Color(0xCC002B4D), Color(0xCC001833)))
                isSystem -> Brush.linearGradient(listOf(Color(0xCC331B00), Color(0xCC1A0D00)))
                else -> Brush.linearGradient(listOf(Color(0xCC09182E), Color(0xCC050D1A)))
            }
            val borderBrush = when {
                isUser -> Brush.linearGradient(listOf(ElectricBlue.copy(alpha = 0.6f), Color.Transparent))
                isSystem -> Brush.linearGradient(listOf(OmxGold.copy(alpha = 0.5f), Color.Transparent))
                else -> Brush.linearGradient(listOf(ArcCyan.copy(alpha = 0.7f), ElectricBlue.copy(alpha = 0.2f)))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(bubbleBg)
                    .border(
                        width = 1.dp,
                        brush = borderBrush,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = content,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    fontFamily = if (isSystem) HudMonospace else androidx.compose.ui.text.font.FontFamily.Default
                )
            }
        }
    }
}

@Composable
fun QuickActionRibbon(
    onQuickActionClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickChip(label = "Status Report", icon = Icons.Default.Info) {
            onQuickActionClick("JARVIS, give me a complete system status report.")
        }
        QuickChip(label = "Read Screen", icon = Icons.Default.Smartphone) {
            onQuickActionClick("Analyze the current screen and tell me what actions you can take.")
        }
        QuickChip(label = "Search Web", icon = Icons.Default.Search) {
            onQuickActionClick("Search the web for the latest artificial intelligence news.")
        }
        QuickChip(label = "Show Memories", icon = Icons.Default.Memory) {
            onQuickActionClick("What do you remember about me?")
        }
        QuickChip(label = "Quiet Mode", icon = Icons.Default.VolumeOff) {
            onQuickActionClick("Enable quiet mode.")
        }
    }
}

@Composable
fun QuickChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x3300F0FF))
            .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ArcCyan,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = TextHolo,
                fontSize = 11.sp,
                fontFamily = HudMonospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyDialogueState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NEURAL LINK STANDBY",
                color = ArcCyan.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = HudMonospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Awaiting voice command or text query, sir.",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = HudMonospace
            )
        }
    }
}
