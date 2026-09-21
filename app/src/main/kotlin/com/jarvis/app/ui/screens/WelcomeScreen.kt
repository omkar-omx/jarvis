package com.jarvis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.JarvisApplication
import com.jarvis.app.ui.theme.*

// ─── Model options per provider ───────────────────────────────────────────────
private val GEMINI_MODELS = listOf(
    "gemini-2.5-flash-preview-05-20",
    "gemini-2.5-pro-preview-06-05",
    "gemini-1.5-flash",
    "gemini-1.5-pro",
    "gemini-1.0-pro"
)

private val OPENAI_MODELS = listOf(
    "gpt-4o-mini",
    "gpt-4o",
    "gpt-4-turbo",
    "gpt-3.5-turbo"
)

private val VOICE_OPTIONS = listOf(
    "en-IN (Hindi/English Female)",
    "en-IN (Hindi/English Male)",
    "en-US (American English)",
    "hi-IN (Hindi)",
    "en-GB (British)"
)

private val PROVIDER_OPTIONS = listOf("Google Gemini", "OpenAI / ChatGPT")

@Composable
fun WelcomeScreen(onSetupComplete: () -> Unit) {
    val scrollState = rememberScrollState()

    // ── User Identity Fields ──────────────────────────────────────────────────
    var userName by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var assistantName by remember { mutableStateOf("Jarvis") }

    // ── Provider Selection ────────────────────────────────────────────────────
    var selectedProvider by remember { mutableStateOf("Google Gemini") }
    var selectedModel by remember { mutableStateOf(GEMINI_MODELS[0]) }
    var selectedVoice by remember { mutableStateOf(VOICE_OPTIONS[0]) }
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    // ── Dropdowns open state ──────────────────────────────────────────────────
    var providerDropdownOpen by remember { mutableStateOf(false) }
    var modelDropdownOpen by remember { mutableStateOf(false) }
    var voiceDropdownOpen by remember { mutableStateOf(false) }

    // ── Validation ────────────────────────────────────────────────────────────
    var showError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    // When provider changes, reset model to first option
    val modelOptions = if (selectedProvider == "Google Gemini") GEMINI_MODELS else OPENAI_MODELS
    LaunchedEffect(selectedProvider) {
        selectedModel = modelOptions[0]
    }

    val bgGradient = Brush.verticalGradient(
        listOf(Color(0xFF060B14), Color(0xFF09182E), Color(0xFF040A14))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Header ────────────────────────────────────────────────────────────
        Text(
            text = "J.A.R.V.I.S.",
            color = ArcCyan,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = HudMonospace,
            letterSpacing = 3.sp
        )
        Text(
            text = "SYSTEM CONFIGURATION INTERFACE // V2.0",
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = HudMonospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(18.dp))
        Divider(color = Color(0x3300F0FF), thickness = 1.dp)
        Spacer(modifier = Modifier.height(18.dp))

        // ── Identity Card ─────────────────────────────────────────────────────
        SetupSectionCard {
            // IDENTITY_PROFILE badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x2200F0FF))
                        .border(1.dp, Color(0x5500F0FF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "IDENTITY_PROFILE",
                        color = ArcCyan,
                        fontSize = 8.sp,
                        fontFamily = HudMonospace,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Username
            SetupFieldLabel("USER NAME")
            Spacer(modifier = Modifier.height(4.dp))
            SetupTextField(
                value = userName,
                onValueChange = { userName = it },
                placeholder = "your_username",
                keyboardType = KeyboardType.Ascii
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Full Name (required *)
            SetupFieldLabel("FULL NAME", required = true)
            Spacer(modifier = Modifier.height(4.dp))
            SetupTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "Your Full Name",
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Assistant Name
            SetupFieldLabel("ASSISTANT NAME")
            Spacer(modifier = Modifier.height(4.dp))
            SetupTextField(
                value = assistantName,
                onValueChange = { assistantName = it },
                placeholder = "Jarvis",
                keyboardType = KeyboardType.Text
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Model & Voice Card ────────────────────────────────────────────────
        SetupSectionCard {
            // NEURAL_CORE badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x2200F0FF))
                        .border(1.dp, Color(0x5500F0FF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "NEURAL_CORE",
                        color = ArcCyan,
                        fontSize = 8.sp,
                        fontFamily = HudMonospace,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // SELECT MODEL + MODEL NAME in one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Provider picker
                Column(modifier = Modifier.weight(1f)) {
                    SetupFieldLabel("SELECT MODEL", required = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        SetupDropdownButton(
                            value = selectedProvider,
                            onClick = { providerDropdownOpen = true }
                        )
                        DropdownMenu(
                            expanded = providerDropdownOpen,
                            onDismissRequest = { providerDropdownOpen = false },
                            modifier = Modifier.background(Color(0xFF0D1E38))
                        ) {
                            PROVIDER_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option,
                                            color = if (option == selectedProvider) ArcCyan else TextPrimary,
                                            fontFamily = HudMonospace,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        selectedProvider = option
                                        providerDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Model name picker
                Column(modifier = Modifier.weight(1f)) {
                    SetupFieldLabel("MODEL NAME", required = true)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        SetupDropdownButton(
                            value = selectedModel,
                            onClick = { modelDropdownOpen = true }
                        )
                        DropdownMenu(
                            expanded = modelDropdownOpen,
                            onDismissRequest = { modelDropdownOpen = false },
                            modifier = Modifier.background(Color(0xFF0D1E38))
                        ) {
                            modelOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option,
                                            color = if (option == selectedModel) ArcCyan else TextPrimary,
                                            fontFamily = HudMonospace,
                                            fontSize = 11.sp
                                        )
                                    },
                                    onClick = {
                                        selectedModel = option
                                        modelDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SELECT VOICE
            SetupFieldLabel("SELECT VOICE", required = true)
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                SetupDropdownButton(
                    value = selectedVoice,
                    onClick = { voiceDropdownOpen = true }
                )
                DropdownMenu(
                    expanded = voiceDropdownOpen,
                    onDismissRequest = { voiceDropdownOpen = false },
                    modifier = Modifier.background(Color(0xFF0D1E38))
                ) {
                    VOICE_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option,
                                    color = if (option == selectedVoice) ArcCyan else TextPrimary,
                                    fontFamily = HudMonospace,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                selectedVoice = option
                                voiceDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MODEL API key
            SetupFieldLabel("MODEL API", required = true)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (selectedProvider == "Google Gemini") "AIzaSy..." else "sk-...",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = HudMonospace
                    )
                },
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(
                        onClick = { showApiKey = !showApiKey },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            if (showApiKey) "HIDE" else "SHOW",
                            color = ArcCyan,
                            fontFamily = HudMonospace,
                            fontSize = 9.sp
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcCyan,
                    unfocusedBorderColor = Color(0x3300F0FF),
                    focusedContainerColor = Color(0x33060D1A),
                    unfocusedContainerColor = Color(0x33060D1A),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Help text under API key
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (selectedProvider == "Google Gemini")
                    "Get free key: aistudio.google.com → API Keys"
                else
                    "Get key: platform.openai.com → API Keys",
                color = TextMuted,
                fontSize = 9.5.sp,
                fontFamily = HudMonospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Optional APIs Card ────────────────────────────────────────────────
        SetupSectionCard {
            Text(
                "OPTIONAL INTEL SOURCES",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = HudMonospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "These unlock web search & live weather. You can add them later in Protocols.",
                color = TextMuted,
                fontSize = 10.5.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            var tavilyKey by remember { mutableStateOf("") }
            var weatherKey by remember { mutableStateOf("") }

            SetupFieldLabel("TAVILY SEARCH API (Web Intel)")
            Spacer(modifier = Modifier.height(4.dp))
            SetupTextField(
                value = tavilyKey,
                onValueChange = { tavilyKey = it },
                placeholder = "tvly-... (free at tavily.com)",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            SetupFieldLabel("OPENWEATHER API (Environment)")
            Spacer(modifier = Modifier.height(4.dp))
            SetupTextField(
                value = weatherKey,
                onValueChange = { weatherKey = it },
                placeholder = "openweathermap.org API key",
                isPassword = true
            )

            // Store these in parent scope via state hoisting workaround
            LaunchedEffect(tavilyKey) {
                if (tavilyKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(tavilyKey.trim(), "tavily")
            }
            LaunchedEffect(weatherKey) {
                if (weatherKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(weatherKey.trim(), "weather")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Error Banner ──────────────────────────────────────────────────────
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
                Icon(Icons.Default.Warning, contentDescription = null, tint = CyberCrimson, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(errorMsg, color = CyberCrimson, fontSize = 11.sp, fontFamily = HudMonospace)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ── Initialize Button ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HoloCyanGradient)
                .clickable {
                    // Validate
                    when {
                        fullName.isBlank() -> {
                            errorMsg = "Full Name is required to personalize JARVIS."
                            showError = true
                        }
                        apiKey.isBlank() -> {
                            errorMsg = "API Key is required. Get a free Gemini key from aistudio.google.com"
                            showError = true
                        }
                        else -> {
                            showError = false
                            initializeJarvis(
                                userName = userName.trim(),
                                fullName = fullName.trim(),
                                assistantName = assistantName.trim().ifBlank { "Jarvis" },
                                provider = selectedProvider,
                                model = selectedModel,
                                voice = selectedVoice,
                                apiKey = apiKey.trim(),
                                onSetupComplete = onSetupComplete
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "INITIALIZE J.A.R.V.I.S. PROTOCOLS",
                    color = Color.Black,
                    fontFamily = HudMonospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─── Setup Logic ──────────────────────────────────────────────────────────────

private fun initializeJarvis(
    userName: String,
    fullName: String,
    assistantName: String,
    provider: String,
    model: String,
    voice: String,
    apiKey: String,
    onSetupComplete: () -> Unit
) {
    // Determine internal provider name
    val providerName = if (provider == "Google Gemini") "Gemini" else "OpenAI"

    // Save API key to SecureStorage
    val keyType = if (providerName == "Gemini") "gemini" else "openai"
    JarvisApplication.secureStorage.saveApiKey(apiKey, keyType)
    // Also save under 'gemini' slot if Gemini (for legacy compatibility)
    if (providerName == "Gemini") {
        JarvisApplication.secureStorage.saveApiKey(apiKey, "gemini")
    }

    // Save settings
    val currentSettings = JarvisApplication.settingsRepository.settingsFlow.value
    JarvisApplication.settingsRepository.saveSettings(
        currentSettings.copy(
            isSetupComplete = true,
            userName = userName,
            fullName = fullName,
            assistantName = assistantName,
            aiProviderName = providerName,
            aiApiKey = apiKey,
            aiModelName = model,
            selectedVoice = voice
        )
    )

    // Immediately configure AI core — no restart needed
    com.jarvis.app.brain.BrainManager.configureProvider(
        com.jarvis.app.brain.AIProviderConfig(
            providerName = providerName,
            apiKey = apiKey,
            modelName = model
        )
    )

    // Start Hey JARVIS wake-word listener
    try {
        com.jarvis.app.voice.WakeWordService.start(JarvisApplication.instance)
    } catch (_: Exception) {}

    onSetupComplete()
}

// ─── Reusable Composables ─────────────────────────────────────────────────────

@Composable
private fun SetupSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xBB09182E))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0x7700F0FF), Color(0x2200F0FF))),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SetupFieldLabel(text: String, required: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 10.5.sp,
            fontFamily = HudMonospace,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
        if (required) {
            Text(" *", color = CyberCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SetupTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 12.sp, fontFamily = HudMonospace) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ArcCyan,
            unfocusedBorderColor = Color(0x3300F0FF),
            focusedContainerColor = Color(0x33060D1A),
            unfocusedContainerColor = Color(0x33060D1A),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun SetupDropdownButton(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33060D1A))
            .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = HudMonospace,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = ArcCyan,
            modifier = Modifier.size(20.dp)
        )
    }
}
