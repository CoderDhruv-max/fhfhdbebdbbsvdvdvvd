package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.ui.glass.rememberIconPressState
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlin.math.abs
import kotlin.math.hypot

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconView(
    app: AppInfo,
    onLaunch: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    showLabel: Boolean = true,
    textColor: Color = Color.White,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    val (pressModifier, triggerPress) = rememberIconPressState(onLaunch = onLaunch)
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var itemWindowOffset by remember { mutableStateOf(Offset.Zero) }

    val gestureModifier = if (onDragStart != null && onDrag != null && onDragEnd != null) {
        Modifier
            .onGloballyPositioned { coordinates ->
                itemWindowOffset = coordinates.positionInWindow()
            }
            .pointerInput(app.packageName) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        dragDistance = 0f
                        onDragStart(itemWindowOffset + offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistance += hypot(dragAmount.x, dragAmount.y)
                        onDrag(dragAmount)
                    },
                    onDragEnd = {
                        if (dragDistance < 15f) {
                            onLongClick()
                        }
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    }
                )
            }
            .combinedClickable(
                onClick = triggerPress
            )
    } else {
        Modifier.combinedClickable(
            onClick = triggerPress,
            onLongClick = onLongClick
        )
    }

    // Dynamic gradient colors for fallback icon badge based on package hash
    val fallbackGradients = remember(app.packageName) {
        val palettes = listOf(
            listOf(Color(0xFF38BDF8), Color(0xFF0284C7)), // Cyan / Blue
            listOf(Color(0xFF818CF8), Color(0xFF4F46E5)), // Indigo / Violet
            listOf(Color(0xFFF472B6), Color(0xFFDB2777)), // Pink / Magenta
            listOf(Color(0xFF34D399), Color(0xFF059669)), // Emerald / Teal
            listOf(Color(0xFFFBBF24), Color(0xFFD97706)), // Amber / Orange
            listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))  // Purple
        )
        val index = abs(app.packageName.hashCode()) % palettes.size
        palettes[index]
    }

    val badgeShape = remember(iconSize) {
        RoundedCornerShape((iconSize.value * 0.28f).coerceIn(8f, 16f).dp)
    }

    Column(
        modifier = modifier
            .then(pressModifier)
            .then(gestureModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-contrast Liquid Glass icon backing badge
        Box(
            modifier = Modifier
                .size(iconSize)
                .shadow(
                    elevation = if (iconSize < 36.dp) 3.dp else 6.dp,
                    shape = badgeShape,
                    ambientColor = Color(0x40000000),
                    spotColor = Color(0x30000000)
                )
                .clip(badgeShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.70f),
                            Color.White.copy(alpha = 0.20f),
                            Color(0x8038BDF8)
                        )
                    ),
                    shape = badgeShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .background(
                            Brush.linearGradient(colors = fallbackGradients)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                    if (initial.isNotEmpty()) {
                        Text(
                            text = initial,
                            fontSize = (iconSize.value * 0.45f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = app.label,
                            tint = Color.White,
                            modifier = Modifier.size(iconSize * 0.58f)
                        )
                    }
                }
            }

            // Top-left subtle specular gloss glint
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = iconSize.value * 2.2f
                        )
                    )
            )
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = app.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
