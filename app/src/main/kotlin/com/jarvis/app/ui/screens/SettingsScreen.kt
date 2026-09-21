package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.jarvis.app.JarvisApplication
import com.jarvis.app.ui.theme.*

class SettingsViewModel : ViewModel() {
    var geminiKey by mutableStateOf(JarvisApplication.secureStorage.getApiKey("gemini") ?: "")
    var tavilyKey by mutableStateOf(JarvisApplication.secureStorage.getApiKey("tavily") ?: "")
    var weatherKey by mutableStateOf(JarvisApplication.secureStorage.getApiKey("weather") ?: "")

    var voiceLanguage by mutableStateOf("English (en-IN)")
    var speechSynthesisEnabled by mutableStateOf(true)
    var wakeWordEnabled by mutableStateOf(true)

    var quietMode by mutableStateOf(false)
    var requireBiometricAuth by mutableStateOf(true)
    var unlockPin by mutableStateOf(com.jarvis.app.auth.LockscreenUnlocker.getUnlockPin() ?: "")

    var isSavedToastVisible by mutableStateOf(false)

    fun saveAll() {
        if (geminiKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(geminiKey.trim(), "gemini")
        if (tavilyKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(tavilyKey.trim(), "tavily")
        if (weatherKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(weatherKey.trim(), "weather")
        if (unlockPin.isNotBlank()) com.jarvis.app.auth.LockscreenUnlocker.saveUnlockPin(unlockPin.trim())

        // Reconfigure active provider
        val currentSettings = JarvisApplication.settingsRepository.settingsFlow.value
        JarvisApplication.settingsRepository.saveSettings(
            currentSettings.copy(
                aiProviderName = "Gemini",
                aiApiKey = geminiKey.trim(),
                isSetupComplete = true
            )
        )
        isSavedToastVisible = true
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val scrollState = rememberScrollState()

    val bgGradient = Brush.verticalGradient(
        listOf(VoidBlack, DeepSpaceNavy, Color(0xFF040A14))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
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
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = ArcCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SYSTEM CONFIGURATION",
                    color = ArcCyan,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = HudMonospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "STARK PROTOCOLS // HARDWARE BUS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = HudMonospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Section 1: AI & Neural Network Vault
        StarkConfigSection(title = "NEURAL NETWORK VAULT", icon = Icons.Default.Psychology) {
            StarkKeyField(
                label = "Google Gemini API Key (Core Brain)",
                value = viewModel.geminiKey,
                onValueChange = { viewModel.geminiKey = it },
                placeholder = "AIzaSy..."
            )
            Spacer(modifier = Modifier.height(8.dp))
            StarkKeyField(
                label = "Tavily Search API Key (Web Intel)",
                value = viewModel.tavilyKey,
                onValueChange = { viewModel.tavilyKey = it },
                placeholder = "tvly-..."
            )
            Spacer(modifier = Modifier.height(8.dp))
            StarkKeyField(
                label = "OpenWeather API Key (Environment)",
                value = viewModel.weatherKey,
                onValueChange = { viewModel.weatherKey = it },
                placeholder = "OpenWeather API Key"
            )
        }

        // Section 2: Speech & Vocal Synthesis
        StarkConfigSection(title = "SPEECH & VOCAL MATRIX", icon = Icons.Default.GraphicEq) {
            StarkToggleRow(
                title = "Speech Synthesis (TTS)",
                subtitle = "J.A.R.V.I.S. vocal audio response output",
                checked = viewModel.speechSynthesisEnabled,
                onCheckedChange = { viewModel.speechSynthesisEnabled = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            StarkToggleRow(
                title = "Wake Word Detection",
                subtitle = "Listen for \"Hey Jarvis\" hotword in background",
                checked = viewModel.wakeWordEnabled,
                onCheckedChange = { viewModel.wakeWordEnabled = it }
            )
        }

        // Section 3: Security & Operational Protocols
        StarkConfigSection(title = "SECURITY & OPERATIONAL PROTOCOLS", icon = Icons.Default.Security) {
            StarkToggleRow(
                title = "Biometric Owner Authentication",
                subtitle = "Require fingerprint/face auth for sensitive actions",
                checked = viewModel.requireBiometricAuth,
                onCheckedChange = { viewModel.requireBiometricAuth = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            StarkToggleRow(
                title = "Quiet Protocol",
                subtitle = "Mute vocal audio output during operations",
                checked = viewModel.quietMode,
                onCheckedChange = { viewModel.quietMode = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            StarkKeyField(
                label = "Autonomous Unlock PIN / Password",
                value = viewModel.unlockPin,
                onValueChange = { viewModel.unlockPin = it },
                placeholder = "Enter device PIN (e.g. 1234)"
            )
        }

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HoloCyanGradient)
                .clickable { viewModel.saveAll() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE & SYNCHRONIZE PROTOCOLS",
                    color = Color.Black,
                    fontFamily = HudMonospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Telemetry Diagnostics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x66060E1C))
                .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "SYSTEM DIAGNOSTICS // HOST INFO",
                    color = ArcCyan,
                    fontSize = 10.sp,
                    fontFamily = HudMonospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• ARCHITECTURE: Kotlin Compose Multiplatform (Native Android)",
                    color = TextSecondary,
                    fontSize = 9.5.sp,
                    fontFamily = HudMonospace
                )
                Text(
                    text = "• MEMORY ENGINE: SQLite Room Database v1 (5 Entities)",
                    color = TextSecondary,
                    fontSize = 9.5.sp,
                    fontFamily = HudMonospace
                )
                Text(
                    text = "• ENCRYPTION: Android Keystore Hardware AES-256 GCM",
                    color = CyberGreen,
                    fontSize = 9.5.sp,
                    fontFamily = HudMonospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StarkConfigSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ArcCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = ArcCyan,
                    fontSize = 11.5.sp,
                    fontFamily = HudMonospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun StarkKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.5.sp,
            fontFamily = HudMonospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextMuted, fontSize = 12.sp, fontFamily = HudMonospace) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ArcCyan,
                unfocusedBorderColor = Color(0x3300F0FF),
                focusedContainerColor = Color(0x66060D1A),
                unfocusedContainerColor = Color(0x66060D1A),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun StarkToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ArcCyan,
                checkedTrackColor = Color(0x4D00F0FF),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0x33101E38)
            )
        )
    }
}
