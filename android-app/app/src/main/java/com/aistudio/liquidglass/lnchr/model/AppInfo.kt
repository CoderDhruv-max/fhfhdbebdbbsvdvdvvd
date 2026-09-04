package com.aistudio.liquidglass.lnchr.model

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconBitmap: Bitmap? = null,
    val isSystemApp: Boolean = false,
    val launchCount: Int = 0,
    val lastLaunchTime: Long = 0L
) {
    val componentKey: String
        get() = "$packageName/$activityName"
}
