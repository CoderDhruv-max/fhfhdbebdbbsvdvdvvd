package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DockScaleMode
import com.example.model.LauncherSettings
import com.example.model.PillMode
import com.example.ui.glass.LiquidGlassSurface

@Composable
fun CustomizationSheet(
    settings: LauncherSettings,
    isDefaultLauncher: Boolean,
    onDismiss: () -> Unit,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenDefaultHomeSettings: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("General", "Layout", "Glass", "Clock")

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            cornerRadius = 32.dp,
            opacity = 0.92f,
            elevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Liquid Glass",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Launcher Customization",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x14000000))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        val selected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { activeTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Color(0xFF1E293B) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (activeTab) {
                        0 -> GeneralTab(
                            isDefaultLauncher = isDefaultLauncher,
                            onOpenDefaultHomeSettings = onOpenDefaultHomeSettings,
                            onOpenWallpaperPicker = onOpenWallpaperPicker,
                            onAddWidget = onAddWidget,
                            settings = settings,
                            onUpdateSettings = onUpdateSettings
                        )
                        1 -> LayoutTab(
                            settings = settings,
                            onUpdateSettings = onUpdateSettings
                        )
                        2 -> GlassTab(
                            settings = settings,
                            onUpdateSettings = onUpdateSettings
                        )
                        3 -> ClockTab(
                            settings = settings,
                            onUpdateSettings = onUpdateSettings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralTab(
    isDefaultLauncher: Boolean,
    onOpenDefaultHomeSettings: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onAddWidget: () -> Unit,
    settings: LauncherSettings,
    onUpdateSettings: (LauncherSettings) -> Unit
) {
    // Default Home Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isDefaultLauncher) Color(0x1F10B981) else Color(0x1FE0E7FF))
            .border(
                1.dp,
                if (isDefaultLauncher) Color(0x4D10B981) else Color(0x4D6366F1),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDefaultLauncher) Icons.Default.Check else Icons.Default.Home,
                    contentDescription = null,
                    tint = if (isDefaultLauncher) Color(0xFF059669) else Color(0xFF4F46E5),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDefaultLauncher) "Default Home App Active" else "Set as Default Home",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDefaultLauncher) Color(0xFF059669) else Color(0xFF4F46E5)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isDefaultLauncher)
                    "Liquid Glass Launcher is your primary Android home experience."
                else
                    "Choose Liquid Glass Launcher in Android Default apps to make it open on Home button.",
                fontSize = 12.sp,
                color = Color(0xFF475569)
            )
            if (!isDefaultLauncher) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenDefaultHomeSettings,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("Select Default Home App", fontSize = 13.sp)
                }
            }
        }
    }

    // Wallpaper action
    SettingActionCard(
        icon = Icons.Default.Wallpaper,
        title = "Change System Wallpaper",
        description = "Pick wallpaper using Android's system wallpaper picker",
        onClick = onOpenWallpaperPicker
    )

    // Add Widget action
    SettingActionCard(
        icon = Icons.Default.Widgets,
        title = "Add Android Widget",
        description = "Place functional real Android app widgets on Home",
        onClick = onAddWidget
    )

    // Pill Mode Selection
    Text(
        text = "Floating Pill Mode",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1E293B)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PillMode.entries.forEach { mode ->
            val selected = settings.pillMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color(0xFF334155) else Color(0x14000000))
                    .clickable { onUpdateSettings(settings.copy(pillMode = mode)) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) Color.White else Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
private fun LayoutTab(
    settings: LauncherSettings,
    onUpdateSettings: (LauncherSettings) -> Unit
) {
    // Grid Rows
    SettingSliderRow(
        title = "Grid Rows: ${settings.gridRows}",
        value = settings.gridRows.toFloat(),
        range = 4f..7f,
        steps = 2,
        onValueChange = { onUpdateSettings(settings.copy(gridRows = it.toInt())) }
    )

    // Grid Columns
    SettingSliderRow(
        title = "Grid Columns: ${settings.gridCols}",
        value = settings.gridCols.toFloat(),
        range = 3f..6f,
        steps = 2,
        onValueChange = { onUpdateSettings(settings.copy(gridCols = it.toInt())) }
    )

    // Icon Size
    SettingSliderRow(
        title = "Icon Size: ${settings.iconSizeDp}dp",
        value = settings.iconSizeDp.toFloat(),
        range = 44f..68f,
        steps = 5,
        onValueChange = { onUpdateSettings(settings.copy(iconSizeDp = it.toInt())) }
    )

    // Home Pages
    SettingSliderRow(
        title = "Home Pages: ${settings.pageCount}",
        value = settings.pageCount.toFloat(),
        range = 1f..4f,
        steps = 2,
        onValueChange = { onUpdateSettings(settings.copy(pageCount = it.toInt())) }
    )

    // Show App Labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Show App Labels", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
            Text("Display names under icons", fontSize = 12.sp, color = Color(0xFF64748B))
        }
        Switch(
            checked = settings.showLabels,
            onCheckedChange = { onUpdateSettings(settings.copy(showLabels = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF334155))
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Bottom Dock Size Mode
    Column {
        Text(
            text = "Bottom Dock Size",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
        Text(
            text = "Controls dock scale and icon dimensions",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x14000000))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DockScaleMode.entries.forEach { mode ->
                val selected = settings.dockScaleMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFF334155) else Color.Transparent)
                        .clickable { onUpdateSettings(settings.copy(dockScaleMode = mode)) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else Color(0xFF475569)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTab(
    settings: LauncherSettings,
    onUpdateSettings: (LauncherSettings) -> Unit
) {
    // Glass Opacity
    SettingSliderRow(
        title = "Glass Opacity: ${(settings.glassOpacity * 100).toInt()}%",
        value = settings.glassOpacity,
        range = 0.50f..0.88f,
        steps = 7,
        onValueChange = { onUpdateSettings(settings.copy(glassOpacity = it)) }
    )

    // Corner Radius
    SettingSliderRow(
        title = "Corner Radius: ${settings.cornerRadiusDp}dp",
        value = settings.cornerRadiusDp.toFloat(),
        range = 16f..36f,
        steps = 9,
        onValueChange = { onUpdateSettings(settings.copy(cornerRadiusDp = it.toInt())) }
    )

    // Perimeter Highlight
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Perimeter Trace Effect", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
            Text("Clockwise subtle perimeter line on interaction", fontSize = 12.sp, color = Color(0xFF64748B))
        }
        Switch(
            checked = settings.showPerimeterAnimation,
            onCheckedChange = { onUpdateSettings(settings.copy(showPerimeterAnimation = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF334155))
        )
    }
}

@Composable
private fun ClockTab(
    settings: LauncherSettings,
    onUpdateSettings: (LauncherSettings) -> Unit
) {
    // 24 Hour Toggle
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("24-Hour Time Format", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
            Text("Display time in 24-hour mode (e.g. 14:30)", fontSize = 12.sp, color = Color(0xFF64748B))
        }
        Switch(
            checked = settings.clock24Hour,
            onCheckedChange = { onUpdateSettings(settings.copy(clock24Hour = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF334155))
        )
    }

    // Show Date Toggle
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Show Day & Date", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
            Text("Display day and date below the minimal clock", fontSize = 12.sp, color = Color(0xFF64748B))
        }
        Switch(
            checked = settings.showClockDate,
            onCheckedChange = { onUpdateSettings(settings.copy(showClockDate = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF334155))
        )
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF334155),
                activeTrackColor = Color(0xFF64748B)
            )
        )
    }
}

@Composable
private fun SettingActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0F000000))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x1A000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Text(description, fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}
