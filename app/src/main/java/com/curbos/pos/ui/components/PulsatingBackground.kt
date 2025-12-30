package com.curbos.pos.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import com.curbos.pos.ui.theme.ElectricLime
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class TriangleData(
    val xRatio: Float,
    val yRatio: Float,
    val size: Float,
    val rotationSpeed: Float,
    val phase: Float
)

@Composable
fun PulsatingBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Generate deterministic random triangles
    val triangles = remember {
        val rng = Random(1234) // Fixed seed for consistency
        List(50) {
            TriangleData(
                xRatio = rng.nextFloat(),
                yRatio = rng.nextFloat(),
                size = 20f + rng.nextFloat() * 40f, // 20-60px to match web
                rotationSpeed = 0.1f + rng.nextFloat() * 0.4f, // Slower rotation
                phase = rng.nextFloat() * 2 * Math.PI.toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Dark background matching web
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // --- 1. Draw Watermark (Wireframe Taco) ---
            // Draw mostly in the center-right or center

            val iconX = width * 0.85f
            val iconY = height * 0.85f
            
            // Taco Path Construction
            val tacoPath = Path().apply {
                // Shell (Semi-circle ish)
                moveTo(-100f, 0f)
                cubicTo(-100f, -80f, -60f, -120f, 0f, -120f)
                cubicTo(60f, -120f, 100f, -80f, 100f, 0f)
                // Bottom flat/curve
                cubicTo(80f, 20f, -80f, 20f, -100f, 0f)
                
                // Filling details (zigzag)
                moveTo(-80f, -20f)
                lineTo(-60f, -50f)
                lineTo(-40f, -20f)
                lineTo(-20f, -50f)
                lineTo(0f, -20f)
                lineTo(20f, -50f)
                lineTo(40f, -20f)
                lineTo(60f, -50f)
                lineTo(80f, -20f)
            }
            
            withTransform({
                translate(left = iconX, top = iconY)
                rotate(degrees = -15f + sin(time * 0.5f) * 5f) // Gentle wobble
                scale(scaleX = 2.5f, scaleY = 2.5f)
            }) {
                drawPath(
                    path = tacoPath,
                    color = ElectricLime.copy(alpha = 0.05f),
                    style = Stroke(width = 4f)
                )
            }

            // --- 2. Draw Triangles ---
            triangles.forEach { triangle ->
                // Animate properties
                val currentRotation = (time * triangle.rotationSpeed + triangle.phase)
                
                // Gentle float Up/Down
                val offsetY = sin(time + triangle.phase) * 30f 
                
                // Opacity pulse
                val pulse = (sin(time * 0.5f + triangle.phase) + 1f) / 2f 
                val opacity = 0.05f + pulse * 0.15f // 0.05 .. 0.20 (Subtle)

                val centerX = triangle.xRatio * width
                val centerY = (triangle.yRatio * height) + offsetY
                
                // Wrap around effect simulation (simple)
                // If we really wanted to simulate the JS 'y -= speed' we'd need a running 'accumulatedY' state, 
                // but for a background, the sine wave float is often sufficient and smoother.
                // Let's stick to the Sine Float from previous implementation as it's cleaner in Compose 
                // without managing frame-by-frame state updates manually.

                val path = Path().apply {
                    val r = triangle.size / 2
                    for (i in 0..2) {
                        val angle = currentRotation + i * (2 * Math.PI / 3)
                        val x = centerX + r * cos(angle).toFloat()
                        val y = centerY + r * sin(angle).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }

                drawPath(
                    path = path,
                    color = ElectricLime.copy(alpha = opacity),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Foreground content container
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
