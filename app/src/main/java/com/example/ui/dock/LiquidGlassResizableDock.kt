package com.example.ui.dock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppInfo
import com.example.model.DockScaleMode
import com.example.model.LauncherSettings
import com.example.ui.components.AppIconView
import com.example.ui.glass.LiquidGlass
import com.example.ui.glass.LiquidGlassDefaults
import kotlin.math.roundToInt

/**
 * Dynamic, resizable dock component at the bottom of the home screen.
 * - Sized dynamically to fit pinned apps with smooth spring animation
 * - Resizable between Compact, Standard, and Expanded dock modes via touch handle
 * - Supports pinning frequently used apps via quick "+" sheet
 * - Features interactive drag-and-drop capability for reordering dock items
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassResizableDock(
    dockApps: List<AppInfo>,
    frequentApps: List<AppInfo>,
    allApps: List<AppInfo>,
    settings: LauncherSettings,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onReorderDock: (fromIndex: Int, toIndex: Int) -> Unit,
    onPinToDock: (AppInfo, Int?) -> Unit,
    onUnpinFromDock: (String) -> Unit,
    onUpdateDockScaleMode: (DockScaleMode) -> Unit,
    modifier: Modifier = Modifier,
    isExternalDragActive: Boolean = false,
    draggedExternalApp: AppInfo? = null,
    onExternalDropOnDock: (AppInfo) -> Unit = {}
) {
    var showPinSheet by remember { mutableStateOf(false) }

    // Dock dimensions based on scale mode
    val currentMode = settings.dockScaleMode
    val (iconSizeDp, slotPaddingDp, minDockHeightDp) = when (currentMode) {
        DockScaleMode.COMPACT -> Triple(40.dp, 5.dp, 66.dp)
        DockScaleMode.STANDARD -> Triple(48.dp, 7.dp, 78.dp)
        DockScaleMode.EXPANDED -> Triple(58.dp, 10.dp, 94.dp)
    }

    // Drag and drop state within dock
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragHorizontalOffset by remember { mutableFloatStateOf(0f) }
    var dragVerticalOffset by remember { mutableFloatStateOf(0f) }
    var targetHoverIndex by remember { mutableIntStateOf(-1) }

    val density = LocalDensity.current
    val slotWidthPx = with(density) { (iconSizeDp + (slotPaddingDp * 2)).toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Glass Dock Container
        LiquidGlass(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    )
                )
                .then(
                    if (isExternalDragActive) {
                        Modifier.border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF60A5FA), Color(0xFF38BDF8), Color(0xFF818CF8))
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                    } else Modifier
                ),
            cornerRadius = 32.dp,
            opacity = settings.glassOpacity,
            blurRadius = 20.dp,
            elevation = 14.dp,
            showPerimeterEffect = isExternalDragActive
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dock Top Resize Handle / Mode Indicator
                Row(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // Cycle through sizes: Compact -> Standard -> Expanded -> Compact
                            val nextMode = when (currentMode) {
                                DockScaleMode.COMPACT -> DockScaleMode.STANDARD
                                DockScaleMode.STANDARD -> DockScaleMode.EXPANDED
                                DockScaleMode.EXPANDED -> DockScaleMode.COMPACT
                            }
                            onUpdateDockScaleMode(nextMode)
                        }
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimalist resize grab handle
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.55f))
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = when (currentMode) {
                            DockScaleMode.COMPACT -> "S"
                            DockScaleMode.STANDARD -> "M"
                            DockScaleMode.EXPANDED -> "L"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // If external drag is hovering over dock, show drop banner
                if (isExternalDragActive && draggedExternalApp != null) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x3338BDF8))
                            .clickable { onExternalDropOnDock(draggedExternalApp) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Release to Pin ${draggedExternalApp.label} to Dock",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Pinned Apps Row with Drag & Drop
                Row(
                    modifier = Modifier
                        .height(minDockHeightDp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxSlots = 7
                    val canPinMore = dockApps.size < maxSlots

                    dockApps.forEachIndexed { index, app ->
                        val isBeingDragged = draggingIndex == index
                        val isTargetSlot = targetHoverIndex == index && draggingIndex != -1
                        val isLiftedToUnpin = isBeingDragged && dragVerticalOffset < -70f

                        // Dynamic neighbor slot shift with spring physics
                        val slotShift = when {
                            draggingIndex != -1 && draggingIndex < targetHoverIndex && index > draggingIndex && index <= targetHoverIndex -> -slotWidthPx
                            draggingIndex != -1 && draggingIndex > targetHoverIndex && index < draggingIndex && index >= targetHoverIndex -> slotWidthPx
                            else -> 0f
                        }
                        val animatedSlotShift by animateFloatAsState(
                            targetValue = slotShift,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            ),
                            label = "slot_shift_$index"
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = slotPaddingDp)
                                .pointerInput(index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragHorizontalOffset = 0f
                                            dragVerticalOffset = 0f
                                            targetHoverIndex = index
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragHorizontalOffset += dragAmount.x
                                            dragVerticalOffset += dragAmount.y
                                            val shiftSlots = (dragHorizontalOffset / slotWidthPx).roundToInt()
                                            val newTarget = (index + shiftSlots).coerceIn(0, dockApps.lastIndex)
                                            targetHoverIndex = newTarget
                                        },
                                        onDragEnd = {
                                            if (draggingIndex != -1) {
                                                if (dragVerticalOffset < -70f) {
                                                    val appToUnpin = dockApps.getOrNull(draggingIndex)
                                                    if (appToUnpin != null) {
                                                        onUnpinFromDock(appToUnpin.packageName)
                                                    }
                                                } else if (targetHoverIndex != -1 && draggingIndex != targetHoverIndex) {
                                                    onReorderDock(draggingIndex, targetHoverIndex)
                                                }
                                            }
                                            draggingIndex = -1
                                            dragHorizontalOffset = 0f
                                            dragVerticalOffset = 0f
                                            targetHoverIndex = -1
                                        },
                                        onDragCancel = {
                                            draggingIndex = -1
                                            dragHorizontalOffset = 0f
                                            dragVerticalOffset = 0f
                                            targetHoverIndex = -1
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Target hover insertion glow
                            if (isTargetSlot && !isBeingDragged) {
                                Box(
                                    modifier = Modifier
                                        .size(iconSizeDp + 10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x4D38BDF8))
                                        .border(1.5.dp, Color(0xFF7DD3FC), CircleShape)
                                )
                            }

                            // App Icon with drag offset, spring shifts, and elevation
                            val offsetX = if (isBeingDragged) dragHorizontalOffset else animatedSlotShift
                            val offsetY = if (isBeingDragged) dragVerticalOffset else 0f
                            val scale = if (isBeingDragged) 1.22f else 1.0f

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = offsetX
                                        translationY = offsetY
                                        scaleX = scale
                                        scaleY = scale
                                        shadowElevation = if (isBeingDragged) 24f else 0f
                                    }
                            ) {
                                AppIconView(
                                    app = app,
                                    onLaunch = {
                                        if (draggingIndex == -1) {
                                            onLaunchApp(app.packageName)
                                        }
                                    },
                                    onLongClick = {
                                        if (draggingIndex == -1) {
                                            onAppLongClick(app)
                                        }
                                    },
                                    iconSize = iconSizeDp,
                                    showLabel = false
                                )
                            }

                            // Floating Unpin Badge when dragged upwards out of dock
                            if (isLiftedToUnpin) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(dragHorizontalOffset.roundToInt(), (dragVerticalOffset - 60f).roundToInt()) }
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xF0EF4444))
                                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Unpin",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Unpin",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // External Drag & Drop slot indicator in dock
                    if (isExternalDragActive && draggedExternalApp != null && canPinMore) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = slotPaddingDp)
                                .size(iconSizeDp + 6.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x3338BDF8))
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8))),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { onExternalDropOnDock(draggedExternalApp) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Drop to pin to dock",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size((iconSizeDp.value * 0.45f).dp)
                            )
                        }
                    }

                    // "+" Button to Pin Frequently Used Apps if slots available
                    if (canPinMore && !isExternalDragActive) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = slotPaddingDp)
                                .size(iconSizeDp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                                .clickable { showPinSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Pin frequently used app to dock",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size((iconSizeDp.value * 0.45f).dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Pin Frequently Used Apps Sheet / Dialog
    if (showPinSheet) {
        PinFrequentAppsDialog(
            frequentApps = frequentApps,
            allApps = allApps,
            dockApps = dockApps,
            currentScaleMode = currentMode,
            onDismiss = { showPinSheet = false },
            onPinApp = { app ->
                onPinToDock(app, null)
            },
            onUnpinApp = { packageName ->
                onUnpinFromDock(packageName)
            },
            onUpdateScaleMode = onUpdateDockScaleMode
        )
    }
}

/**
 * Bottom dialog sheet to pin frequently used apps and configure dock scale.
 */
@Composable
fun PinFrequentAppsDialog(
    frequentApps: List<AppInfo>,
    allApps: List<AppInfo>,
    dockApps: List<AppInfo>,
    currentScaleMode: DockScaleMode,
    onDismiss: () -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (String) -> Unit,
    onUpdateScaleMode: (DockScaleMode) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val pinnedPackages = remember(dockApps) { dockApps.map { it.packageName }.toSet() }

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlass(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            cornerRadius = 28.dp,
            opacity = 0.92f,
            blurRadius = 24.dp,
            elevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pin to Dock",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "${dockApps.size}/7 pinned apps",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dock Size Selector Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x14000000))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DockScaleMode.values().forEach { mode ->
                        val isSelected = currentScaleMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { onUpdateScaleMode(mode) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (mode) {
                                    DockScaleMode.COMPACT -> "Compact"
                                    DockScaleMode.STANDARD -> "Standard"
                                    DockScaleMode.EXPANDED -> "Expanded"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps to pin...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0x33000000)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Frequently Used Section (if search is empty)
                    if (searchQuery.isBlank() && frequentApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "Frequently Used",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(frequentApps.take(5)) { app ->
                            val isPinned = pinnedPackages.contains(app.packageName)
                            PinAppItemRow(
                                app = app,
                                isPinned = isPinned,
                                subtitle = if (app.launchCount > 0) "Launched ${app.launchCount} times" else "Frequently used",
                                onTogglePin = {
                                    if (isPinned) onUnpinApp(app.packageName)
                                    else onPinApp(app)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "All Installed Apps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    items(filteredApps) { app ->
                        val isPinned = pinnedPackages.contains(app.packageName)
                        PinAppItemRow(
                            app = app,
                            isPinned = isPinned,
                            subtitle = app.packageName,
                            onTogglePin = {
                                if (isPinned) onUnpinApp(app.packageName)
                                else onPinApp(app)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinAppItemRow(
    app: AppInfo,
    isPinned: Boolean,
    subtitle: String,
    onTogglePin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0A000000))
            .clickable { onTogglePin() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            AppIconView(
                app = app,
                onLaunch = {},
                onLongClick = {},
                iconSize = 38.dp,
                showLabel = false
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = app.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Pin/Unpin action pill button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPinned) Color(0xFF3B82F6) else Color(0x1F3B82F6))
                .clickable { onTogglePin() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.Check else Icons.Default.PushPin,
                    contentDescription = if (isPinned) "Pinned" else "Pin to Dock",
                    tint = if (isPinned) Color.White else Color(0xFF2563EB),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPinned) "Pinned" else "Pin",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPinned) Color.White else Color(0xFF2563EB)
                )
            }
        }
    }
}
