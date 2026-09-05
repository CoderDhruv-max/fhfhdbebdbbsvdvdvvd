package com.example.ui.glass

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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
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
    val GlassWhite = Color(0xF8FAFC)
    val GlassBorderHigh = Color(0x99FFFFFF)
    val GlassBorderLow = Color(0x33FFFFFF)
    val GlassHighlight = Color(0x55FFFFFF)
    val GlassShadow = Color(0x2B0F172A)
}

/**
 * Reusable Minimal Glassmorphism container component.
 * Features:
 * - Semi-transparent milky surface (60-75% perceived opacity)
 * - Subtle vertical light gradient (frosted glass feel)
 * - Very subtle crisp border (high-light at top, low-light at bottom)
 * - Soft ambient depth shadow
 * - Subtle inner specular highlight at the top edge
 * - Rounded geometry
 * - Children inside content() remain 100% sharp and unblurred.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    opacity: Float = 0.70f,
    blurRadius: Dp = 18.dp,
    elevation: Dp = 6.dp,
    borderWidth: Dp = 1.dp,
    tintColor: Color = Color.White,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val safeOpacity = opacity.coerceIn(0.55f, 0.85f)

    // Milky translucent glass gradient (calm, elegant, minimal)
    val backgroundBrush = remember(safeOpacity, tintColor) {
        Brush.verticalGradient(
            colors = listOf(
                tintColor.copy(alpha = (safeOpacity * 0.92f).coerceAtMost(0.85f)),
                Color(0xFFF1F5F9).copy(alpha = (safeOpacity * 0.78f).coerceAtMost(0.75f)),
                Color(0xFFE2E8F0).copy(alpha = (safeOpacity * 0.68f).coerceAtMost(0.65f))
            )
        )
    }

    // Subtle natural directional lighting border
    val borderBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                LiquidGlassDefaults.GlassBorderHigh,
                Color.White.copy(alpha = 0.45f),
                LiquidGlassDefaults.GlassBorderLow
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
            .border(
                border = BorderStroke(borderWidth, borderBrush),
                shape = shape
            )
    ) {
        // Frosted Glass Blur layer matching the "Liquid Glass" theme
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                .background(backgroundBrush)
        )

        // Subtle top specular highlight glint for milky glass depth
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 120f
                    )
                )
        )

        // Crystal clear content
        content()
    }
}

/**
 * Reusable LiquidGlass container component implementing minimal glassmorphism.
 * Maintained for backwards-compatibility across the app.
 */
@Composable
fun LiquidGlass(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    opacity: Float = 0.70f,
    blurRadius: Dp = 18.dp, // Maintained for API compatibility
    elevation: Dp = 6.dp,
    borderWidth: Dp = 1.dp,
    tintColor: Color = Color.White,
    showPerimeterEffect: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        cornerRadius = cornerRadius,
        opacity = opacity,
        blurRadius = blurRadius,
        elevation = elevation,
        borderWidth = borderWidth,
        tintColor = tintColor
    ) {
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
 * LiquidGlassSurface backwards-compatible alias that forwards to GlassSurface container.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    opacity: Float = 0.70f,
    blurRadius: Dp = 18.dp,
    elevation: Dp = 6.dp,
    borderWidth: Dp = 1.dp,
    tintColor: Color = Color.White,
    showPerimeterEffect: Boolean = false,
    content: @Composable () -> Unit
) {
    GlassSurface(
        modifier = modifier,
        cornerRadius = cornerRadius,
        opacity = opacity,
        blurRadius = blurRadius,
        elevation = elevation,
        borderWidth = borderWidth,
        tintColor = tintColor
    ) {
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
 * Floating horizontal Liquid Glass pill for recents / favorites.
 * Features a streamlined reduced height and an illuminated white border stroke
 * that animates with parabolic (slow-fast-slow) speed when a new recent app is detected.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    opacity: Float = 0.70f,
    strokeTrigger: Long = 0L,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            cornerRadius = cornerRadius,
            opacity = opacity,
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 2.dp),
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
    cornerRadius: Dp = 30.dp,
    opacity: Float = 0.70f,
    content: @Composable RowScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        cornerRadius = cornerRadius,
        opacity = opacity,
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Interactive icon press animation:
 * Curve: 1.0 -> 0.93 -> 1.02 -> 1.0 with subtle brightness response (180-240ms).
 */
@Composable
fun rememberIconPressState(
    onLaunch: () -> Unit
): Pair<Modifier, () -> Unit> {
    val scale = remember { Animatable(1.0f) }
    val brightness = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isTriggered by remember { mutableStateOf(false) }

    val trigger: () -> Unit = {
        if (!isTriggered) {
            isTriggered = true
            scope.launch {
                // Step 1: 1.0 -> 0.93 with subtle highlight response (~70ms)
                launch { brightness.animateTo(0.18f, tween(70, easing = FastOutSlowInEasing)) }
                scale.animateTo(0.93f, tween(70, easing = FastOutSlowInEasing))

                // Step 2: 0.93 -> 1.02 subtle rebound (~70ms)
                scale.animateTo(1.02f, tween(70, easing = FastOutSlowInEasing))

                // Step 3: 1.02 -> 1.0 settle back to normal (~70ms)
                launch { brightness.animateTo(0f, tween(70, easing = FastOutSlowInEasing)) }
                scale.animateTo(1.0f, tween(70, easing = FastOutSlowInEasing))

                isTriggered = false
                onLaunch()
            }
        }
    }

    val modifier = Modifier.graphicsLayer {
        this.scaleX = scale.value
        this.scaleY = scale.value
        alpha = (1f + brightness.value * 0.15f).coerceAtMost(1f)
    }

    return Pair(modifier, trigger)
}

/**
 * Modifier extension for physical glass press response:
 * Slightly brighter, slightly smaller (scale 0.97), border highlight, soft spring return.
 */
@Composable
fun Modifier.glassPressable(
    onClick: () -> Unit
): Modifier {
    val scale = remember { Animatable(1f) }
    val brightness = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = (1f + brightness.value * 0.2f).coerceAtMost(1f)
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scale.animateTo(0.97f, tween(80))
                        brightness.animateTo(0.20f, tween(80))
                    }
                    val released = tryAwaitRelease()
                    scope.launch {
                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        brightness.animateTo(0f, tween(120))
                    }
                    if (released) {
                        onClick()
                    }
                }
            )
        }
}
