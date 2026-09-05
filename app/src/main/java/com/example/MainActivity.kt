package com.example

import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppWidgetHelper
import com.example.model.AppInfo
import com.example.model.LauncherItem
import com.example.ui.components.AppActionDialog
import com.example.ui.components.FolderDialog
import com.example.ui.drawer.AppDrawerSheet
import com.example.ui.home.HomeScreen
import com.example.ui.settings.CustomizationSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var widgetHelper: AppWidgetHelper
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val isDefaultLauncherState = mutableStateOf(false)

    // Activity Result Launcher for RoleManager default home app prompt
    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val isDefault = isDefaultLauncher(this)
        isDefaultLauncherState.value = isDefault
        if (isDefault) {
            viewModel.dismissDefaultPrompt()
            Toast.makeText(this, "Liquid Glass Launcher set as default home app!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetHelper = AppWidgetHelper(this)
        isDefaultLauncherState.value = isDefaultLauncher(this)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val isDefault by isDefaultLauncherState
                LauncherApp(
                    viewModel = viewModel,
                    widgetHelper = widgetHelper,
                    isDefaultLauncher = isDefault,
                    onOpenWallpaperPicker = { openWallpaperPicker(this) },
                    onAddWidget = { requestAddWidget() },
                    onOpenDefaultHomeSettings = { requestSetDefaultLauncher() },
                    onExpandNotifications = { expandNotifications(this) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshOnResume()
        val isDefault = isDefaultLauncher(this)
        isDefaultLauncherState.value = isDefault
        if (isDefault) {
            viewModel.dismissDefaultPrompt()
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHelper.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetHelper.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home button pressed: return to home screen, close drawer & dialogs
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.closeAppDrawer()
            viewModel.closeCustomization()
            viewModel.closeFolder()
            viewModel.closeAppAction()
            viewModel.closeWidgetAction()
        }
    }

    /**
     * Bulletproof, multi-tier launcher to set this app as Android's Default Home app.
     */
    fun requestSetDefaultLauncher() {
        if (isDefaultLauncher(this)) {
            Toast.makeText(this, "Liquid Glass Launcher is already your default home app!", Toast.LENGTH_SHORT).show()
            viewModel.dismissDefaultPrompt()
            return
        }

        // Tier 1: Modern Android 10+ (Q) RoleManager flow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    try {
                        roleRequestLauncher.launch(roleIntent)
                        return
                    } catch (_: Exception) {
                        // Fallback to settings intent if role launcher fails
                    }
                } else {
                    isDefaultLauncherState.value = true
                    viewModel.dismissDefaultPrompt()
                    return
                }
            }
        }

        // Tier 2: ACTION_HOME_SETTINGS
        try {
            val homeIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            Toast.makeText(this, "Select Liquid Glass Launcher as your Home app", Toast.LENGTH_LONG).show()
            return
        } catch (_: Exception) {}

        // Tier 3: ACTION_MANAGE_DEFAULT_APPS_SETTINGS
        try {
            val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(defaultAppsIntent)
            Toast.makeText(this, "Select Default apps > Home app > Liquid Glass Launcher", Toast.LENGTH_LONG).show()
            return
        } catch (_: Exception) {}

        // Tier 4: Home Chooser Intent
        try {
            val homeChooser = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(Intent.createChooser(homeChooser, "Select Default Home App").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        } catch (_: Exception) {}

        // Tier 5: Application Details Settings
        try {
            val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(detailsIntent)
            Toast.makeText(this, "Tap 'Home app' to set Liquid Glass Launcher as default", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(settingsIntent)
        }
    }

    private fun requestAddWidget() {
        val widgetId = widgetHelper.allocateAppWidgetId()
        pendingWidgetId = widgetId
        widgetHelper.launchWidgetPicker(this, widgetId)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AppWidgetHelper.REQUEST_PICK_APPWIDGET -> {
                if (resultCode == Activity.RESULT_OK) {
                    val appWidgetId = data?.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        pendingWidgetId
                    ) ?: pendingWidgetId

                    val appWidgetInfo = widgetHelper.getAppWidgetInfo(appWidgetId)
                    if (appWidgetInfo?.configure != null) {
                        // Launch configuration activity
                        val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                            component = appWidgetInfo.configure
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        try {
                            startActivityForResult(configIntent, AppWidgetHelper.REQUEST_CREATE_APPWIDGET)
                        } catch (_: Exception) {
                            viewModel.addWidget(appWidgetId)
                        }
                    } else {
                        viewModel.addWidget(appWidgetId)
                    }
                } else {
                    if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        widgetHelper.deleteAppWidgetId(pendingWidgetId)
                        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                    }
                }
            }

            AppWidgetHelper.REQUEST_CREATE_APPWIDGET -> {
                val appWidgetId = data?.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    pendingWidgetId
                ) ?: pendingWidgetId

                if (resultCode == Activity.RESULT_OK) {
                    viewModel.addWidget(appWidgetId)
                } else {
                    widgetHelper.deleteAppWidgetId(appWidgetId)
                }
            }
        }
    }

    companion object {
        fun isDefaultLauncher(context: Context): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(RoleManager::class.java)
                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                        return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                    }
                }
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.resolveActivity(
                        intent,
                        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                    )
                } else {
                    context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                }
                resolveInfo?.activityInfo?.packageName == context.packageName
            } catch (_: Exception) {
                false
            }
        }

        fun openWallpaperPicker(context: Context) {
            try {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                context.startActivity(Intent.createChooser(intent, "Choose Wallpaper"))
            } catch (_: Exception) {
                // Ignore if unavailable
            }
        }

        fun openDefaultHomeSettings(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        context.startActivity(intent)
                        return
                    }
                }
                val homeIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
            } catch (_: Exception) {
                try {
                    val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(defaultAppsIntent)
                } catch (_: Exception) {
                    val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                }
            }
        }

        fun expandNotifications(context: Context) {
            try {
                val statusBarService = context.getSystemService("statusbar")
                val statusBarManager = Class.forName("android.app.StatusBarManager")
                val method = statusBarManager.getMethod("expandNotificationsPanel")
                method.invoke(statusBarService)
            } catch (_: Exception) {
                // Fallback: silently ignore if restricted
            }
        }
    }
}

@Composable
fun LauncherApp(
    viewModel: LauncherViewModel,
    widgetHelper: AppWidgetHelper,
    isDefaultLauncher: Boolean,
    onOpenWallpaperPicker: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenDefaultHomeSettings: () -> Unit,
    onExpandNotifications: () -> Unit
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val homeItems by viewModel.homeItems.collectAsStateWithLifecycle()
    val dockApps by viewModel.dockApps.collectAsStateWithLifecycle()
    val pillApps by viewModel.pillApps.collectAsStateWithLifecycle()
    val frequentApps by viewModel.frequentApps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val drawerState by viewModel.drawerState.collectAsStateWithLifecycle()
    val newRecentAppEvent by viewModel.newRecentAppEvent.collectAsStateWithLifecycle()
    val isCustomizationOpen by viewModel.isCustomizationOpen.collectAsStateWithLifecycle()
    val activeFolder by viewModel.activeFolder.collectAsStateWithLifecycle()
    val selectedAppForAction by viewModel.selectedAppForAction.collectAsStateWithLifecycle()
    val selectedWidgetForAction by viewModel.selectedWidgetForAction.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Home Screen
        HomeScreen(
            apps = apps,
            homeItems = homeItems,
            dockApps = dockApps,
            pillApps = pillApps,
            frequentApps = frequentApps,
            settings = settings,
            widgetHelper = widgetHelper,
            isDefaultLauncher = isDefaultLauncher,
            drawerState = drawerState,
            newRecentAppEvent = newRecentAppEvent,
            onConsumeNewRecentAppEvent = { viewModel.consumeNewRecentAppEvent() },
            onLaunchApp = { packageName -> viewModel.launchApp(packageName) },
            onOpenAppDrawer = { viewModel.openAppDrawer() },
            onOpenCustomization = { viewModel.openCustomization() },
            onExpandNotifications = onExpandNotifications,
            onAppLongClick = { app, isOnHome -> viewModel.openAppAction(app, isOnHome) },
            onFolderClick = { folder -> viewModel.openFolder(folder) },
            onFolderLongClick = { folder -> viewModel.openFolder(folder) },
            onWidgetLongClick = { widget -> viewModel.openWidgetAction(widget) },
            onSetDefaultLauncher = onOpenDefaultHomeSettings,
            onDismissDefaultPrompt = { viewModel.dismissDefaultPrompt() },
            onReorderDock = { from, to -> viewModel.reorderDock(from, to) },
            onPinToDock = { app, index -> viewModel.pinToDock(app, index) },
            onUnpinFromDock = { pkg -> viewModel.unpinFromDock(pkg) },
            onUpdateDockScaleMode = { mode -> viewModel.setDockScaleMode(mode) },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture-driven Liquid Glass App Drawer
        AppDrawerSheet(
            drawerState = drawerState,
            onClose = { viewModel.closeAppDrawer() },
            apps = apps,
            settings = settings,
            onLaunchApp = { packageName -> viewModel.launchApp(packageName) },
            onAppLongClick = { app -> viewModel.openAppAction(app, false) },
            onDrawerStateChanged = { newState -> viewModel.setDrawerState(newState) }
        )

        // Customization Dialog
        if (isCustomizationOpen) {
            CustomizationSheet(
                settings = settings,
                isDefaultLauncher = isDefaultLauncher,
                onDismiss = { viewModel.closeCustomization() },
                onUpdateSettings = { newSettings -> viewModel.updateSettings(newSettings) },
                onOpenWallpaperPicker = {
                    viewModel.closeCustomization()
                    onOpenWallpaperPicker()
                },
                onAddWidget = {
                    viewModel.closeCustomization()
                    onAddWidget()
                },
                onOpenDefaultHomeSettings = {
                    viewModel.closeCustomization()
                    onOpenDefaultHomeSettings()
                }
            )
        }

        // Active Folder Expansion Dialog
        activeFolder?.let { folder ->
            FolderDialog(
                folder = folder,
                apps = apps,
                onDismiss = { viewModel.closeFolder() },
                onLaunchApp = { pkg -> viewModel.launchApp(pkg) },
                onRenameFolder = { newName -> viewModel.renameFolder(folder.id, newName) },
                onRemoveAppFromFolder = { pkg -> viewModel.removeAppFromFolder(folder.id, pkg) },
                onDeleteFolder = { viewModel.deleteFolder(folder.id) }
            )
        }

        // App Long-Press Action Sheet
        selectedAppForAction?.let { (app, isOnHome) ->
            val isInDock = settings.dockApps.contains(app.packageName)
            val isInPill = settings.pillApps.contains(app.packageName)

            AppActionDialog(
                app = app,
                isInDock = isInDock,
                isInPill = isInPill,
                isOnHome = isOnHome,
                onDismiss = { viewModel.closeAppAction() },
                onPinToHome = { viewModel.pinToHome(app) },
                onRemoveFromHome = { viewModel.removeFromHome(app) },
                onToggleDock = { viewModel.toggleDock(app) },
                onTogglePill = { viewModel.togglePill(app) },
                onCreateFolder = { viewModel.createFolder(app) },
                onOpenAppInfo = { viewModel.appRepository.openAppInfo(app.packageName) },
                onUninstall = { viewModel.appRepository.requestUninstall(app.packageName) }
            )
        }

        // Widget Long-Press Delete Dialog
        selectedWidgetForAction?.let { widget ->
            AlertDialog(
                onDismissRequest = { viewModel.closeWidgetAction() },
                title = { Text("Remove Widget") },
                text = { Text("Remove this widget from your home screen?") },
                confirmButton = {
                    TextButton(onClick = {
                        widgetHelper.deleteAppWidgetId(widget.appWidgetId)
                        viewModel.removeWidget(widget)
                    }) {
                        Text("Remove", color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeWidgetAction() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
