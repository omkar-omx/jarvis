package com.jarvis.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jarvis.app.ui.screens.*
import com.jarvis.app.ui.theme.*

class MainActivity : ComponentActivity() {

    // ─── Permission Launcher ────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        // After runtime permissions are answered, check overlay permission
        checkAndRequestOverlay()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the app draw edge-to-edge (fixes title shifting down)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // Request runtime permissions on first launch
        requestEssentialPermissions()

        setContent {
            JarvisTheme {
                val navController = rememberNavController()
                val isSetupComplete = JarvisApplication.settingsRepository.settingsFlow
                    .collectAsState(initial = JarvisApplication.settingsRepository.loadSettings())
                    .value.isSetupComplete
                val startDestination = if (isSetupComplete) "home" else "welcome"

                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        if (currentRoute != "welcome") {
                            NavigationBar(
                                containerColor = VoidBlack,
                                modifier = Modifier.border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(listOf(Color(0x3300F0FF), Color.Transparent)),
                                    shape = androidx.compose.ui.graphics.RectangleShape
                                )
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Radio, contentDescription = "Command") },
                                    label = { Text("COMMAND", fontFamily = HudMonospace, fontSize = 10.sp) },
                                    selected = currentRoute == "home",
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ArcCyan,
                                        selectedTextColor = ArcCyan,
                                        indicatorColor = Color(0x3300F0FF),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    onClick = {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Tune, contentDescription = "Protocols") },
                                    label = { Text("PROTOCOLS", fontFamily = HudMonospace, fontSize = 10.sp) },
                                    selected = currentRoute == "settings",
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ArcCyan,
                                        selectedTextColor = ArcCyan,
                                        indicatorColor = Color(0x3300F0FF),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    onClick = {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .background(VoidBlack)
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                onSetupComplete = {
                                    navController.navigate("home") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            val homeViewModel: HomeViewModel = viewModel()
                            HomeScreen(viewModel = homeViewModel)
                        }
                        composable("memory") {
                            val memoryViewModel: MemoryViewModel = viewModel()
                            MemoryScreen(viewModel = memoryViewModel)
                        }
                        composable("settings") {
                            val settingsViewModel: SettingsViewModel = viewModel()
                            SettingsScreen(viewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val settings = JarvisApplication.settingsRepository.settingsFlow.value
        if (settings.isSetupComplete || settings.aiApiKey.isNotBlank()) {
            try {
                com.jarvis.app.voice.WakeWordService.start(this)
            } catch (_: Exception) {}
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Requests RECORD_AUDIO, CAMERA, and POST_NOTIFICATIONS at first launch.
     * Works exactly like Google Assistant — asks upfront on first open.
     */
    private fun requestEssentialPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // All permissions already granted — check overlay
            checkAndRequestOverlay()
        }
    }

    /**
     * After mic/camera/notification permissions are resolved, check SYSTEM_ALERT_WINDOW
     * (Overlay over other apps). Required for the floating JARVIS assistant bubble.
     */
    private fun checkAndRequestOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
