package com.aistudio.liquidglass.lnchr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.liquidglass.lnchr.model.AppInfo
import com.aistudio.liquidglass.lnchr.model.LauncherItem
import com.aistudio.liquidglass.lnchr.ui.glass.LiquidGlassSurface
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIconView(
    folder: LauncherItem.Folder,
    apps: List<AppInfo>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    showLabel: Boolean = true,
    textColor: Color = Color(0xFF1E293B)
) {
    val folderApps = remember(folder.packageNames, apps) {
        folder.packageNames.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
    }

    Column(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LiquidGlassSurface(
            modifier = Modifier.size(iconSize),
            cornerRadius = 18.dp,
            opacity = 0.72f,
            elevation = 5.dp
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                // 2x2 preview grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MiniAppIcon(app = folderApps.getOrNull(0), size = (iconSize - 16.dp) / 2)
                        MiniAppIcon(app = folderApps.getOrNull(1), size = (iconSize - 16.dp) / 2)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MiniAppIcon(app = folderApps.getOrNull(2), size = (iconSize - 16.dp) / 2)
                        MiniAppIcon(app = folderApps.getOrNull(3), size = (iconSize - 16.dp) / 2)
                    }
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = folder.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MiniAppIcon(app: AppInfo?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(if (app != null) Color.Transparent else Color(0x1AFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        if (app?.iconBitmap != null) {
            Image(
                bitmap = app.iconBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
fun FolderDialog(
    folder: LauncherItem.Folder,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onRenameFolder: (String) -> Unit,
    onRemoveAppFromFolder: (String) -> Unit,
    onDeleteFolder: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var folderNameText by remember(folder.name) { mutableStateOf(folder.name) }

    val folderApps = remember(folder.packageNames, apps) {
        folder.packageNames.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
    }

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 32.dp,
            opacity = 0.82f,
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = folderNameText,
                            onValueChange = { folderNameText = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = {
                                    onRenameFolder(folderNameText.ifBlank { "Folder" })
                                    isEditingName = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Done")
                                }
                            }
                        )
                    } else {
                        Text(
                            text = folder.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDeleteFolder) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Folder",
                            tint = Color(0xFFEF4444)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (folderApps.isEmpty()) {
                    Text(
                        text = "Folder is empty",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(folderApps, key = { it.packageName }) { app ->
                            AppIconView(
                                app = app,
                                onLaunch = {
                                    onDismiss()
                                    onLaunchApp(app.packageName)
                                },
                                onLongClick = {
                                    onRemoveAppFromFolder(app.packageName)
                                },
                                iconSize = 52.dp,
                                showLabel = true,
                                textColor = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Long-press an app to remove it from this folder",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
