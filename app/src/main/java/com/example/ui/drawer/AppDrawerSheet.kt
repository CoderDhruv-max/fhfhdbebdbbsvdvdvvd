package com.example.ui.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppInfo
import com.example.model.LauncherDrawerState
import com.example.model.LauncherSettings
import com.example.ui.components.AppIconView
import com.example.ui.glass.GlassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Ultra-Premium Liquid Glass App Drawer:
 * - 100% Reliable Search Bar with instant focus, software keyboard activation, search action, clear button, and voice search.
 * - Ultra-Luxurious Frosted Glass aesthetic with glowing neon-cyan ambient rim, glass cards, and soft blur depth.
 * - Top Suggested Apps Tray for instantaneous access to most-used applications.
 * - Fast-Scroll Alphabet Index on the right margin with floating frosted preview indicator.
 * - Staggered entrance micro-animations.
 * - Zero gesture contention: nested scrolling is confined to the app grid so headers & search inputs are never blocked.
 * - Integrated "Search on Google Play Store" fallback when search returns no matching apps.
 */
@Composable
fun AppDrawerSheet(
    drawerState: LauncherDrawerState,
    onClose: () -> Unit,
    apps: List<AppInfo>,
    settings: LauncherSettings,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onDrawerStateChanged: (LauncherDrawerState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (drawerState == LauncherDrawerState.HOME) {
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var activeLetterIndicator by remember { mutableStateOf<Char?>(null) }

    BackHandler {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            onClose()
        }
    }

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

    val suggestedApps = remember(apps) {
        val withStats = apps.filter { it.launchCount > 0 || it.lastLaunchTime > 0 }
            .sortedWith(compareByDescending<AppInfo> { it.lastLaunchTime }.thenByDescending { it.launchCount })
        if (withStats.isNotEmpty()) {
            withStats.take(5)
        } else {
            apps.take(5)
        }
    }

    val availableLetters = remember(apps) {
        apps.mapNotNull { it.label.firstOrNull()?.uppercaseChar() }
            .filter { it in 'A'..'Z' }
            .distinct()
            .sorted()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sheetHeightPx = constraints.maxHeight.toFloat()
        val progress = remember { Animatable(0f) }

        LaunchedEffect(drawerState) {
            when (drawerState) {
                LauncherDrawerState.DRAWER_OPENING, LauncherDrawerState.DRAWER_OPEN -> {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.76f,
                            stiffness = 360f
                        )
                    )
                    onDrawerStateChanged(LauncherDrawerState.DRAWER_OPEN)
                }
                LauncherDrawerState.DRAWER_CLOSING -> {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    progress.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.88f,
                            stiffness = 460f
                        )
                    )
                    onDrawerStateChanged(LauncherDrawerState.HOME)
                }
                LauncherDrawerState.HOME -> {
                    progress.snapTo(0f)
                }
            }
        }

        val currentProgress = progress.value
        val sheetOffsetY = ((1f - currentProgress) * sheetHeightPx).coerceIn(0f, sheetHeightPx)
        val contentScale = 0.95f + (currentProgress * 0.05f)

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -15f && isSearchFocused) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 18f && gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onClose()
                        return available
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (available.y > 600f && gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onClose()
                        return available
                    }
                    return super.onPostFling(consumed, available)
                }
            }
        }

        // Frosted Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (currentProgress * 0.58f).coerceIn(0f, 0.58f)
                }
                .background(Color(0xFF030712))
                .clickable { onClose() }
        )

        val drawerShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

        // Floating Frosted Liquid Glass Drawer Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, sheetOffsetY.roundToInt()) }
                .shadow(
                    elevation = 28.dp,
                    shape = drawerShape,
                    ambientColor = Color(0x66000000),
                    spotColor = Color(0x8838BDF8)
                )
                .clip(drawerShape)
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x6638BDF8),
                            Color(0x22818CF8),
                            Color(0x05FFFFFF)
                        )
                    ),
                    shape = drawerShape
                )
        ) {
            // Frosted Glassmorphism blur backdrop layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(radius = 24.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xF00F172A),
                                Color(0xF50B1120),
                                Color(0xF8030712)
                            )
                        )
                    )
            )

            // Top specular gleam
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = contentScale
                        scaleY = contentScale
                    }
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Pull-to-close top tactile handle bar with real-time physical spring gesture
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .pointerInput(sheetHeightPx) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newProgress = (progress.value - (dragAmount / sheetHeightPx)).coerceIn(0f, 1f)
                                    scope.launch { progress.snapTo(newProgress) }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        if (progress.value < 0.72f) {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            progress.animateTo(
                                                0f,
                                                spring(dampingRatio = 0.88f, stiffness = 460f)
                                            )
                                            onClose()
                                        } else {
                                            progress.animateTo(
                                                1f,
                                                spring(dampingRatio = 0.76f, stiffness = 360f)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        progress.animateTo(
                                            1f,
                                            spring(dampingRatio = 0.76f, stiffness = 360f)
                                        )
                                    }
                                }
                            )
                        }
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x66FFFFFF),
                                        Color(0xAA38BDF8),
                                        Color(0x66FFFFFF)
                                    )
                                )
                            )
                    )
                }

                // Ultra-Premium Liquid Glass Search Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                ) {
                    val borderAlpha by animateFloatAsState(
                        targetValue = if (isSearchFocused) 0.85f else 0.25f,
                        label = "search_border"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(if (isSearchFocused) 12.dp else 4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0x401E293B))
                            .border(
                                width = 1.2.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF38BDF8).copy(alpha = borderAlpha),
                                        Color(0xFF818CF8).copy(alpha = borderAlpha * 0.7f),
                                        Color(0xFF38BDF8).copy(alpha = borderAlpha)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search ${apps.size} apps...",
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.50f)
                                    )
                                )
                            }

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { state ->
                                        isSearchFocused = state.isFocused
                                    },
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                ),
                                cursorBrush = SolidColor(Color(0xFF38BDF8)),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search,
                                    keyboardType = KeyboardType.Text,
                                    autoCorrect = false
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (filteredApps.isNotEmpty()) {
                                            val firstApp = filteredApps.first()
                                            onClose()
                                            onLaunchApp(firstApp.packageName)
                                        }
                                        keyboardController?.hide()
                                    }
                                )
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    focusRequester.requestFocus()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    launchVoiceSearch(context)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice search",
                                    tint = Color(0xFF38BDF8).copy(alpha = 0.90f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Search status badge
                if (searchQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SEARCH RESULTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "${filteredApps.size} ${if (filteredApps.size == 1) "app" else "apps"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }
                }

                // Main App Content Layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (filteredApps.isEmpty() && searchQuery.isNotBlank()) {
                        EmptySearchState(
                            query = searchQuery,
                            onSearchPlayStore = {
                                launchPlayStoreSearch(context, searchQuery)
                                onClose()
                            }
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(settings.gridCols),
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 12.dp,
                                    end = if (searchQuery.isBlank() && availableLetters.size > 5) 30.dp else 12.dp
                                )
                                .nestedScroll(nestedScrollConnection),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (searchQuery.isBlank() && suggestedApps.isNotEmpty()) {
                                item(span = { GridItemSpan(settings.gridCols) }) {
                                    SuggestedAppsSection(
                                        suggestedApps = suggestedApps,
                                        settings = settings,
                                        onLaunchApp = { pkg ->
                                            onClose()
                                            onLaunchApp(pkg)
                                        },
                                        onAppLongClick = onAppLongClick
                                    )
                                }

                                item(span = { GridItemSpan(settings.gridCols) }) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ALL APPS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.sp,
                                            color = Color.White.copy(alpha = 0.50f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(0.8.dp)
                                                .background(Color.White.copy(alpha = 0.12f))
                                        )
                                    }
                                }
                            }

                            itemsIndexed(
                                items = filteredApps,
                                key = { _, app -> app.packageName }
                            ) { index, app ->
                                val rowIndex = index / settings.gridCols
                                StaggeredAppItem(
                                    app = app,
                                    rowIndex = rowIndex,
                                    isDrawerOpening = currentProgress < 0.95f,
                                    settings = settings,
                                    onLaunch = {
                                        onClose()
                                        onLaunchApp(app.packageName)
                                    },
                                    onLongClick = { onAppLongClick(app) }
                                )
                            }
                        }

                        if (searchQuery.isBlank() && availableLetters.size > 5) {
                            AlphabetFastScroller(
                                letters = availableLetters,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp, top = 20.dp, bottom = 20.dp),
                                onLetterSelected = { letter ->
                                    activeLetterIndicator = letter
                                    val targetIndex = filteredApps.indexOfFirst {
                                        it.label.firstOrNull()?.equals(letter, ignoreCase = true) == true
                                    }
                                    if (targetIndex >= 0) {
                                        scope.launch {
                                            val offset = if (suggestedApps.isNotEmpty()) 2 else 0
                                            gridState.scrollToItem(targetIndex + offset)
                                        }
                                    }
                                },
                                onLetterDismissed = {
                                    activeLetterIndicator = null
                                }
                            )
                        }

                        if (activeLetterIndicator != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(76.dp)
                                    .shadow(16.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color(0xE60F172A))
                                    .border(2.dp, Color(0xFF38BDF8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeLetterIndicator.toString(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedAppsSection(
    suggestedApps: List<AppInfo>,
    settings: LauncherSettings,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUGGESTED APPS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color(0xFF38BDF8).copy(alpha = 0.90f)
            )
        }

        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            cornerRadius = 20.dp,
            opacity = 0.50f,
            elevation = 4.dp,
            borderWidth = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                suggestedApps.forEach { app ->
                    AppIconView(
                        app = app,
                        onLaunch = { onLaunchApp(app.packageName) },
                        onLongClick = { onAppLongClick(app) },
                        iconSize = (settings.iconSizeDp * 0.90f).dp,
                        showLabel = settings.showLabels,
                        textColor = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    onSearchPlayStore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x2438BDF8))
                .border(1.5.dp, Color(0x6638BDF8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "No apps found matching",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )

        Text(
            text = "\"$query\"",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                    )
                )
                .clickable { onSearchPlayStore() }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = "Play Store",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search Google Play",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AlphabetFastScroller(
    letters: List<Char>,
    modifier: Modifier = Modifier,
    onLetterSelected: (Char) -> Unit,
    onLetterDismissed: () -> Unit
) {
    Column(
        modifier = modifier
            .width(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0x331E293B))
            .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(11.dp))
            .padding(vertical = 4.dp)
            .pointerInput(letters) {
                detectTapGestures(
                    onPress = { offset ->
                        val itemHeight = size.height / letters.size.coerceAtLeast(1)
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                        onLetterSelected(letters[index])
                        tryAwaitRelease()
                        onLetterDismissed()
                    }
                )
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val itemHeight = size.height / letters.size.coerceAtLeast(1)
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                        onLetterSelected(letters[index])
                    },
                    onDragEnd = { onLetterDismissed() },
                    onDragCancel = { onLetterDismissed() },
                    onVerticalDrag = { change, _ ->
                        val itemHeight = size.height / letters.size.coerceAtLeast(1)
                        val index = (change.position.y / itemHeight).toInt().coerceIn(0, letters.lastIndex)
                        onLetterSelected(letters[index])
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        letters.forEach { char ->
            Text(
                text = char.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.70f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StaggeredAppItem(
    app: AppInfo,
    rowIndex: Int,
    isDrawerOpening: Boolean,
    settings: LauncherSettings,
    onLaunch: () -> Unit,
    onLongClick: () -> Unit
) {
    val alpha = remember { Animatable(if (isDrawerOpening) 0f else 1f) }
    val scale = remember { Animatable(if (isDrawerOpening) 0.94f else 1f) }
    val translationY = remember { Animatable(if (isDrawerOpening) 8f else 0f) }

    LaunchedEffect(isDrawerOpening) {
        if (isDrawerOpening) {
            val delayMs = (rowIndex * 14L).coerceAtMost(260L)
            delay(delayMs)
            launch {
                alpha.animateTo(1f, tween(durationMillis = 200, easing = FastOutSlowInEasing))
            }
            launch {
                scale.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
            translationY.animateTo(0f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
        } else {
            alpha.snapTo(1f)
            scale.snapTo(1f)
            translationY.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha.value
                this.scaleX = scale.value
                this.scaleY = scale.value
                this.translationY = translationY.value
            },
        contentAlignment = Alignment.Center
    ) {
        AppIconView(
            app = app,
            onLaunch = onLaunch,
            onLongClick = onLongClick,
            iconSize = settings.iconSizeDp.dp,
            showLabel = settings.showLabels,
            textColor = Color.White
        )
    }
}

private fun launchVoiceSearch(context: Context) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
            // Ignore if speech recognition unavailable
        }
    }
}

private fun launchPlayStoreSearch(context: Context, query: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$query")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {
            // Ignore failure
        }
    }
}
