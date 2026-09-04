package com.example.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.model.LauncherSettings
import com.example.ui.components.AppIconView
import com.example.ui.glass.LiquidGlassDefaults
import com.example.ui.glass.LiquidGlassSurface
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AppDrawerSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
    apps: List<AppInfo>,
    settings: LauncherSettings,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    BackHandler {
        onClose()
    }

    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            apps.filter {
                it.label.lowercase(Locale.getDefault()).contains(q) ||
                        it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sheetHeightPx = constraints.maxHeight.toFloat()
        val offsetY = remember { Animatable(sheetHeightPx) }

        // Animate open smoothly on launch
        LaunchedEffect(isOpen) {
            if (isOpen) {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        // Gesture-driven drag to dismiss
        val draggableState = rememberDraggableState { delta ->
            val newOffset = (offsetY.value + delta).coerceAtLeast(0f)
            scope.launch {
                offsetY.snapTo(newOffset)
            }
        }

        val shape = RoundedCornerShape(
            topStart = (settings.cornerRadiusDp + 8).dp,
            topEnd = (settings.cornerRadiusDp + 8).dp
        )

        // Scrim background with subtle blur feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (0.25f * (1f - (offsetY.value / sheetHeightPx))).coerceIn(0f, 0.25f)))
        )

        // Frosted Milky Liquid Glass Drawer Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (offsetY.value > sheetHeightPx * 0.35f || velocity > 1200f) {
                            scope.launch {
                                offsetY.animateTo(
                                    sheetHeightPx,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )
                                onClose()
                            }
                        } else {
                            scope.launch {
                                offsetY.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    }
                )
                .shadow(
                    elevation = 24.dp,
                    shape = shape,
                    ambientColor = LiquidGlassDefaults.GlassShadow,
                    spotColor = LiquidGlassDefaults.GlassShadow
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF0FFFFFF).copy(alpha = (settings.glassOpacity * 1.05f).coerceAtMost(0.92f)),
                            Color(0xEAEFF6).copy(alpha = settings.glassOpacity),
                            Color(0xDFE7F3).copy(alpha = (settings.glassOpacity * 0.96f))
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            LiquidGlassDefaults.GlassBorderHigh,
                            LiquidGlassDefaults.GlassBorderLow
                        )
                    ),
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Drag handle pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(Color(0x33000000))
                    )
                }

                // Liquid Glass Search Bar
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    cornerRadius = 24.dp,
                    opacity = 0.85f,
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF334155)),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search apps...",
                                        fontSize = 15.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // App Count or empty state indicator
                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No apps found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Try typing a different name",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                } else {
                    // Vertical App Grid
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(settings.gridCols),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 40.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(
                            items = filteredApps,
                            key = { it.packageName }
                        ) { app ->
                            AppIconView(
                                app = app,
                                onLaunch = {
                                    onClose()
                                    onLaunchApp(app.packageName)
                                },
                                onLongClick = {
                                    onAppLongClick(app)
                                },
                                iconSize = settings.iconSizeDp.dp,
                                showLabel = settings.showLabels,
                                textColor = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    }
}
