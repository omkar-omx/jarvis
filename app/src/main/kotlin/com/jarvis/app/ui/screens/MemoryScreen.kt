package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MemoryItem(
    val id: String,
    val content: String,
    val category: String,
    val importance: Int,
    val date: String
)

class MemoryViewModel : ViewModel() {
    private val _memories = MutableStateFlow<List<MemoryItem>>(
        listOf(
            MemoryItem("1", "User prefers holographic dark HUD interface.", "Preference", 9, "2026-09-20"),
            MemoryItem("2", "Security Protocol: Biometric owner authorization required for financial transactions.", "Protocol", 10, "2026-09-21"),
            MemoryItem("3", "Primary AI Reasoning Engine linked to Google Gemini Core.", "Architecture", 8, "2026-09-21"),
            MemoryItem("4", "Voice interface trained for English, Hindi, and Hinglish syntax.", "Voice Core", 8, "2026-09-21")
        )
    )
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteMemory(id: String) {
        _memories.value = _memories.value.filter { it.id != id }
    }
}

@Composable
fun MemoryScreen(viewModel: MemoryViewModel) {
    val memories by viewModel.memories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredMemories = remember(memories, searchQuery) {
        if (searchQuery.isBlank()) memories
        else memories.filter {
            it.content.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val bgGradient = Brush.verticalGradient(
        listOf(VoidBlack, DeepSpaceNavy, Color(0xFF040A14))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300F0FF))
                    .border(1.dp, ArcCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = ArcCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "NEURAL MEMORY VAULT",
                    color = ArcCyan,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = HudMonospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "STARK ENCRYPTED LOCAL ENGRAMS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = HudMonospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Cyber Search Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x99091426))
                .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ArcCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Scan neural memories...",
                            color = TextMuted,
                            fontFamily = HudMonospace,
                            fontSize = 12.5.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Memories List
        if (filteredMemories.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO NEURAL ENGRAMS LOCATED",
                        color = TextMuted,
                        fontFamily = HudMonospace,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMemories, key = { it.id }) { memory ->
                    CyberMemoryCard(
                        memory = memory,
                        onDelete = { viewModel.deleteMemory(memory.id) }
                    )
                }
            }
        }

        // Bottom Telemetry Stats Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x80070F1E))
                .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ENGRAMS: ${memories.size}",
                    color = ArcCyan,
                    fontFamily = HudMonospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ENCRYPTION: AES-256",
                    color = CyberGreen,
                    fontFamily = HudMonospace,
                    fontSize = 10.sp
                )
                Text(
                    text = "INDEX: SYNCHRONIZED",
                    color = TextSecondary,
                    fontFamily = HudMonospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun CyberMemoryCard(
    memory: MemoryItem,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC09182E))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0x8000F0FF), Color(0x260077FE))),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Category & Importance tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3300F0FF))
                            .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = memory.category.uppercase(),
                            color = ArcCyan,
                            fontSize = 9.5.sp,
                            fontFamily = HudMonospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "PRIORITY: ${memory.importance}/10",
                        color = if (memory.importance >= 8) StarkGold else TextMuted,
                        fontSize = 9.5.sp,
                        fontFamily = HudMonospace,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = memory.date,
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = HudMonospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = memory.content,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Memory",
                    tint = CyberCrimson.copy(alpha = 0.7f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
