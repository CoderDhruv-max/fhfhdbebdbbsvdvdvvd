package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.LauncherPreferences
import com.example.model.AppInfo
import com.example.model.DockScaleMode
import com.example.model.HomeItemPlacement
import com.example.model.LauncherDrawerState
import com.example.model.LauncherItem
import com.example.model.LauncherSettings
import com.example.model.PillMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class NewRecentAppEvent(
    val packageName: String,
    val trigger: Long = System.currentTimeMillis()
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val appRepository = AppRepository(application)
    val preferences = LauncherPreferences(application)

    private val _settings = MutableStateFlow(preferences.loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _homeItems = MutableStateFlow<List<HomeItemPlacement>>(emptyList())
    val homeItems: StateFlow<List<HomeItemPlacement>> = _homeItems.asStateFlow()

    val installedApps: StateFlow<List<AppInfo>> = appRepository.installedApps

    private val _drawerState = MutableStateFlow(LauncherDrawerState.HOME)
    val drawerState: StateFlow<LauncherDrawerState> = _drawerState.asStateFlow()

    private val _isAppDrawerOpen = MutableStateFlow(false)
    val isAppDrawerOpen: StateFlow<Boolean> = _isAppDrawerOpen.asStateFlow()

    private val _recentPackages = MutableStateFlow<List<String>>(preferences.loadRecentPackages())
    val recentPackages: StateFlow<List<String>> = _recentPackages.asStateFlow()

    private val _newRecentAppEvent = MutableStateFlow<NewRecentAppEvent?>(null)
    val newRecentAppEvent: StateFlow<NewRecentAppEvent?> = _newRecentAppEvent.asStateFlow()

    private val _isCustomizationOpen = MutableStateFlow(false)
    val isCustomizationOpen: StateFlow<Boolean> = _isCustomizationOpen.asStateFlow()

    private val _activeFolder = MutableStateFlow<LauncherItem.Folder?>(null)
    val activeFolder: StateFlow<LauncherItem.Folder?> = _activeFolder.asStateFlow()

    private val _selectedAppForAction = MutableStateFlow<Pair<AppInfo, Boolean>?>(null)
    val selectedAppForAction: StateFlow<Pair<AppInfo, Boolean>?> = _selectedAppForAction.asStateFlow()

    private val _selectedWidgetForAction = MutableStateFlow<LauncherItem.Widget?>(null)
    val selectedWidgetForAction: StateFlow<LauncherItem.Widget?> = _selectedWidgetForAction.asStateFlow()

    // Derived Dock Apps
    val dockApps: StateFlow<List<AppInfo>> = combine(installedApps, settings) { apps, currentSettings ->
        if (currentSettings.dockApps.isNotEmpty()) {
            currentSettings.dockApps.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
        } else {
            // Default smart dock: find common apps or first 4 installed apps
            val defaults = apps.filter {
                val p = it.packageName.lowercase()
                p.contains("dialer") || p.contains("phone") ||
                        p.contains("messaging") || p.contains("mms") ||
                        p.contains("chrome") || p.contains("browser") ||
                        p.contains("camera")
            }.take(5)
            if (defaults.isNotEmpty()) defaults else apps.take(4)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived Pill Apps (Recents / Frequent / Favorites)
    val pillApps: StateFlow<List<AppInfo>> = combine(installedApps, settings, _recentPackages) { apps, currentSettings, recentsList ->
        when (currentSettings.pillMode) {
            PillMode.RECENTS -> {
                val realRecents = recentsList.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
                if (realRecents.isNotEmpty()) {
                    val remaining = apps.filter { app -> realRecents.none { it.packageName == app.packageName } }
                    (realRecents + remaining).take(5)
                } else {
                    apps.take(4)
                }
            }
            PillMode.FREQUENT -> {
                val sorted = apps.filter { it.launchCount > 0 }
                    .sortedByDescending { it.launchCount }
                    .take(5)
                if (sorted.isNotEmpty()) sorted else apps.take(4)
            }
            PillMode.FAVORITES -> {
                currentSettings.pillApps.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived Frequent Apps (Sorted by launch count and last launch time)
    val frequentApps: StateFlow<List<AppInfo>> = installedApps.combine(settings) { apps, _ ->
        apps.sortedWith(
            compareByDescending<AppInfo> { it.launchCount }
                .thenByDescending { it.lastLaunchTime }
        ).take(12)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val stats = preferences.loadAppLaunchStats()
            appRepository.loadApps(stats)

            val loadedHomeItems = preferences.loadHomeItems()
            if (loadedHomeItems.isEmpty()) {
                // Initialize default home page with first few apps
                val defaultItems = createInitialHomeItems()
                _homeItems.value = defaultItems
                preferences.saveHomeItems(defaultItems)
            } else {
                _homeItems.value = loadedHomeItems
            }
        }
    }

    private fun createInitialHomeItems(): List<HomeItemPlacement> {
        val apps = installedApps.value
        val items = mutableListOf<HomeItemPlacement>()
        var row = 0
        var col = 0
        for (app in apps.take(12)) {
            items.add(
                HomeItemPlacement(
                    id = UUID.randomUUID().toString(),
                    item = LauncherItem.App(
                        id = UUID.randomUUID().toString(),
                        packageName = app.packageName,
                        activityName = app.activityName
                    ),
                    page = 0,
                    row = row,
                    col = col
                )
            )
            col++
            if (col >= 4) {
                col = 0
                row++
            }
        }
        return items
    }

    fun launchApp(packageName: String, activityName: String? = null) {
        val currentPillPackages = pillApps.value.map { it.packageName }
        val isNewToPill = currentPillPackages.isNotEmpty() && !currentPillPackages.contains(packageName)

        // 1. Move to index 0 of recent packages
        val currentRecents = _recentPackages.value.filter { it != packageName }.toMutableList()
        currentRecents.add(0, packageName)
        val trimmed = currentRecents.take(15)
        _recentPackages.value = trimmed
        preferences.saveRecentPackages(trimmed)

        // 2. Trigger New App White Perimeter Stroke ONLY if genuinely new to the pill
        if (isNewToPill) {
            _newRecentAppEvent.value = NewRecentAppEvent(
                packageName = packageName,
                trigger = System.currentTimeMillis()
            )
        }

        // 3. Record launch in preferences and fire intent
        preferences.recordAppLaunch(packageName)
        appRepository.launchApp(packageName, activityName)

        // 4. Update in-memory app list immediately to reflect instant launch stats
        appRepository.recordLaunch(packageName)
    }

    fun consumeNewRecentAppEvent() {
        _newRecentAppEvent.value = null
    }

    fun openAppDrawer() {
        _drawerState.value = LauncherDrawerState.DRAWER_OPENING
        _isAppDrawerOpen.value = true
    }

    fun closeAppDrawer() {
        _drawerState.value = LauncherDrawerState.DRAWER_CLOSING
        _isAppDrawerOpen.value = false
    }

    fun setDrawerState(state: LauncherDrawerState) {
        _drawerState.value = state
        _isAppDrawerOpen.value = (state != LauncherDrawerState.HOME)
    }

    fun refreshOnResume() {
        // Guarantee drawer state is cleanly reset if it was in the middle of closing
        if (_drawerState.value == LauncherDrawerState.DRAWER_CLOSING) {
            _drawerState.value = LauncherDrawerState.HOME
            _isAppDrawerOpen.value = false
        }
        val stats = preferences.loadAppLaunchStats()
        viewModelScope.launch {
            appRepository.loadApps(stats)
        }
    }

    fun openCustomization() {
        _isCustomizationOpen.value = true
    }

    fun closeCustomization() {
        _isCustomizationOpen.value = false
    }

    fun openFolder(folder: LauncherItem.Folder) {
        _activeFolder.value = folder
    }

    fun closeFolder() {
        _activeFolder.value = null
    }

    fun openAppAction(app: AppInfo, isOnHome: Boolean) {
        _selectedAppForAction.value = Pair(app, isOnHome)
    }

    fun closeAppAction() {
        _selectedAppForAction.value = null
    }

    fun openWidgetAction(widget: LauncherItem.Widget) {
        _selectedWidgetForAction.value = widget
    }

    fun closeWidgetAction() {
        _selectedWidgetForAction.value = null
    }

    fun updateSettings(newSettings: LauncherSettings) {
        _settings.value = newSettings
        preferences.saveSettings(newSettings)
    }

    fun dismissDefaultPrompt() {
        val updated = _settings.value.copy(firstRunCompleted = true)
        updateSettings(updated)
        preferences.setFirstRunCompleted(true)
    }

    fun pinToHome(appInfo: AppInfo) {
        val current = _homeItems.value.toMutableList()
        val nextIndex = current.size
        val page = 0
        val row = nextIndex / settings.value.gridCols
        val col = nextIndex % settings.value.gridCols

        val newItem = HomeItemPlacement(
            id = UUID.randomUUID().toString(),
            item = LauncherItem.App(
                id = UUID.randomUUID().toString(),
                packageName = appInfo.packageName,
                activityName = appInfo.activityName
            ),
            page = page,
            row = row,
            col = col
        )
        current.add(newItem)
        _homeItems.value = current
        preferences.saveHomeItems(current)
    }

    fun removeFromHome(appInfo: AppInfo) {
        val current = _homeItems.value.filterNot { placement ->
            when (val item = placement.item) {
                is LauncherItem.App -> item.packageName == appInfo.packageName
                else -> false
            }
        }
        _homeItems.value = current
        preferences.saveHomeItems(current)
    }

    fun toggleDock(appInfo: AppInfo) {
        val currentDock = settings.value.dockApps.toMutableList()
        if (currentDock.contains(appInfo.packageName)) {
            currentDock.remove(appInfo.packageName)
        } else {
            if (currentDock.size < 7) {
                currentDock.add(appInfo.packageName)
            }
        }
        val updated = settings.value.copy(dockApps = currentDock)
        updateSettings(updated)
    }

    fun pinToDock(appInfo: AppInfo, targetIndex: Int? = null) {
        val current = settings.value.dockApps.toMutableList()
        current.remove(appInfo.packageName) // Re-position if already in dock
        if (targetIndex != null && targetIndex in 0..current.size) {
            current.add(targetIndex, appInfo.packageName)
        } else {
            current.add(appInfo.packageName)
        }
        val limited = current.take(7)
        val updated = settings.value.copy(dockApps = limited)
        updateSettings(updated)
    }

    fun unpinFromDock(packageName: String) {
        val current = settings.value.dockApps.filterNot { it == packageName }
        val updated = settings.value.copy(dockApps = current)
        updateSettings(updated)
    }

    fun reorderDock(fromIndex: Int, toIndex: Int) {
        val current = settings.value.dockApps.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            val updated = settings.value.copy(dockApps = current)
            updateSettings(updated)
        }
    }

    fun setDockScaleMode(mode: DockScaleMode) {
        val updated = settings.value.copy(dockScaleMode = mode)
        updateSettings(updated)
    }

    fun togglePill(appInfo: AppInfo) {
        val currentPill = settings.value.pillApps.toMutableList()
        if (currentPill.contains(appInfo.packageName)) {
            currentPill.remove(appInfo.packageName)
        } else {
            if (currentPill.size < 6) {
                currentPill.add(appInfo.packageName)
            }
        }
        val updated = settings.value.copy(pillApps = currentPill, pillMode = PillMode.FAVORITES)
        updateSettings(updated)
    }

    fun createFolder(appInfo: AppInfo) {
        val folderId = UUID.randomUUID().toString()
        val folder = LauncherItem.Folder(
            id = folderId,
            name = "Folder",
            packageNames = listOf(appInfo.packageName)
        )
        val placement = HomeItemPlacement(
            id = UUID.randomUUID().toString(),
            item = folder,
            page = 0,
            row = (_homeItems.value.size / settings.value.gridCols),
            col = (_homeItems.value.size % settings.value.gridCols)
        )
        val updated = _homeItems.value + placement
        _homeItems.value = updated
        preferences.saveHomeItems(updated)
    }

    fun renameFolder(folderId: String, newName: String) {
        val updated = _homeItems.value.map { placement ->
            if (placement.item is LauncherItem.Folder && placement.item.id == folderId) {
                placement.copy(item = placement.item.copy(name = newName))
            } else {
                placement
            }
        }
        _homeItems.value = updated
        preferences.saveHomeItems(updated)
        val active = _activeFolder.value
        if (active?.id == folderId) {
            _activeFolder.value = active.copy(name = newName)
        }
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        val updated = _homeItems.value.mapNotNull { placement ->
            if (placement.item is LauncherItem.Folder && placement.item.id == folderId) {
                val remaining = placement.item.packageNames - packageName
                if (remaining.isEmpty()) {
                    null // delete empty folder
                } else {
                    placement.copy(item = placement.item.copy(packageNames = remaining))
                }
            } else {
                placement
            }
        }
        _homeItems.value = updated
        preferences.saveHomeItems(updated)
        val active = _activeFolder.value
        if (active?.id == folderId) {
            val remaining = active.packageNames - packageName
            if (remaining.isEmpty()) {
                _activeFolder.value = null
            } else {
                _activeFolder.value = active.copy(packageNames = remaining)
            }
        }
    }

    fun deleteFolder(folderId: String) {
        val updated = _homeItems.value.filterNot {
            it.item is LauncherItem.Folder && it.item.id == folderId
        }
        _homeItems.value = updated
        preferences.saveHomeItems(updated)
        _activeFolder.value = null
    }

    fun addWidget(appWidgetId: Int) {
        val widgetId = UUID.randomUUID().toString()
        val widget = LauncherItem.Widget(
            id = widgetId,
            appWidgetId = appWidgetId,
            spanX = settings.value.gridCols,
            spanY = 2
        )
        val placement = HomeItemPlacement(
            id = widgetId,
            item = widget,
            page = 0,
            row = 0,
            col = 0
        )
        val updated = listOf(placement) + _homeItems.value
        _homeItems.value = updated
        preferences.saveHomeItems(updated)
    }

    fun removeWidget(widget: LauncherItem.Widget) {
        val updated = _homeItems.value.filterNot {
            it.item is LauncherItem.Widget && it.item.id == widget.id
        }
        _homeItems.value = updated
        preferences.saveHomeItems(updated)
        _selectedWidgetForAction.value = null
    }

    override fun onCleared() {
        super.onCleared()
        appRepository.unregisterReceiver()
    }
}
