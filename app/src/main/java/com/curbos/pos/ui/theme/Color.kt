package com.curbos.pos.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// High Contrast Colors
val ElectricLime = Color(0xFFCCFF00)
val SafetyOrange = Color(0xFFFF5F00)
val DarkBackground = Color(0xFF121212)
val SurfaceColor = Color(0xFF1E1E1E)
val CyanNeon = Color(0xFF00FFFF)

// Gradients
val ElectricGradient = Brush.horizontalGradient(
    colors = listOf(ElectricLime, CyanNeon)
)

val DarkSurfaceGradient = Brush.verticalGradient(
    colors = listOf(SurfaceColor, Color.Black)
)
