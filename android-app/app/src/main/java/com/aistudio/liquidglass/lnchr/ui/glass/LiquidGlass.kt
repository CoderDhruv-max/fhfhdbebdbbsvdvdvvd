package com.aistudio.liquidglass.lnchr.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object LiquidGlassDefaults {
    val GlassWhite = Color(0xF5F7FA)
    val GlassBorderHigh = Color(0x99FFFFFF)
    val GlassBorderLow = Color(0x2EFFFFFF)
    val GlassHighlight = Color(0x66FFFFFF)
    val GlassShadow = Color(0x400A1128)
}

/**
 * Reusable LiquidGlass container component implementing pristine translucent
 * surface gradients, specular perimeter borders, and animated caustics shimmer.
 * Children inside content() remain 100% sharp and unblurred.
 */
@Composable
fun LiquidGlass(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    opacity: Float = 0.70f,
    blurRadius: Dp = 18.dp, // Maintained for API compatibility
    elevation: Dp = 8.dp,
    borderWidth: Dp = 1.dp,
    tintColor: Color = Color.White,
    showPerimeterEffect: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    // Animated diagonal specular light shimmer sweep (creative liquid glass effect)
    val infiniteTransition = rememberInfiniteTransition(label = "glass_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val backgroundBrush = remember(opacity, tintColor) {
        val safeOpacity = opacity.coerceIn(0.20f, 0.90f)
        Brush.linearGradient(
            colors = listOf(
                tintColor.copy(alpha = (safeOpacity * 0.75f).coerceAtMost(0.85f)),
                Color(0xFFE2E8F0).copy(alpha = (safeOpacity * 0.65f)),
                Color(0xFFCBD5E1).copy(alpha = (safeOpacity * 0.55f))
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val borderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                LiquidGlassDefaults.GlassBorderHigh,
                Color(0x8038BDF8), // Cyan refraction touch
                LiquidGlassDefaults.GlassBorderLow,
                LiquidGlassDefaults.GlassBorderHigh
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = LiquidGlassDefaults.GlassShadow,
                spotColor = LiquidGlassDefaults.GlassShadow
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(
                border = BorderStroke(borderWidth, borderBrush),
                shape = shape
            )
    ) {
        // Specular top-left refraction gloss overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = 450f
                    )
                )
        )

        // Animated specular caustic sheen sweep
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = 0.60f
                }
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerOffset * 600f, 0f),
                        end = Offset((shimmerOffset + 0.6f) * 600f, 400f)
                    )
                )
        )

        // Crystal clear content (icons, text, buttons are NEVER blurred)
        content()

        if (showPerimeterEffect) {
            PerimeterHighlightAnimation(
                trigger = true,
                cornerRadius = cornerRadius
            )
        }
    }
}

/**
 * LiquidGlassSurface backwards-compatible alias that forwards to LiquidGlass container.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    opacity: Float = 0.70f,
    blurRadius: Dp = 18.dp,
    elevation: Dp = 8.dp,
    borderWidth: Dp = 1.dp,
    tintColor: Color = Color.White,
    showPerimeterEffect: Boolean = false,
    content: @Composable () -> Unit
) {
    LiquidGlass(
        modifier = modifier,
        cornerRadius = cornerRadius,
        opacity = opacity,
        blurRadius = blurRadius,
        elevation = elevation,
        borderWidth = borderWidth,
        tintColor = tintColor,
        showPerimeterEffect = showPerimeterEffect
    ) {
        content()
    }
}

/**
 * Floating horizontal Liquid Glass pill for recents / favorites.
 * Features a streamlined reduced height and an illuminated white border stroke
 * that animates with parabolic (slow-fast-slow) speed when a new recent app is detected.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    opacity: Float = 0.72f,
    strokeTrigger: Long = 0L,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            cornerRadius = cornerRadius,
            opacity = opacity,
            elevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        // White border stroke generated when a recent app is new (speed in parabola: slow-fast-slow)
        PillBorderStrokeAnimation(
            trigger = strokeTrigger,
            cornerRadius = cornerRadius,
            modifier = Modifier.matchParentSize()
        )
    }
}

/**
 * Floating bottom Liquid Glass dock.
 */
@Composable
fun LiquidGlassDock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    opacity: Float = 0.72f,
    content: @Composable RowScope.() -> Unit
) {
    LiquidGlassSurface(
        modifier = modifier,
        cornerRadius = cornerRadius,
        opacity = opacity,
        elevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Interactive liquid icon press animation:
 * Organic jelly squash & stretch physics (scaleX & scaleY deform like a liquid drop),
 * followed by a bouncy spring overshoot.
 */
@Composable
fun rememberIconPressState(
    onLaunch: () -> Unit
): Pair<Modifier, () -> Unit> {
    val scaleX = remember { Animatable(1.0f) }
    val scaleY = remember { Animatable(1.0f) }
    val brightness = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isTriggered by remember { mutableStateOf(false) }

    val trigger: () -> Unit = {
        if (!isTriggered) {
            isTriggered = true
            scope.launch {
                // Phase 1: Liquid squish compression
                launch { brightness.animateTo(0.24f, tween(60)) }
                launch { scaleX.animateTo(1.10f, tween(60, easing = FastOutSlowInEasing)) }
                scaleY.animateTo(0.88f, tween(60, easing = FastOutSlowInEasing))

                // Phase 2: Elastic liquid bounce release
                launch { brightness.animateTo(0f, tween(140)) }
                launch {
                    scaleX.animateTo(
                        1.0f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }
                scaleY.animateTo(
                    1.0f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )

                delay(15)
                isTriggered = false
                onLaunch()
            }
        }
    }

    val modifier = Modifier.graphicsLayer {
        this.scaleX = scaleX.value
        this.scaleY = scaleY.value
        alpha = (1f + brightness.value * 0.2f).coerceAtMost(1f)
    }

    return Pair(modifier, trigger)
}
