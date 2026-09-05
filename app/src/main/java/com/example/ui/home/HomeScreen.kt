package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppWidgetHelper
import com.example.model.AppInfo
import com.example.model.DockScaleMode
import com.example.model.HomeItemPlacement
import com.example.model.LauncherDrawerState
import com.example.model.LauncherItem
import com.example.model.LauncherSettings
import com.example.ui.components.AppIconView
import com.example.ui.components.FolderIconView
import com.example.ui.components.MinimalClock
import com.example.ui.components.WidgetHostContainer
import com.example.ui.dock.LiquidGlassResizableDock
import com.example.ui.glass.LiquidGlassPill
import com.example.ui.settings.DefaultLauncherPrompt
import com.example.viewmodel.NewRecentAppEvent
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    drawerState: LauncherDrawerState,
    newRecentAppEvent: NewRecentAppEvent?,
    onConsumeNewRecentAppEvent: () -> Unit,
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
    // Total pages: Page 0 = Google Feed, Pages 1..N = Launcher Home Pages
    val totalPages = 1 + settings.pageCount.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { totalPages }
    )

    // Compute parallax offset for fluid wallpaper based on pager scroll
    val pagerOffset = (pagerState.currentPage - 1) + pagerState.currentPageOffsetFraction

    // Drag-to-dock state from home page grid
    var isDraggingFromHome by remember { mutableStateOf(false) }
    var draggedHomeApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var dockBounds by remember { mutableStateOf(Rect.Zero) }

    // Spring-based animations for background blur and home scale when app drawer opens
    val isDrawerActive = drawerState != LauncherDrawerState.HOME
    val backgroundBlur by animateDpAsState(
        targetValue = if (isDrawerActive) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "background_blur"
    )
    val homeScale by animateFloatAsState(
        targetValue = if (isDrawerActive) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "home_scale"
    )
    val homeAlpha by animateFloatAsState(
        targetValue = if (isDrawerActive) 0.65f else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium
        ),
        label = "home_alpha"
    )

    // White perimeter stroke animation trigger state for genuine new apps
    var pillStrokeTrigger by remember { mutableFloatStateOf(0f) }
    var newlyAddedPackage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(newRecentAppEvent) {
        if (newRecentAppEvent != null) {
            newlyAddedPackage = newRecentAppEvent.packageName
            pillStrokeTrigger = newRecentAppEvent.trigger.toFloat()
            onConsumeNewRecentAppEvent()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Long-press wallpaper on empty area opens customization
            .pointerInput(drawerState) {
                if (drawerState == LauncherDrawerState.HOME) {
                    detectTapGestures(
                        onLongPress = { onOpenCustomization() }
                    )
                }
            }
            // Vertical gesture detector: unambiguous separation from horizontal pager
            .pointerInput(drawerState) {
                if (drawerState == LauncherDrawerState.HOME) {
                    var totalDragY = 0f
                    var startY = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            totalDragY = 0f
                            startY = offset.y
                        },
                        onDrag = { change, dragAmount ->
                            totalDragY += dragAmount.y
                            // Swipe UP from lower half -> Open App Drawer
                            if (totalDragY < -40f && startY > size.height * 0.40f) {
                                change.consume()
                                onOpenAppDrawer()
                            }
                            // Swipe DOWN from upper 45% of screen -> Expand Notifications
                            else if (totalDragY > 45f && startY < size.height * 0.45f) {
                                change.consume()
                                onExpandNotifications()
                            }
                        }
                    )
                }
            }
    ) {
        // Living Ambient Liquid Fluid Wallpaper with subtle parallax shift and glassmorphism blur
        LiquidFluidWallpaper(
            parallaxOffset = pagerOffset,
            blurRadius = backgroundBlur
        )

        // Horizontal Pager: Page 0 is Google Feed, Page 1+ are Launcher Home screens
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = homeScale
                    scaleY = homeScale
                    alpha = homeAlpha
                }
        ) { page ->
            if (page == 0) {
                // Dedicated Google Feed & Discover Page
                GoogleFeedPage(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Subtle shift & fade when moving into feed
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            alpha = (1f - pageOffset.coerceIn(0f, 1f) * 0.35f)
                        }
                )
            } else {
                val homePageIndex = page - 1
                val isHoveringDock = isDraggingFromHome &&
                    dockBounds != Rect.Zero &&
                    dragPosition.y >= (dockBounds.top - 60f)

                HomeMainContent(
                    pageIndex = homePageIndex,
                    apps = apps,
                    homeItems = homeItems,
                    dockApps = dockApps,
                    pillApps = pillApps,
                    frequentApps = frequentApps,
                    settings = settings,
                    widgetHelper = widgetHelper,
                    isDefaultLauncher = isDefaultLauncher,
                    newlyAddedPackage = newlyAddedPackage,
                    pillStrokeTrigger = pillStrokeTrigger.toLong(),
                    onLaunchApp = onLaunchApp,
                    onOpenAppDrawer = onOpenAppDrawer,
                    onAppLongClick = onAppLongClick,
                    onFolderClick = onFolderClick,
                    onFolderLongClick = onFolderLongClick,
                    onWidgetLongClick = onWidgetLongClick,
                    onSetDefaultLauncher = onSetDefaultLauncher,
                    onDismissDefaultPrompt = onDismissDefaultPrompt,
                    onReorderDock = onReorderDock,
                    onPinToDock = onPinToDock,
                    onUnpinFromDock = onUnpinFromDock,
                    onUpdateDockScaleMode = onUpdateDockScaleMode,
                    isDraggingFromHome = isDraggingFromHome || isHoveringDock,
                    draggedHomeApp = draggedHomeApp,
                    onDockPositioned = { bounds -> dockBounds = bounds },
                    onDragStartApp = { app, offset ->
                        isDraggingFromHome = true
                        draggedHomeApp = app
                        dragPosition = offset
                    },
                    onDragApp = { dragAmount ->
                        dragPosition += dragAmount
                    },
                    onDragEndApp = {
                        if (draggedHomeApp != null) {
                            val isOverDock = dockBounds != Rect.Zero &&
                                dragPosition.y >= (dockBounds.top - 60f) &&
                                dragPosition.y <= (dockBounds.bottom + 60f)
                            if (isOverDock) {
                                onPinToDock(draggedHomeApp!!, null)
                            }
                        }
                        isDraggingFromHome = false
                        draggedHomeApp = null
                    }
                )
            }
        }

        // Floating Page Indicator Dots (Only displayed when there are multiple pages)
        if (totalPages > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 106.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { pageIndex ->
                    val isSelected = pagerState.currentPage == pageIndex
                    if (pageIndex == 0) {
                        // Google Feed indicator dot
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF38BDF8) else Color(0x6638BDF8))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color(0x66FFFFFF))
                        )
                    }
                }
            }
        }

        // Floating drag preview when dragging an app from home screen to dock
        if (isDraggingFromHome && draggedHomeApp != null) {
            val isNearDock = dockBounds != Rect.Zero && dragPosition.y >= (dockBounds.top - 60f)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragPosition.x - 38.dp.roundToPx()).roundToInt(),
                            (dragPosition.y - 38.dp.roundToPx()).roundToInt()
                        )
                    }
                    .size(76.dp)
                    .shadow(24.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xE60F172A))
                    .border(
                        2.dp,
                        if (isNearDock) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.65f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppIconView(
                    app = draggedHomeApp!!,
                    onLaunch = {},
                    onLongClick = {},
                    iconSize = 52.dp,
                    showLabel = false
                )
            }
        }
    }
}

/**
 * Main Home screen layout featuring:
 * - Minimal Clock
 * - Recent Apps Pill with animated white perimeter stroke
 * - Home Page App & Widget Grid
 * - Swipe up for apps pill
 * - Resizable Liquid Glass Dock
 */
@Composable
private fun HomeMainContent(
    pageIndex: Int,
    apps: List<AppInfo>,
    homeItems: List<HomeItemPlacement>,
    dockApps: List<AppInfo>,
    pillApps: List<AppInfo>,
    frequentApps: List<AppInfo>,
    settings: LauncherSettings,
    widgetHelper: AppWidgetHelper,
    isDefaultLauncher: Boolean,
    newlyAddedPackage: String?,
    pillStrokeTrigger: Long,
    onLaunchApp: (String) -> Unit,
    onOpenAppDrawer: () -> Unit,
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
    isDraggingFromHome: Boolean,
    draggedHomeApp: AppInfo?,
    onDockPositioned: ((Rect) -> Unit)? = null,
    onDragStartApp: (AppInfo, Offset) -> Unit,
    onDragApp: (Offset) -> Unit,
    onDragEndApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // First-run default launcher prompt
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

        // Minimal Clock and Date (rendered on primary page)
        if (pageIndex == 0) {
            MinimalClock(
                is24Hour = settings.clock24Hour,
                showDate = settings.showClockDate
            )

            // Floating Minimal Liquid Glass Recents Pill with perimeter highlight animation
            if (pillApps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
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
        }

        // Grid Area for this page
        val pageItems = remember(homeItems, pageIndex) {
            homeItems.filter { it.page == pageIndex }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
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
                onDragStartApp = onDragStartApp,
                onDragApp = onDragApp,
                onDragEndApp = onDragEndApp
            )
        }

        // Swipe Up App Drawer Pill Hint
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAppDrawer() }
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Swipe up for apps",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
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
                .onGloballyPositioned { coordinates ->
                    onDockPositioned?.invoke(coordinates.boundsInWindow())
                }
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
                }
            )
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                alpha.animateTo(1f, animationSpec = tween(durationMillis = 240))
            }
            scale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
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
            .padding(horizontal = 3.dp)
            .defaultMinSize(minWidth = 46.dp, minHeight = 42.dp),
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
