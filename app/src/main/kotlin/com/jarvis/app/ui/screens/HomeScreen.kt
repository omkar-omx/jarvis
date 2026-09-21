package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.app.agent.AgentState
import com.jarvis.app.security.EmergencyStop
import com.jarvis.app.ui.components.ArcReactorWidget
import com.jarvis.app.ui.components.CommandInput
import com.jarvis.app.ui.components.ConversationPanel
import com.jarvis.app.ui.components.TelemetryHeader
import com.jarvis.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _agentState = MutableStateFlow(AgentState.IDLE)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private val _messages = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            "jarvis" to "Good day, sir. All core protocols initialized. Ready for your command."
        )
    )
    val messages: StateFlow<List<Pair<String, String>>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    fun processCommand(command: String) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add("user" to command)
        _messages.value = currentMessages

        viewModelScope.launch {
            _agentState.value = AgentState.PLANNING

            val response = try {
                val provider = com.jarvis.app.brain.BrainManager.activeProvider
                provider.generateResponse(command)
            } catch (e: Exception) {
                "Sir, I encountered an issue: ${e.message}"
            }

            val newMessages = _messages.value.toMutableList()
            newMessages.add("jarvis" to response)
            _messages.value = newMessages
            _agentState.value = AgentState.IDLE

            // Real TTS Voice Synthesis with Indian English/Hinglish pronunciation
            try {
                com.jarvis.app.JarvisApplication.ttsEngine.speak(response)
            } catch (_: Exception) {}
        }
    }

    fun toggleListening() {
        if (_isListening.value) {
            _isListening.value = false
            _agentState.value = AgentState.IDLE
            try {
                com.jarvis.app.JarvisApplication.speechRecognizerEngine.stopListening()
            } catch (_: Exception) {}
        } else {
            _isListening.value = true
            _agentState.value = AgentState.OBSERVING
            try {
                com.jarvis.app.JarvisApplication.speechRecognizerEngine.startListening(
                    onResult = { recognizedText ->
                        _isListening.value = false
                        _agentState.value = AgentState.IDLE
                        if (recognizedText.isNotBlank()) {
                            processCommand(recognizedText)
                        }
                    },
                    onError = {
                        _isListening.value = false
                        _agentState.value = AgentState.IDLE
                    }
                )
            } catch (_: Exception) {
                _isListening.value = false
                _agentState.value = AgentState.IDLE
            }
        }
    }

    fun emergencyStop() {
        EmergencyStop.activate()
        _agentState.value = AgentState.EMERGENCY_STOPPED
        _isListening.value = false
        val newMessages = _messages.value.toMutableList()
        newMessages.add("system" to "⚠️ EMERGENCY HALT ACTIVATED // All background tasks and accessibility gestures terminated.")
        _messages.value = newMessages
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val agentState by viewModel.agentState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    // Void Cyber Background gradient
    val bgGradient = Brush.verticalGradient(
        listOf(
            VoidBlack,
            DeepSpaceNavy,
            Color(0xFF040A14)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        // 1. Top HUD Telemetry Ribbon
        TelemetryHeader()

        // 2. Central Animated Arc Reactor Core
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ArcReactorWidget(
                    agentState = agentState,
                    isListening = isListening,
                    size = 155.dp,
                    onClick = { viewModel.toggleListening() }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tactical state badge below Arc Reactor
                Text(
                    text = when (agentState) {
                        AgentState.IDLE -> if (isListening) "VOICE RECOGNITION ACTIVE" else "STARK NEURAL CORE // STANDBY"
                        AgentState.PLANNING -> "CALCULATING TACTICAL PATH..."
                        AgentState.EXECUTING -> "EXECUTING AUTOMATED ACTIONS..."
                        AgentState.OBSERVING -> "OBSERVING SCREEN MATRIX..."
                        AgentState.EMERGENCY_STOPPED -> "EMERGENCY HALT ENGAGED"
                        AgentState.WAITING_CONFIRMATION -> "AWAITING OPERATOR CONFIRMATION"
                        else -> "SYSTEM NOMINAL"
                    },
                    color = when (agentState) {
                        AgentState.EMERGENCY_STOPPED -> CyberCrimson
                        AgentState.EXECUTING, AgentState.PLANNING -> StarkGold
                        else -> if (isListening) CyberGreen else ArcCyan
                    },
                    fontFamily = HudMonospace,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Divider(
            color = Color(0x3300F0FF),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
        )

        // 3. Dialogue Terminal (Message Stream + Quick Action Chips)
        Box(modifier = Modifier.weight(1f)) {
            ConversationPanel(
                messages = messages,
                onQuickActionClick = { viewModel.processCommand(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 4. Cyber Command Deck (Input bar + Arc Mic + Guarded Halt Switch)
        CommandInput(
            onTextSubmit = { viewModel.processCommand(it) },
            onMicClick = { viewModel.toggleListening() },
            onEmergencyStop = { viewModel.emergencyStop() },
            isListening = isListening
        )
    }
}
