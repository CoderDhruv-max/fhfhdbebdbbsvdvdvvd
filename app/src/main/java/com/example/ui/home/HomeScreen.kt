package com.example.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppWidgetHelper
import com.example.model.AppInfo
import com.example.model.DockScaleMode
import com.example.model.HomeItemPlacement
import com.example.model.LauncherItem
import com.example.model.LauncherSettings
import com.example.model.PillMode
import com.example.ui.components.AppIconView
import com.example.ui.components.FolderIconView
import com.example.ui.components.MinimalClock
import com.example.ui.components.WidgetHostContainer
import com.example.ui.dock.LiquidGlassResizableDock
import com.example.ui.glass.LiquidGlass
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.settings.DefaultLauncherPrompt
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    homeItems: List<HomeItemPlacement>,
    dockApps: List<AppInfo>,
    pillApps: List<AppInfo>,
    frequentApps: List<AppInfo>,
    settings: LauncherSettings,
    widgetHelper: AppWidgetHelper,
    isDefaultLauncher: Boolean,
    onLaunchApp: (String) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenCustomization: () -> Unit,
    onExpandNotifications: () -> Unit,
    onAppLongClick: (AppInfo, isOnHome: Boolean) -> Unit,
    onFolderClick: (LauncherItem.Folder) -> Unit,
    onFolderLongClick: (LauncherItem.Folder) -> Unit,
    onWidgetLongClick: (LauncherItem.Widget) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onDismissDefaultPrompt: () -> Unit,
    onReorderDock: (fromIndex: Int, toIndex: Int) -> Unit,
    onPinToDock: (AppInfo, Int?) -> Unit,
    onUnpinFromDock: (String) -> Unit,
    onUpdateDockScaleMode: (DockScaleMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = settings.defaultPage.coerceIn(0, (settings.pageCount - 1).coerceAtLeast(0)),
        pageCount = { settings.pageCount.coerceAtLeast(1) }
    )

    // Drag-to-dock state from home page grid
    var isDraggingFromHome by remember { mutableStateOf(false) }
    var draggedHomeApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }

    // Gesture detection for swipe-up (app drawer) and swipe-down (notifications)
    val verticalDraggableState = rememberDraggableState { delta ->
        if (delta < -25f) {
            onOpenAppDrawer()
        } else if (delta > 35f) {
            onExpandNotifications()
        }
    }

    // Genuinely new app detection in Recent Apps pill
    var previousPillPackages by remember { mutableStateOf<List<String>?>(null) }
    var newlyAddedPackage by remember { mutableStateOf<String?>(null) }
    var pillStrokeTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(pillApps) {
        val currentPackages = pillApps.map { it.packageName }
        if (currentPackages.isNotEmpty()) {
            val previous = previousPillPackages
            if (previous == null) {
                // Initialize on startup: Do NOT trigger animation on initial launcher load
                previousPillPackages = currentPackages
            } else {
                // Trigger ONLY when a genuinely NEW app is added to the Recent Apps pill
                val added = currentPackages.firstOrNull { it !in previous }
                if (added != null) {
                    newlyAddedPackage = added
                    pillStrokeTrigger = System.currentTimeMillis()
                }
                previousPillPackages = currentPackages
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                state = verticalDraggableState,
                orientation = Orientation.Vertical
            )
            .combinedClickable(
                onClick = {},
                onLongClick = onOpenCustomization
            )
    ) {
        // Living Ambient Liquid Fluid Wallpaper
        LiquidFluidWallpaper()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // First-run default launcher prompt if not default
            AnimatedVisibility(
                visible = !isDefaultLauncher && !settings.firstRunCompleted,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DefaultLauncherPrompt(
                    onSetDefault = onSetDefaultLauncher,
                    onDismiss = onDismissDefaultPrompt
                )
            }

            // Minimal Clock and Date
            MinimalClock(
                is24Hour = settings.clock24Hour,
                showDate = settings.showClockDate
            )

            // Floating Liquid Glass Recents Pill - Thin, compact capsule with animated white perimeter stroke
            if (pillApps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassPill(
                        opacity = settings.glassOpacity,
                        strokeTrigger = pillStrokeTrigger
                    ) {
                        pillApps.take(5).forEach { app ->
                            PillAppIconItem(
                                app = app,
                                isNewlyAdded = (app.packageName == newlyAddedPackage),
                                onLaunch = { onLaunchApp(app.packageName) },
                                onLongClick = { onAppLongClick(app, false) },
                                iconSize = 34.dp
                            )
                        }
                    }
                }
            }

            // Multi-Page Grid Area
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pageItems = remember(homeItems, page) {
                    homeItems.filter { it.page == page }
                }

                HomePageGrid(
                    items = pageItems,
                    apps = apps,
                    settings = settings,
                    widgetHelper = widgetHelper,
                    onLaunchApp = onLaunchApp,
                    onAppLongClick = { app -> onAppLongClick(app, true) },
                    onFolderClick = onFolderClick,
                    onFolderLongClick = onFolderLongClick,
                    onWidgetLongClick = onWidgetLongClick,
                    modifier = Modifier.fillMaxSize(),
                    onDragStartApp = { app, offset ->
                        isDraggingFromHome = true
                        draggedHomeApp = app
                        dragPosition = offset
                    },
                    onDragApp = { dragAmount ->
                        dragPosition += dragAmount
                    },
                    onDragEndApp = {
                        // If released near bottom (e.g. over dock zone), pin to dock!
                        if (draggedHomeApp != null) {
                            onPinToDock(draggedHomeApp!!, null)
                        }
                        isDraggingFromHome = false
                        draggedHomeApp = null
                    }
                )
            }

            // Page Indicator Dots
            if (settings.pageCount > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(settings.pageCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0x66FFFFFF))
                        )
                    }
                }
            }

            // Swipe Up App Drawer Hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAppDrawer() }
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Swipe up for apps",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Swipe up for apps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Dynamic, Resizable Liquid Glass Dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                LiquidGlassResizableDock(
                    dockApps = dockApps,
                    frequentApps = frequentApps,
                    allApps = apps,
                    settings = settings,
                    onLaunchApp = onLaunchApp,
                    onAppLongClick = { app -> onAppLongClick(app, false) },
                    onReorderDock = onReorderDock,
                    onPinToDock = onPinToDock,
                    onUnpinFromDock = onUnpinFromDock,
                    onUpdateDockScaleMode = onUpdateDockScaleMode,
                    isExternalDragActive = isDraggingFromHome,
                    draggedExternalApp = draggedHomeApp,
                    onExternalDropOnDock = { app ->
                        onPinToDock(app, null)
                        isDraggingFromHome = false
                        draggedHomeApp = null
                    }
                )
            }
        }

        // Floating drag preview when dragging an app from home screen
        if (isDraggingFromHome && draggedHomeApp != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragPosition.x - 36).roundToInt(),
                            (dragPosition.y - 36).roundToInt()
                        )
                    }
                    .size(72.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.90f)),
                contentAlignment = Alignment.Center
            ) {
                AppIconView(
                    app = draggedHomeApp!!,
                    onLaunch = {},
                    onLongClick = {},
                    iconSize = 50.dp,
                    showLabel = false
                )
            }
        }
    }
}

@Composable
private fun HomePageGrid(
    items: List<HomeItemPlacement>,
    apps: List<AppInfo>,
    settings: LauncherSettings,
    widgetHelper: AppWidgetHelper,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onFolderClick: (LauncherItem.Folder) -> Unit,
    onFolderLongClick: (LauncherItem.Folder) -> Unit,
    onWidgetLongClick: (LauncherItem.Widget) -> Unit,
    modifier: Modifier = Modifier,
    onDragStartApp: ((AppInfo, Offset) -> Unit)? = null,
    onDragApp: ((Offset) -> Unit)? = null,
    onDragEndApp: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Render Widgets first if present on this page
        val widgets = items.mapNotNull { it.item as? LauncherItem.Widget }
        widgets.forEach { widget ->
            WidgetHostContainer(
                widgetItem = widget,
                widgetHelper = widgetHelper,
                onLongClick = { onWidgetLongClick(widget) }
            )
        }

        // Render Apps & Folders in rows
        val nonWidgets = items.filter { it.item !is LauncherItem.Widget }
        val cols = settings.gridCols
        val chunked = nonWidgets.chunked(cols)

        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { placement ->
                    when (val item = placement.item) {
                        is LauncherItem.App -> {
                            val app = apps.find { it.packageName == item.packageName }
                            if (app != null) {
                                AppIconView(
                                    app = app,
                                    onLaunch = { onLaunchApp(app.packageName) },
                                    onLongClick = { onAppLongClick(app) },
                                    iconSize = settings.iconSizeDp.dp,
                                    showLabel = settings.showLabels,
                                    textColor = Color.White,
                                    onDragStart = { offset -> onDragStartApp?.invoke(app, offset) },
                                    onDrag = { dragAmount -> onDragApp?.invoke(dragAmount) },
                                    onDragEnd = { onDragEndApp?.invoke() }
                                )
                            }
                        }
                        is LauncherItem.Folder -> {
                            FolderIconView(
                                folder = item,
                                apps = apps,
                                onClick = { onFolderClick(item) },
                                onLongClick = { onFolderLongClick(item) },
                                iconSize = settings.iconSizeDp.dp,
                                showLabel = settings.showLabels,
                                textColor = Color.White
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

/**
 * Compact pill app icon with subtle insertion animation (scale 0.90 -> 1.05 -> 1.0, fade 0 -> 1)
 * and comfortable minimum touch target size.
 */
@Composable
private fun PillAppIconItem(
    app: AppInfo,
    isNewlyAdded: Boolean,
    onLaunch: () -> Unit,
    onLongClick: () -> Unit,
    iconSize: Dp = 34.dp
) {
    val scale = remember(app.packageName) { Animatable(if (isNewlyAdded) 0.90f else 1f) }
    val alpha = remember(app.packageName) { Animatable(if (isNewlyAdded) 0f else 1f) }

    LaunchedEffect(isNewlyAdded) {
        if (isNewlyAdded) {
            scale.snapTo(0.90f)
            alpha.snapTo(0f)
            launch {
                alpha.animateTo(1f, animationSpec = tween(durationMillis = 280))
            }
            // scale 0.90 -> 1.05 -> 1.0
            scale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .padding(horizontal = 4.dp)
            .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        AppIconView(
            app = app,
            onLaunch = onLaunch,
            onLongClick = onLongClick,
            iconSize = iconSize,
            showLabel = false
        )
    }
}
