package com.curbos.pos.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// High Contrast Colors - Core Brand Palette
val ElectricLime = Color(0xFFCCFF00)
val SafetyOrange = Color(0xFFFF5F00)
val DarkBackground = Color(0xFF121212)
val SurfaceColor = Color(0xFF1E1E1E)
val CyanNeon = Color(0xFF00FFFF)

// Semantic Color Tokens - Use these instead of hardcoded values
val SurfaceElevated = Color(0xFF222222)     // Elevated cards, secondary surfaces
val SecondaryText = Color(0xFF9E9E9E)       // Subtle text, labels (replaces Color.Gray)
val ErrorRed = Color(0xFFFF6B6B)            // Danger zones, error states
val DividerColor = Color(0xFF333333)        // Dividers, subtle borders
val DisabledColor = Color(0xFF666666)       // Disabled states

// Gradients
val ElectricGradient = Brush.horizontalGradient(
    colors = listOf(ElectricLime, CyanNeon)
)

val DarkSurfaceGradient = Brush.verticalGradient(
    colors = listOf(SurfaceColor, Color.Black)
)
