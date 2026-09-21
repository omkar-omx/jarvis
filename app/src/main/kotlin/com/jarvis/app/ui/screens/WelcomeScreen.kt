package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.JarvisApplication
import com.jarvis.app.ui.theme.*

@Composable
fun WelcomeScreen(onSetupComplete: () -> Unit) {
    val scrollState = rememberScrollState()

    var geminiKey by remember { mutableStateOf("") }
    var tavilyKey by remember { mutableStateOf("") }
    var weatherKey by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    val bgGradient = Brush.verticalGradient(
        listOf(VoidBlack, DeepSpaceNavy, Color(0xFF040A14))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Futuristic Brand Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300F0FF))
                    .border(1.5.dp, ArcCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ArcCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "J.A.R.V.I.S.",
                    color = ArcCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = HudMonospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "STARK INDUSTRIES // NEURAL INITIALIZATION",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = HudMonospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Text(
            text = "To empower J.A.R.V.I.S. with real-time intelligence, connect the neural core to the following free cloud services.",
            color = TextPrimary,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )

        Divider(color = Color(0x3300F0FF), thickness = 1.dp)

        // 1. Gemini
        SetupCard(
            title = "1. THE NEURAL BRAIN // GOOGLE GEMINI",
            description = "Visit aistudio.google.com and generate a free Gemini API Key for deep reasoning and screen analysis.",
            value = geminiKey,
            onValueChange = { geminiKey = it },
            placeholder = "AIzaSy..."
        )

        // 2. Tavily
        SetupCard(
            title = "2. REAL-TIME WEB INTEL // TAVILY SEARCH",
            description = "Visit tavily.com and create a free developer account to unlock live web search capabilities.",
            value = tavilyKey,
            onValueChange = { tavilyKey = it },
            placeholder = "tvly-..."
        )

        // 3. OpenWeather
        SetupCard(
            title = "3. TACTICAL ENVIRONMENT // OPENWEATHER",
            description = "Visit openweathermap.org for atmospheric awareness and real-time weather context.",
            value = weatherKey,
            onValueChange = { weatherKey = it },
            placeholder = "OpenWeather API Key"
        )

        if (showError) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FF1E44))
                    .border(1.dp, CyberCrimson, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Error", tint = CyberCrimson, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Please provide at least the Gemini API Key to activate the core.",
                    color = CyberCrimson,
                    fontSize = 11.5.sp,
                    fontFamily = HudMonospace
                )
            }
        }

        // Initialize Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HoloCyanGradient)
                .clickable {
                    if (geminiKey.isNotBlank()) {
                        JarvisApplication.secureStorage.saveApiKey(geminiKey.trim(), "gemini")
                        if (tavilyKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(tavilyKey.trim(), "tavily")
                        if (weatherKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(weatherKey.trim(), "weather")

                        val currentSettings = JarvisApplication.settingsRepository.settingsFlow.value
                        JarvisApplication.settingsRepository.saveSettings(
                            currentSettings.copy(
                                isSetupComplete = true,
                                aiProviderName = "Gemini",
                                aiApiKey = geminiKey.trim()
                            )
                        )

                        onSetupComplete()
                    } else {
                        showError = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Initialize", tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "INITIALIZE J.A.R.V.I.S. PROTOCOLS",
                    color = Color.Black,
                    fontFamily = HudMonospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SetupCard(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
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
            Text(
                text = title,
                color = ArcCyan,
                fontSize = 11.5.sp,
                fontFamily = HudMonospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = TextMuted, fontSize = 12.sp, fontFamily = HudMonospace) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
}
