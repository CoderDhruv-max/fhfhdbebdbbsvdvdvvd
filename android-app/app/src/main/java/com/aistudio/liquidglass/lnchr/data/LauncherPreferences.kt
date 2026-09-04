package com.aistudio.liquidglass.lnchr.data

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.liquidglass.lnchr.model.DockScaleMode
import com.aistudio.liquidglass.lnchr.model.HomeItemPlacement
import com.aistudio.liquidglass.lnchr.model.LauncherItem
import com.aistudio.liquidglass.lnchr.model.LauncherSettings
import com.aistudio.liquidglass.lnchr.model.PillMode
import org.json.JSONArray
import org.json.JSONObject

class LauncherPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("liquid_glass_launcher_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GRID_ROWS = "grid_rows"
        private const val KEY_GRID_COLS = "grid_cols"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_SHOW_LABELS = "show_labels"
        private const val KEY_GLASS_OPACITY = "glass_opacity"
        private const val KEY_CORNER_RADIUS = "corner_radius"
        private const val KEY_CLOCK_24H = "clock_24h"
        private const val KEY_SHOW_CLOCK_DATE = "show_clock_date"
        private const val KEY_PAGE_COUNT = "page_count"
        private const val KEY_DEFAULT_PAGE = "default_page"
        private const val KEY_PERIMETER_ANIM = "show_perimeter_anim"
        private const val KEY_FIRST_RUN_DONE = "first_run_done"
        private const val KEY_PILL_MODE = "pill_mode"
        private const val KEY_DOCK_SCALE_MODE = "dock_scale_mode"
        private const val KEY_DOCK_APPS = "dock_apps_json"
        private const val KEY_PILL_APPS = "pill_apps_json"
        private const val KEY_HOME_ITEMS = "home_items_json"
        private const val KEY_APP_STATS = "app_launch_stats_json"
    }

    fun loadSettings(): LauncherSettings {
        val pillModeStr = prefs.getString(KEY_PILL_MODE, PillMode.RECENTS.name)
        val pillMode = try {
            PillMode.valueOf(pillModeStr ?: PillMode.RECENTS.name)
        } catch (_: Exception) {
            PillMode.RECENTS
        }

        val dockScaleStr = prefs.getString(KEY_DOCK_SCALE_MODE, DockScaleMode.STANDARD.name)
        val dockScaleMode = try {
            DockScaleMode.valueOf(dockScaleStr ?: DockScaleMode.STANDARD.name)
        } catch (_: Exception) {
            DockScaleMode.STANDARD
        }

        return LauncherSettings(
            gridRows = prefs.getInt(KEY_GRID_ROWS, 5),
            gridCols = prefs.getInt(KEY_GRID_COLS, 4),
            iconSizeDp = prefs.getInt(KEY_ICON_SIZE, 56),
            showLabels = prefs.getBoolean(KEY_SHOW_LABELS, true),
            dockApps = loadStringList(KEY_DOCK_APPS),
            pillApps = loadStringList(KEY_PILL_APPS),
            pillMode = pillMode,
            dockScaleMode = dockScaleMode,
            glassOpacity = prefs.getFloat(KEY_GLASS_OPACITY, 0.70f),
            cornerRadiusDp = prefs.getInt(KEY_CORNER_RADIUS, 24),
            clock24Hour = prefs.getBoolean(KEY_CLOCK_24H, false),
            showClockDate = prefs.getBoolean(KEY_SHOW_CLOCK_DATE, true),
            pageCount = prefs.getInt(KEY_PAGE_COUNT, 2),
            defaultPage = prefs.getInt(KEY_DEFAULT_PAGE, 0),
            showPerimeterAnimation = prefs.getBoolean(KEY_PERIMETER_ANIM, true),
            firstRunCompleted = prefs.getBoolean(KEY_FIRST_RUN_DONE, false)
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        prefs.edit()
            .putInt(KEY_GRID_ROWS, settings.gridRows)
            .putInt(KEY_GRID_COLS, settings.gridCols)
            .putInt(KEY_ICON_SIZE, settings.iconSizeDp)
            .putBoolean(KEY_SHOW_LABELS, settings.showLabels)
            .putFloat(KEY_GLASS_OPACITY, settings.glassOpacity)
            .putInt(KEY_CORNER_RADIUS, settings.cornerRadiusDp)
            .putBoolean(KEY_CLOCK_24H, settings.clock24Hour)
            .putBoolean(KEY_SHOW_CLOCK_DATE, settings.showClockDate)
            .putInt(KEY_PAGE_COUNT, settings.pageCount)
            .putInt(KEY_DEFAULT_PAGE, settings.defaultPage)
            .putBoolean(KEY_PERIMETER_ANIM, settings.showPerimeterAnimation)
            .putBoolean(KEY_FIRST_RUN_DONE, settings.firstRunCompleted)
            .putString(KEY_PILL_MODE, settings.pillMode.name)
            .putString(KEY_DOCK_SCALE_MODE, settings.dockScaleMode.name)
            .apply()

        saveStringList(KEY_DOCK_APPS, settings.dockApps)
        saveStringList(KEY_PILL_APPS, settings.pillApps)
    }

    fun setFirstRunCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_RUN_DONE, completed).apply()
    }

    fun saveDockApps(apps: List<String>) {
        saveStringList(KEY_DOCK_APPS, apps)
    }

    fun savePillApps(apps: List<String>) {
        saveStringList(KEY_PILL_APPS, apps)
    }

    fun saveHomeItems(items: List<HomeItemPlacement>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("page", item.page)
                put("row", item.row)
                put("col", item.col)
                when (val launcherItem = item.item) {
                    is LauncherItem.App -> {
                        put("type", "app")
                        put("packageName", launcherItem.packageName)
                        put("activityName", launcherItem.activityName ?: "")
                    }
                    is LauncherItem.Folder -> {
                        put("type", "folder")
                        put("folderId", launcherItem.id)
                        put("folderName", launcherItem.name)
                        val appsArr = JSONArray()
                        launcherItem.packageNames.forEach { appsArr.put(it) }
                        put("folderApps", appsArr)
                    }
                    is LauncherItem.Widget -> {
                        put("type", "widget")
                        put("widgetId", launcherItem.id)
                        put("appWidgetId", launcherItem.appWidgetId)
                        put("spanX", launcherItem.spanX)
                        put("spanY", launcherItem.spanY)
                    }
                }
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HOME_ITEMS, array.toString()).apply()
    }

    fun loadHomeItems(): List<HomeItemPlacement> {
        val jsonStr = prefs.getString(KEY_HOME_ITEMS, null) ?: return emptyList()
        val result = mutableListOf<HomeItemPlacement>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val page = obj.getInt("page")
                val row = obj.getInt("row")
                val col = obj.getInt("col")
                val type = obj.optString("type", "app")

                val launcherItem: LauncherItem? = when (type) {
                    "app" -> {
                        val pkg = obj.getString("packageName")
                        val act = obj.optString("activityName", "").ifBlank { null }
                        LauncherItem.App(id = id, packageName = pkg, activityName = act)
                    }
                    "folder" -> {
                        val folderId = obj.optString("folderId", id)
                        val folderName = obj.optString("folderName", "Folder")
                        val appsArr = obj.optJSONArray("folderApps")
                        val packageNames = mutableListOf<String>()
                        if (appsArr != null) {
                            for (j in 0 until appsArr.length()) {
                                packageNames.add(appsArr.getString(j))
                            }
                        }
                        LauncherItem.Folder(id = folderId, name = folderName, packageNames = packageNames)
                    }
                    "widget" -> {
                        val appWidgetId = obj.getInt("appWidgetId")
                        val spanX = obj.optInt("spanX", 4)
                        val spanY = obj.optInt("spanY", 2)
                        LauncherItem.Widget(id = id, appWidgetId = appWidgetId, spanX = spanX, spanY = spanY)
                    }
                    else -> null
                }

                if (launcherItem != null) {
                    result.add(HomeItemPlacement(id, launcherItem, page, row, col))
                }
            }
        } catch (_: Exception) {
            // Return empty list on parse failure
        }
        return result
    }

    fun recordAppLaunch(packageName: String) {
        val stats = loadAppLaunchStats().toMutableMap()
        val current = stats[packageName] ?: (0 to 0L)
        stats[packageName] = Pair(current.first + 1, System.currentTimeMillis())

        val obj = JSONObject()
        for ((pkg, pair) in stats) {
            val item = JSONObject().apply {
                put("count", pair.first)
                put("lastLaunch", pair.second)
            }
            obj.put(pkg, item)
        }
        prefs.edit().putString(KEY_APP_STATS, obj.toString()).apply()
    }

    fun loadAppLaunchStats(): Map<String, Pair<Int, Long>> {
        val jsonStr = prefs.getString(KEY_APP_STATS, null) ?: return emptyMap()
        val result = mutableMapOf<String, Pair<Int, Long>>()
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = obj.getJSONObject(key)
                val count = item.getInt("count")
                val lastLaunch = item.getLong("lastLaunch")
                result[key] = Pair(count, lastLaunch)
            }
        } catch (_: Exception) {
            // Ignore parse errors
        }
        return result
    }

    private fun loadStringList(key: String): List<String> {
        val jsonStr = prefs.getString(key, null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {
            // Ignore
        }
        return list
    }

    private fun saveStringList(key: String, list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
