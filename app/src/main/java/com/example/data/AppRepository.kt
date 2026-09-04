package com.example.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            refreshApps()
        }
    }

    init {
        registerPackageReceiver()
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(packageReceiver, filter)
        }
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (_: Exception) {
            // Already unregistered
        }
    }

    suspend fun loadApps(appStats: Map<String, Pair<Int, Long>> = emptyMap()) = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }

        val myPackage = context.packageName
        val appsList = ArrayList<AppInfo>(resolveInfos.size)

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == myPackage) continue // Don't list this launcher inside itself

            val activityName = resolveInfo.activityInfo.name
            val label = try {
                resolveInfo.loadLabel(packageManager).toString()
            } catch (_: Exception) {
                pkg
            }

            val iconDrawable = try {
                resolveInfo.loadIcon(packageManager)
            } catch (_: Exception) {
                null
            }

            val bitmap = iconDrawable?.let { drawableToBitmap(it) }
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and
                    android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            val stats = appStats[pkg]
            val launchCount = stats?.first ?: 0
            val lastLaunch = stats?.second ?: 0L

            appsList.add(
                AppInfo(
                    packageName = pkg,
                    activityName = activityName,
                    label = label,
                    iconBitmap = bitmap,
                    isSystemApp = isSystem,
                    launchCount = launchCount,
                    lastLaunchTime = lastLaunch
                )
            )
        }

        appsList.sortBy { it.label.lowercase(Locale.getDefault()) }
        _installedApps.value = appsList
    }

    fun refreshApps() {
        // Run in background coroutine if called from broadcast receiver
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            loadApps()
        }
    }

    fun launchApp(packageName: String, activityName: String? = null): Boolean {
        return try {
            val launchIntent = if (!activityName.isNullOrBlank()) {
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(packageName, activityName)
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
            } else {
                packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
            }

            if (launchIntent != null) {
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignore failure
        }
    }

    fun requestUninstall(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignore failure
        }
    }

    private fun drawableToBitmap(drawable: Drawable, targetSize: Int = 144): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, targetSize, targetSize, true)
        }

        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
