package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppInfo
import com.example.ui.glass.LiquidGlassSurface

@Composable
fun AppActionDialog(
    app: AppInfo,
    isInDock: Boolean,
    isInPill: Boolean,
    isOnHome: Boolean,
    onDismiss: () -> Unit,
    onPinToHome: () -> Unit,
    onRemoveFromHome: () -> Unit,
    onToggleDock: () -> Unit,
    onTogglePill: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 28.dp,
            opacity = 0.85f,
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: App info preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    AppIconView(
                        app = app,
                        onLaunch = {},
                        onLongClick = {},
                        iconSize = 44.dp,
                        showLabel = false
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.label,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = app.packageName,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Options
                if (isOnHome) {
                    ActionRow(
                        icon = Icons.Default.Delete,
                        title = "Remove from Home",
                        tint = Color(0xFFEF4444),
                        onClick = {
                            onRemoveFromHome()
                            onDismiss()
                        }
                    )
                } else {
                    ActionRow(
                        icon = Icons.Default.PushPin,
                        title = "Add to Home Screen",
                        onClick = {
                            onPinToHome()
                            onDismiss()
                        }
                    )
                }

                ActionRow(
                    icon = Icons.Default.Star,
                    title = if (isInDock) "Remove from Dock" else "Pin to Dock",
                    onClick = {
                        onToggleDock()
                        onDismiss()
                    }
                )

                ActionRow(
                    icon = Icons.Default.Widgets,
                    title = if (isInPill) "Remove from Pill" else "Pin to Pill",
                    onClick = {
                        onTogglePill()
                        onDismiss()
                    }
                )

                ActionRow(
                    icon = Icons.Default.CreateNewFolder,
                    title = "Create New Folder",
                    onClick = {
                        onCreateFolder()
                        onDismiss()
                    }
                )

                ActionRow(
                    icon = Icons.Default.Info,
                    title = "App Info",
                    onClick = {
                        onOpenAppInfo()
                        onDismiss()
                    }
                )

                if (!app.isSystemApp) {
                    ActionRow(
                        icon = Icons.Default.Delete,
                        title = "Uninstall",
                        tint = Color(0xFFEF4444),
                        onClick = {
                            onUninstall()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = Color(0xFF334155)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}
