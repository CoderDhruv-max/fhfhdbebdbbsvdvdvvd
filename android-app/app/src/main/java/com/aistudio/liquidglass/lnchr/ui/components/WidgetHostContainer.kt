package com.aistudio.liquidglass.lnchr.ui.components

import android.appwidget.AppWidgetHostView
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aistudio.liquidglass.lnchr.data.AppWidgetHelper
import com.aistudio.liquidglass.lnchr.model.LauncherItem
import com.aistudio.liquidglass.lnchr.ui.glass.LiquidGlassSurface
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetHostContainer(
    widgetItem: LauncherItem.Widget,
    widgetHelper: AppWidgetHelper,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hostView: AppWidgetHostView? = remember(widgetItem.appWidgetId) {
        widgetHelper.createHostView(context, widgetItem.appWidgetId)
    }

    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height((widgetItem.spanY * 85).dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        cornerRadius = 24.dp,
        opacity = 0.55f,
        elevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hostView != null) {
                AndroidView(
                    factory = { hostView },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Widget (#${widgetItem.appWidgetId})",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
        }
    }
}
