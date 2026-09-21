package com.jarvis.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// J.A.R.V.I.S. STARK INDUSTRIES HUD PALETTE
// ==========================================

// Deep Void Backgrounds
val VoidBlack = Color(0xFF030712)
val DeepSpaceNavy = Color(0xFF070D1B)
val GlassSurface = Color(0xCC091224)
val GlassSurfaceVariant = Color(0x99101E38)
val GlassCard = Color(0xE60D1B33)

// Holographic Neon Accents
val ArcCyan = Color(0xFF00F0FF)
val ArcCyanGlow = Color(0x6600F0FF)
val ArcCyanSubtle = Color(0x2600F0FF)
val ElectricBlue = Color(0xFF0077FE)
val NeonCyanBright = Color(0xFF5DF2FF)

// Stark Amber / Core Warmth
val StarkGold = Color(0xFFFFB703)
val CoreAmber = Color(0xFFFB8500)
val CoreOrangeGlow = Color(0x66FB8500)

// System Alerts & States
val CyberGreen = Color(0xFF00FF88)
val CyberGreenGlow = Color(0x6600FF88)
val CyberCrimson = Color(0xFFFF1E44)
val CyberCrimsonGlow = Color(0x66FF1E44)

// Text & Monospace Telemetry
val TextPrimary = Color(0xFFE6F1FF)
val TextSecondary = Color(0xFF88A2C0)
val TextMuted = Color(0xFF4C6688)
val TextHolo = Color(0xFF80E5FF)

// Gradients
val HoloCyanGradient = Brush.horizontalGradient(
    listOf(ArcCyan, ElectricBlue)
)

val ArcReactorCoreGradient = Brush.radialGradient(
    listOf(Color.White, ArcCyan, ElectricBlue, Color.Transparent)
)

val EmergencyHazardGradient = Brush.horizontalGradient(
    listOf(CyberCrimson, Color(0xFF990011))
)

val GlassBorderGradient = Brush.linearGradient(
    listOf(Color(0x8000F0FF), Color(0x1A0077FE), Color(0x4D00F0FF))
)

// Legacy Aliases for backwards compatibility
val JarvisCyan = ArcCyan
val JarvisAccent = NeonCyanBright
val JarvisBlue = ElectricBlue
val JarvisDarkBackground = VoidBlack
val JarvisSurface = GlassSurface
val JarvisSurfaceVariant = GlassSurfaceVariant
val JarvisOnSurface = TextPrimary
val JarvisOnSurfaceVariant = TextSecondary
val JarvisSuccess = CyberGreen
val JarvisWarning = StarkGold
val JarvisError = CyberCrimson
val JarvisOnPrimary = Color.Black
val JarvisPrimaryContainer = Color(0xFF00384D)
val JarvisOnPrimaryContainer = TextHolo
