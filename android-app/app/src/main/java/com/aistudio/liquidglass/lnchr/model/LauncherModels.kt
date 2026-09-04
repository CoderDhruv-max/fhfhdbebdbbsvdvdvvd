package com.aistudio.liquidglass.lnchr.model

import androidx.compose.runtime.Immutable

sealed interface LauncherItem {
    val id: String

    data class App(
        override val id: String,
        val packageName: String,
        val activityName: String? = null
    ) : LauncherItem

    data class Folder(
        override val id: String,
        val name: String,
        val packageNames: List<String>
    ) : LauncherItem

    data class Widget(
        override val id: String,
        val appWidgetId: Int,
        val spanX: Int = 4,
        val spanY: Int = 2
    ) : LauncherItem
}

@Immutable
data class HomeItemPlacement(
    val id: String,
    val item: LauncherItem,
    val page: Int,
    val row: Int,
    val col: Int
)

enum class PillMode {
    RECENTS,
    FAVORITES,
    FREQUENT
}

enum class DockScaleMode {
    COMPACT,
    STANDARD,
    EXPANDED
}

@Immutable
data class LauncherSettings(
    val gridRows: Int = 5,
    val gridCols: Int = 4,
    val iconSizeDp: Int = 56,
    val showLabels: Boolean = true,
    val dockApps: List<String> = emptyList(),
    val pillApps: List<String> = emptyList(),
    val pillMode: PillMode = PillMode.RECENTS,
    val dockScaleMode: DockScaleMode = DockScaleMode.STANDARD,
    val glassOpacity: Float = 0.70f,
    val cornerRadiusDp: Int = 24,
    val clock24Hour: Boolean = false,
    val showClockDate: Boolean = true,
    val pageCount: Int = 2,
    val defaultPage: Int = 0,
    val showPerimeterAnimation: Boolean = true,
    val firstRunCompleted: Boolean = false
)
