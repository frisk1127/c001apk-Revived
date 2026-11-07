package com.example.c001apk.compose.ui.collection

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.component.rememberHapticClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialCollectionSheet(
    state: FeedCollectionState,
    creating: Boolean,
    title: String,
    isPublic: Boolean,
    canSave: Boolean,
    onTitleChange: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onCreateMode: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val saving by rememberUpdatedState(state.saving)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !saving })
    val compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactKeyboard = compact && WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val haptic = rememberHapticClick {}
    val actionLabel = if (creating) "新建并收藏" else "保存"
    ModalBottomSheet(
        onDismissRequest = { if (!state.saving) onDismiss() }, sheetState = sheetState,
        sheetGesturesEnabled = !state.saving,
        dragHandle = if (compactKeyboard) null else ({ BottomSheetDefaults.DragHandle() }),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = rememberHapticClick(onClick = onBack), enabled = !state.saving) {
                    Icon(if (creating) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                        if (creating) "返回收藏夹列表" else "关闭")
                }
                Column(Modifier.weight(1f).padding(vertical = if (compactKeyboard) 4.dp else 8.dp)) {
                    Text(if (creating) "新建收藏夹" else "收藏到云端", style = MaterialTheme.typography.titleLarge)
                    if (!creating && state.loaded && !compact) Text("已选择 ${state.selectedIds.size} 个 · 可多选",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (compactKeyboard) TextButton(onClick = rememberHapticClick(onClick = onConfirm), enabled = canSave) { Text(actionLabel) }
            }
            if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = if (compactKeyboard) 4.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.loading) item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("正在加载收藏夹", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.error?.let { error -> item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Text(error, Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                } }
                if (!state.loaded && !state.loading) item {
                    OutlinedButton(onClick = rememberHapticClick(onClick = onRetry)) { Text("重试") }
                }
                if (state.loaded) {
                    if (creating) {
                        item {
                            OutlinedTextField(value = title, onValueChange = onTitleChange,
                                label = { Text("收藏夹名称") }, singleLine = true,
                                enabled = !state.saving, modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { if (canSave) onConfirm() }))
                        }
                        item {
                            ListItem(headlineContent = { Text("公开收藏夹") },
                                supportingContent = { Text("关闭后仅自己可见") },
                                trailingContent = { Switch(isPublic, onCheckedChange = null, enabled = !state.saving) },
                                modifier = Modifier.clip(MaterialTheme.shapes.medium).toggleable(isPublic,
                                    enabled = !state.saving, role = Role.Switch, onValueChange = { haptic(); onPublicChange(it) }),
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
                        }
                    } else {
                        if (state.collections.isEmpty()) item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.Folder, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("暂无收藏夹", style = MaterialTheme.typography.titleMedium)
                                Text("新建一个收藏夹，保存这条动态", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        items(state.collections, key = { requireNotNull(it.id) }) { folder ->
                            val checked = folder.id in state.selectedIds
                            ListItem(headlineContent = { Text(folder.title?.takeIf(String::isNotBlank) ?: "默认收藏夹") },
                                leadingContent = { Icon(Icons.Outlined.Folder, null) },
                                trailingContent = { Checkbox(checked, onCheckedChange = null, enabled = !state.saving) },
                                modifier = Modifier.clip(MaterialTheme.shapes.medium).toggleable(checked,
                                    enabled = !state.saving, role = Role.Checkbox, onValueChange = { haptic(); onToggle(requireNotNull(folder.id)) }),
                                colors = ListItemDefaults.colors(containerColor = if (checked) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow))
                        }
                    }
                }
            }
            if (!compactKeyboard && state.loaded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = if (compact) 8.dp else 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (creating) TextButton(onClick = rememberHapticClick(onClick = onBack), enabled = !state.saving) { Text("返回") }
                    else OutlinedButton(onClick = rememberHapticClick(onClick = onCreateMode), enabled = !state.saving) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("新建收藏夹")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = rememberHapticClick(onClick = onConfirm), enabled = canSave) {
                        Text(if (state.saving) "正在保存…" else actionLabel)
                    }
                }
            }
        }
    }
}
