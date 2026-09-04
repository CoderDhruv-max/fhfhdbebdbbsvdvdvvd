package com.example.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Parabolic Velocity Easing (Slow -> Fast -> Slow):
 * The velocity profile v(t) = 30 * t^2 * (1 - t)^2 is a symmetrical parabola,
 * starting with derivative 0 at t=0, peaking with high velocity in the center at t=0.5,
 * and decelerating smoothly back to 0 at t=1.
 * Integrating this velocity profile yields the quintic smoothstep:
 * s(t) = 6t^5 - 15t^4 + 10t^3.
 */
val ParabolicSlowFastSlowEasing = Easing { fraction ->
    val t = fraction.coerceIn(0f, 1f)
    t * t * t * (t * (t * 6f - 15f) + 10f)
}

/**
 * Generates an illuminated white stroke along the complete outer border of the recents pill
 * when a new recent app is detected. Animates with a parabolic (slow-fast-slow) velocity.
 *
 * Requirements:
 * - Starts at TOP CENTER and completes exactly ONE revolution clockwise.
 * - Parabolic speed profile: SLOW -> FAST -> SLOW (peaking at ~250ms).
 * - Target duration: 500ms.
 * - Ending effect: when completing the perimeter, briefly increases brightness (80ms),
 *   then smoothly fades out (120ms).
 * - Clean, thin pure white stroke (2.dp), no particles, no circles, no neon glow.
 */
@Composable
fun PillBorderStrokeAnimation(
    trigger: Long,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    strokeColor: Color = Color.White,
    durationMillis: Int = 500,
    onAnimationEnd: () -> Unit = {}
) {
    if (trigger <= 0L) return

    val progress = remember(trigger) { Animatable(0f) }
    val flashBrightness = remember(trigger) { Animatable(0f) }
    val fadeAlpha = remember(trigger) { Animatable(1f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        flashBrightness.snapTo(0f)
        fadeAlpha.snapTo(1f)

        // 1. Travel along complete outer perimeter with Parabolic (Slow -> Fast -> Slow) velocity (500ms)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = ParabolicSlowFastSlowEasing
            )
        )

        // 2. Ending Effect: briefly increase brightness for ~80ms (40ms up, 40ms down)
        flashBrightness.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 40)
        )
        flashBrightness.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 40)
        )

        // 3. Smoothly fade out naturally (~120ms)
        fadeAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 120, easing = LinearEasing)
        )
        onAnimationEnd()
    }

    val p = progress.value
    val alpha = fadeAlpha.value
    if (p <= 0f || alpha <= 0f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokePx = strokeWidth.toPx()
        val halfStroke = strokePx / 2f
        val left = halfStroke
        val top = halfStroke
        val right = size.width - halfStroke
        val bottom = size.height - halfStroke
        val w = right - left
        val h = bottom - top
        if (w <= 0f || h <= 0f) return@Canvas

        val r = (cornerRadius.toPx() - halfStroke).coerceIn(0f, minOf(w, h) / 2f)

        // Construct clockwise capsule perimeter path starting from Top-Center
        val fullPath = Path().apply {
            moveTo(left + w / 2f, top) // TOP CENTER
            lineTo(right - r, top)      // -> top-right
            arcTo(                      // -> right side
                rect = Rect(right - 2 * r, top, right, top + 2 * r),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(right, bottom - r)   // -> right side
            arcTo(                      // -> bottom-right
                rect = Rect(right - 2 * r, bottom - 2 * r, right, bottom),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(left + r, bottom)    // -> bottom
            arcTo(                      // -> bottom-left
                rect = Rect(left, bottom - 2 * r, left + 2 * r, bottom),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(left, top + r)       // -> left side
            arcTo(                      // -> top-left
                rect = Rect(left, top, left + 2 * r, top + 2 * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(left + w / 2f, top)  // -> return to TOP CENTER
            close()
        }

        val pathMeasure = PathMeasure()
        pathMeasure.setPath(fullPath, false)
        val totalLength = pathMeasure.length
        if (totalLength > 0f) {
            val generatedLength = (p * totalLength).coerceIn(0f, totalLength)
            val strokePath = Path()
            pathMeasure.getSegment(0f, generatedLength, strokePath, true)

            val flash = flashBrightness.value
            val currentStrokeWidth = strokePx + (0.5.dp.toPx() * flash)

            // Pure white perimeter stroke (clean, premium light)
            drawPath(
                path = strokePath,
                color = strokeColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                style = Stroke(
                    width = currentStrokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
fun PerimeterHighlightAnimation(
    trigger: Boolean,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.5.dp,
    highlightColor: Color = Color.White.copy(alpha = 0.9f),
    onAnimationEnd: () -> Unit = {}
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
        )
        onAnimationEnd()
    }

    val p = progress.value
    if (p <= 0f || p >= 1f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r = cornerRadius.toPx().coerceAtMost(minOf(w, h) / 2f)

        // Construct clockwise path starting from Top-Center:
        // Top-Center is (w/2, 0)
        val roundRect = RoundRect(
            rect = Rect(0f, 0f, w, h),
            topLeft = CornerRadius(r, r),
            topRight = CornerRadius(r, r),
            bottomRight = CornerRadius(r, r),
            bottomLeft = CornerRadius(r, r)
        )

        val fullPath = Path().apply {
            moveTo(w / 2f, 0f)
            lineTo(w - r, 0f)
            arcTo(
                rect = Rect(w - 2 * r, 0f, w, 2 * r),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(w, h - r)
            arcTo(
                rect = Rect(w - 2 * r, h - 2 * r, w, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(r, h)
            arcTo(
                rect = Rect(0f, h - 2 * r, 2 * r, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(0f, r)
            arcTo(
                rect = Rect(0f, 0f, 2 * r, 2 * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(w / 2f, 0f)
            close()
        }

        val pathMeasure = PathMeasure()
        pathMeasure.setPath(fullPath, false)
        val totalLength = pathMeasure.length
        if (totalLength > 0f) {
            val segmentLength = totalLength * 0.28f
            val startDist = p * totalLength
            val endDist = startDist + segmentLength

            val segmentPath = Path()
            if (endDist <= totalLength) {
                pathMeasure.getSegment(startDist, endDist, segmentPath, true)
            } else {
                // Wraps around the start
                pathMeasure.getSegment(startDist, totalLength, segmentPath, true)
                pathMeasure.getSegment(0f, endDist - totalLength, segmentPath, true)
            }

            drawPath(
                path = segmentPath,
                color = highlightColor,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
