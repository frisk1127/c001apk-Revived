package com.example.c001apk.compose.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.c001apk.compose.ui.component.design.AlertDialog
import com.example.c001apk.compose.ui.component.design.DialogActions
import com.example.c001apk.compose.ui.component.design.GlobalPopupBlur
import com.example.c001apk.compose.ui.component.design.StyleButton
import com.example.c001apk.compose.ui.component.design.Text
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.theme.isMiuix
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun DeleteCollectionDialog(
    title: String,
    deleting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val message = "删除“${title.ifBlank { "未命名收藏夹" }}”及其中的收藏记录？已保存的本地收藏不受影响。"
    val confirm: @Composable () -> Unit = {
        StyleButton(onClick = rememberHapticClick(onClick = onConfirm), enabled = !deleting, primary = true) {
            Text(if (deleting) "正在删除…" else "删除")
        }
    }
    if (isMiuix()) {
        var visible by remember { mutableStateOf(true) }
        GlobalPopupBlur(visible)
        WindowDialog(
            show = visible, title = "删除收藏夹", summary = message,
            modifier = Modifier.semantics { dialog() },
            onDismissRequest = if (deleting) null else ({ visible = false }),
            onDismissFinished = onDismiss,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                error?.let { Text(it, color = MiuixTheme.colorScheme.error) }
                DialogActions(listOf({
                    StyleButton(onClick = rememberHapticClick { visible = false }, enabled = !deleting) { Text("取消") }
                }, confirm))
            }
        }
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!deleting) onDismiss() },
            icon = { androidx.compose.material3.Icon(Icons.Outlined.Delete, null) },
            title = { Text("删除收藏夹") },
            text = {
                Column {
                    Text(message)
                    if (deleting) androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                    error?.let { Text(it, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = rememberHapticClick(onClick = onConfirm), enabled = !deleting,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(if (deleting) "正在删除…" else "删除")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = rememberHapticClick(onClick = onDismiss), enabled = !deleting) { Text("取消") }
            },
            properties = DialogProperties(dismissOnBackPress = !deleting, dismissOnClickOutside = !deleting),
        )
    }
}
