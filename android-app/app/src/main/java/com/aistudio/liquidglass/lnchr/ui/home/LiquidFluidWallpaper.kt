package com.aistudio.liquidglass.lnchr.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data class representing an interactive liquid ripple emitted on wallpaper touch.
 */
data class FluidRipple(
    val id: Long,
    val center: Offset,
    val startTime: Long
)

/**
 * Living Liquid Fluid Wallpaper Canvas:
 * - Fluid undulating aurora gradient waves
 * - Drifting ambient liquid glass orbs with refractive glow
 * - Interactive touch ripples: tapping the wallpaper radiates liquid droplet rings
 */
@Composable
fun LiquidFluidWallpaper(
    modifier: Modifier = Modifier,
    onTapWallpaper: ((Offset) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wallpaper_fluid")

    // Slow ambient wave phase animation (12 seconds loop)
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Secondary harmonic phase (8 seconds loop)
    val harmonicPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "harmonic_phase"
    )

    // Interactive ripples list
    val ripples = remember { mutableStateListOf<FluidRipple>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val now = System.currentTimeMillis()
                        // Keep max 5 active ripples
                        if (ripples.size >= 5) ripples.removeAt(0)
                        ripples.add(FluidRipple(id = now, center = offset, startTime = now))
                        onTapWallpaper?.invoke(offset)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val currentTime = System.currentTimeMillis()

            // 1. Deep liquid gradient base (Ethereal Twilight Aqua & Indigo)
            val baseGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0D1B2A), // Deep oceanic midnight
                    Color(0xFF1B263B), // Navy twilight
                    Color(0xFF243B55), // Ambient teal-slate
                    Color(0xFF141E30)  // Deep bottom shade
                )
            )
            drawRect(brush = baseGradient)

            // 2. Animated harmonic liquid glow orbs
            drawLiquidOrb(
                center = Offset(
                    x = width * 0.25f + sin(wavePhase) * 60f,
                    y = height * 0.28f + cos(wavePhase) * 45f
                ),
                radius = width * 0.55f,
                color = Color(0x3D38BDF8) // Luminous Sky Blue
            )

            drawLiquidOrb(
                center = Offset(
                    x = width * 0.78f + cos(harmonicPhase) * 70f,
                    y = height * 0.42f + sin(harmonicPhase) * 50f
                ),
                radius = width * 0.62f,
                color = Color(0x33818CF8) // Electric Indigo
            )

            drawLiquidOrb(
                center = Offset(
                    x = width * 0.45f + sin(wavePhase * 0.8f) * 80f,
                    y = height * 0.78f + cos(wavePhase * 0.8f) * 60f
                ),
                radius = width * 0.70f,
                color = Color(0x3806B6D4) // Radiant Cyan
            )

            drawLiquidOrb(
                center = Offset(
                    x = width * 0.15f + cos(harmonicPhase * 0.7f) * 50f,
                    y = height * 0.85f + sin(harmonicPhase * 0.7f) * 40f
                ),
                radius = width * 0.48f,
                color = Color(0x28A855F7) // Subtle Violet Glow
            )

            // 3. Interactive Liquid Ripples
            val iterator = ripples.iterator()
            while (iterator.hasNext()) {
                val ripple = iterator.next()
                val elapsed = currentTime - ripple.startTime
                val duration = 1200f // 1.2 seconds animation
                if (elapsed > duration) {
                    iterator.remove()
                } else {
                    val progress = (elapsed / duration).coerceIn(0f, 1f)
                    val easeOut = 1f - (1f - progress) * (1f - progress)
                    val radius = easeOut * (width * 0.45f)
                    val alpha = (1f - progress) * 0.45f

                    // Double concentric liquid wave
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = alpha),
                        radius = radius,
                        center = ripple.center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f * (1f - progress))
                    )
                    if (progress > 0.15f) {
                        val innerProgress = (progress - 0.15f) / 0.85f
                        val innerRadius = innerProgress * (width * 0.32f)
                        drawCircle(
                            color = Color(0xFFE0F2FE).copy(alpha = (1f - innerProgress) * 0.35f),
                            radius = innerRadius,
                            center = ripple.center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.0f * (1f - innerProgress))
                        )
                    }
                }
            }
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
