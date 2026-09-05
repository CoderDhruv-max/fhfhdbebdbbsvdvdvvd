package com.example.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Living Liquid Fluid Wallpaper Canvas:
 * - Minimal, elegant, calm ambient fluid gradients (Slate, Navy, Deep Indigo, Teal)
 * - Subtle parallax shift when swiping between Google Feed and Home pages
 * - Glassmorphism blur effect on background surfaces matching the Liquid Glass theme
 * - Completely non-blocking for gestures (no greedy pointer interception)
 */
@Composable
fun LiquidFluidWallpaper(
    modifier: Modifier = Modifier,
    parallaxOffset: Float = 0f,
    blurRadius: Dp = 0.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wallpaper_fluid")

    // Slow ambient wave phase animation (14 seconds loop)
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Secondary harmonic phase (9 seconds loop)
    val harmonicPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "harmonic_phase"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Base Ambient Fluid Canvas with dynamic frosted glassmorphism blur
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurRadius > 0.dp) {
                        Modifier.blur(
                            radius = blurRadius,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded
                        )
                    } else Modifier
                )
        ) {
            val width = size.width
            val height = size.height
            val shiftX = width * parallaxOffset * 0.15f

            // 1. Deep calm liquid gradient base (Ethereal Twilight Aqua & Midnight)
            val baseGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A), // Slate 900
                    Color(0xFF1E293B), // Slate 800
                    Color(0xFF1E1B4B), // Deep Indigo
                    Color(0xFF0F172A)  // Slate 900
                )
            )
            drawRect(brush = baseGradient)

            // 2. Animated harmonic ambient glow orbs with subtle parallax shift
            drawLiquidOrb(
                center = Offset(
                    x = width * 0.25f + sin(wavePhase) * 50f + shiftX,
                    y = height * 0.28f + cos(wavePhase) * 40f
                ),
                radius = width * 0.55f,
                color = Color(0x3338BDF8) // Soft sky cyan
            )

            drawLiquidOrb(
                center = Offset(
                    x = width * 0.78f + cos(harmonicPhase) * 60f + shiftX * 1.2f,
                    y = height * 0.42f + sin(harmonicPhase) * 45f
                ),
                radius = width * 0.60f,
                color = Color(0x2B818CF8) // Soft Indigo
            )

            drawLiquidOrb(
                center = Offset(
                    x = width * 0.50f + sin(wavePhase + 2f) * 40f + shiftX * 0.8f,
                    y = height * 0.75f + cos(wavePhase) * 35f
                ),
                radius = width * 0.50f,
                color = Color(0x242DD4BF) // Subtle Mint Teal
            )
        }
    }
}

private fun DrawScope.drawLiquidOrb(
    center: Offset,
    radius: Float,
    color: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color,
                color.copy(alpha = color.alpha * 0.45f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
