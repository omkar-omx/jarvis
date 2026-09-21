package com.jarvis.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.jarvis.app.JarvisApplication
import com.jarvis.app.accessibility.AccessibilityBridge
import com.jarvis.app.ui.theme.*

class SettingsViewModel : ViewModel() {
    var geminiKey by mutableStateOf(JarvisApplication.secureStorage.getApiKey("gemini") ?: "")
    var openAiKey by mutableStateOf(JarvisApplication.secureStorage.getApiKey("openai") ?: "")
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
        if (openAiKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(openAiKey.trim(), "openai")
        if (tavilyKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(tavilyKey.trim(), "tavily")
        if (weatherKey.isNotBlank()) JarvisApplication.secureStorage.saveApiKey(weatherKey.trim(), "weather")
        if (unlockPin.isNotBlank()) com.jarvis.app.auth.LockscreenUnlocker.saveUnlockPin(unlockPin.trim())

        // Determine preferred provider based on keys
        val chosenProvider = if (openAiKey.isNotBlank() && geminiKey.isBlank()) "OpenAI" else "Gemini"
        val chosenKey = if (chosenProvider == "OpenAI") openAiKey.trim() else geminiKey.trim()

        val currentSettings = JarvisApplication.settingsRepository.settingsFlow.value
        JarvisApplication.settingsRepository.saveSettings(
            currentSettings.copy(
                aiProviderName = chosenProvider,
                aiApiKey = chosenKey,
                openAiApiKey = openAiKey.trim(),
                isSetupComplete = true
            )
        )

        // Reconfigure BrainManager immediately
        if (chosenKey.isNotBlank()) {
            com.jarvis.app.brain.BrainManager.configureProvider(
                com.jarvis.app.brain.AIProviderConfig(
                    providerName = chosenProvider,
                    apiKey = chosenKey,
                    modelName = if (chosenProvider == "OpenAI") "gpt-4o-mini" else "gemini-1.5-flash"
                )
            )
        }
        isSavedToastVisible = true
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle

    // ── Live-refresh tick on every RESUME (user comes back from settings) ──
    var refreshTick by remember { mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Live permission states — re-evaluated whenever refreshTick changes
    val isAccessibilityOnline = remember(refreshTick) {
        if (AccessibilityBridge.isConnected.value) return@remember true
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            enabledServices.contains(context.packageName, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
    val hasOverlay = remember(refreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(context)
        else true
    }
    val hasMic = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val hasCamera = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val hasBatteryExemption = remember(refreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        else true
    }
    val hasNotificationListener = remember(refreshTick) {
        try {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
            flat.contains(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

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
                    text = "OMX PROTOCOLS // HARDWARE BUS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = HudMonospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Section 1: Hardware Permissions & Automation Hub
        OmxConfigSection(title = "PERMISSIONS & AUTOMATION BUS", icon = Icons.Default.SettingsSuggest) {
            OmxPermissionRow(
                title = "Accessibility Automation",
                subtitle = "Enables automatic UI clicks, swipes & PIN unlock",
                isGranted = isAccessibilityOnline,
                onActivate = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxPermissionRow(
                title = "Display Over Other Apps",
                subtitle = "Floating Google Assistant style HUD overlay",
                isGranted = hasOverlay,
                onActivate = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxPermissionRow(
                title = "Microphone & Speech",
                subtitle = "Real-time speech recognition & background wake word",
                isGranted = hasMic,
                onActivate = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxPermissionRow(
                title = "Camera & Flashlight",
                subtitle = "Enables camera launch & hardware flashlight toggle",
                isGranted = hasCamera,
                onActivate = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxPermissionRow(
                title = "Unrestricted Battery",
                subtitle = "Prevents OS from killing background \"Hey Jarvis\" service",
                isGranted = hasBatteryExemption,
                onActivate = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxPermissionRow(
                title = "Notification Listener",
                subtitle = "Allows JARVIS to read incoming message notifications",
                isGranted = hasNotificationListener,
                onActivate = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            )
        }

        // Section 2: AI & Neural Network Vault
        OmxConfigSection(title = "NEURAL NETWORK VAULT", icon = Icons.Default.Psychology) {
            OmxKeyField(
                label = "Google Gemini API Key (Core Brain)",
                value = viewModel.geminiKey,
                onValueChange = { viewModel.geminiKey = it },
                placeholder = "AIzaSy..."
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxKeyField(
                label = "OpenAI / ChatGPT API Key (Alternative Brain)",
                value = viewModel.openAiKey,
                onValueChange = { viewModel.openAiKey = it },
                placeholder = "sk-..."
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxKeyField(
                label = "Tavily Search API Key (Web Intel)",
                value = viewModel.tavilyKey,
                onValueChange = { viewModel.tavilyKey = it },
                placeholder = "tvly-..."
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxKeyField(
                label = "OpenWeather API Key (Environment)",
                value = viewModel.weatherKey,
                onValueChange = { viewModel.weatherKey = it },
                placeholder = "OpenWeather API Key"
            )
        }

        // Section 3: Speech & Vocal Synthesis
        OmxConfigSection(title = "SPEECH & VOCAL MATRIX", icon = Icons.Default.GraphicEq) {
            OmxToggleRow(
                title = "Speech Synthesis (TTS)",
                subtitle = "J.A.R.V.I.S. vocal audio response output in Hinglish",
                checked = viewModel.speechSynthesisEnabled,
                onCheckedChange = { viewModel.speechSynthesisEnabled = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxToggleRow(
                title = "Wake Word Detection",
                subtitle = "Listen for \"Hey Jarvis\" hotword in background",
                checked = viewModel.wakeWordEnabled,
                onCheckedChange = { viewModel.wakeWordEnabled = it }
            )
        }

        // Section 4: Security & Operational Protocols
        OmxConfigSection(title = "SECURITY & OPERATIONAL PROTOCOLS", icon = Icons.Default.Security) {
            OmxToggleRow(
                title = "Biometric Owner Authentication",
                subtitle = "Require fingerprint/face auth for sensitive actions",
                checked = viewModel.requireBiometricAuth,
                onCheckedChange = { viewModel.requireBiometricAuth = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OmxToggleRow(
                title = "Quiet Protocol",
                subtitle = "Mute vocal audio output during operations",
                checked = viewModel.quietMode,
                onCheckedChange = { viewModel.quietMode = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OmxKeyField(
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
                    text = "• BRAND IDENTITY: OmX Infinity (Created by Omkar)",
                    color = OmxGold,
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
fun OmxPermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onActivate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x4D060D1A))
            .border(1.dp, if (isGranted) Color(0x3300FF88) else Color(0x33FF1E44), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HudMonospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isGranted) Color(0x3300FF88) else Color(0x33FF1E44))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (isGranted) "ONLINE" else "REQUIRED",
                        color = if (isGranted) CyberGreen else CyberCrimson,
                        fontSize = 8.5.sp,
                        fontFamily = HudMonospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onActivate,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0x2600F0FF) else Color(0x4D00F0FF),
                contentColor = ArcCyan
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = if (isGranted) "VERIFY" else "ACTIVATE",
                fontSize = 9.sp,
                fontFamily = HudMonospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OmxConfigSection(
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
fun OmxKeyField(
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
fun OmxToggleRow(
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
