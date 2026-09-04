package com.example.data

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

class AppWidgetHelper(private val context: Context) {

    companion object {
        const val APPWIDGET_HOST_ID = 1024
        const val REQUEST_PICK_APPWIDGET = 101
        const val REQUEST_CREATE_APPWIDGET = 102
    }

    val appWidgetHost = AppWidgetHost(context, APPWIDGET_HOST_ID)
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)

    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {
            // Safe guard
        }
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {
            // Safe guard
        }
    }

    fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        try {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) {
            // Safe guard
        }
    }

    fun launchWidgetPicker(activity: Activity, appWidgetId: Int) {
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        activity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET)
    }

    fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(appWidgetId)
        } catch (_: Exception) {
            null
        }
    }

    fun createHostView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = getAppWidgetInfo(appWidgetId) ?: return null
        return try {
            appWidgetHost.createView(context, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        } catch (_: Exception) {
            null
        }
    }
}
