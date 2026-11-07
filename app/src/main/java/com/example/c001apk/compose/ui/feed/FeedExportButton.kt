package com.example.c001apk.compose.ui.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Code
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.theme.isMiuix
import com.example.c001apk.compose.ui.component.rememberHapticClick
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.c001apk.compose.logic.backup.FavoriteDocumentExport
import com.example.c001apk.compose.logic.model.FeedEntity
import com.example.c001apk.compose.logic.model.FeedMediaJson
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.design.*
import com.example.c001apk.compose.util.makeToast
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun FeedExportButton(data: HomeFeedResponse.Data) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pendingExport by rememberSaveable { mutableStateOf<String?>(null) }

    fun export(uri: Uri?, html: Boolean) {
        val snapshot = pendingExport
        pendingExport = null
        if (uri == null || snapshot == null) return
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val item = Gson().fromJson(snapshot, FeedEntity::class.java)
                    val output = context.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IOException("无法打开导出文件")
                    output.bufferedWriter(Charsets.UTF_8).use { writer ->
                        if (html) FavoriteDocumentExport.writeHtml(listOf(item), writer)
                        else FavoriteDocumentExport.writeMarkdown(listOf(item), writer)
                    }
                }
                context.makeToast("动态已导出")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                context.makeToast("导出失败，请检查存储位置后重试")
            } finally {
                busy = false
            }
        }
    }

    val markdownLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) {
        export(it, html = false)
    }
    val htmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) {
        export(it, html = true)
    }
    fun chooseFormat(html: Boolean) {
        expanded = false
        pendingExport = Gson().toJson(FeedEntity(
            data.id.orEmpty(), data.uid.orEmpty(), data.username.orEmpty(), data.userAvatar.orEmpty(),
            data.deviceTitle.orEmpty(), data.message.orEmpty(), data.dateline.toString(),
            mediaJson = FeedMediaJson.encode(data),
        ))
        try {
            if (html) htmlLauncher.launch("c001apk_${data.id}.html")
            else markdownLauncher.launch("c001apk_${data.id}.md")
        } catch (_: Exception) {
            pendingExport = null
            context.makeToast("无法打开文件选择器")
        }
    }

    Box {
        IconButton(onClick = rememberHapticClick { expanded = true }, enabled = !busy && pendingExport == null) {
            if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Download, contentDescription = "导出为…")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("导出为 Markdown") }, onClick = { chooseFormat(false) },
                leadingIcon = if (isMiuix()) null else ({ Icon(Icons.Default.Description, null) }))
            DropdownMenuItem(text = { Text("导出为 HTML") }, onClick = { chooseFormat(true) },
                leadingIcon = if (isMiuix()) null else ({ Icon(Icons.Default.Code, null) }))
        }
    }
}
