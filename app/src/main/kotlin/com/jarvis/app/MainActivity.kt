package com.jarvis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jarvis.app.ui.screens.*
import com.jarvis.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            JarvisTheme {
                val navController = rememberNavController()
                val isSetupComplete = JarvisApplication.settingsRepository.settingsFlow.collectAsState(initial = JarvisApplication.settingsRepository.loadSettings()).value.isSetupComplete
                val startDestination = if (isSetupComplete) "home" else "welcome"
                
                Scaffold(
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
                                    icon = { Icon(Icons.Default.Memory, contentDescription = "Engrams") },
                                    label = { Text("ENGRAMS", fontFamily = HudMonospace, fontSize = 10.sp) },
                                    selected = currentRoute == "memory",
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ArcCyan,
                                        selectedTextColor = ArcCyan,
                                        indicatorColor = Color(0x3300F0FF),
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    onClick = {
                                        navController.navigate("memory") {
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
                            .padding(innerPadding)
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
                            val viewModel: HomeViewModel = viewModel()
                            HomeScreen(viewModel = viewModel)
                        }
                        composable("memory") {
                            val viewModel: MemoryViewModel = viewModel()
                            MemoryScreen(viewModel = viewModel)
                        }
                        composable("settings") {
                            val viewModel: SettingsViewModel = viewModel()
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
