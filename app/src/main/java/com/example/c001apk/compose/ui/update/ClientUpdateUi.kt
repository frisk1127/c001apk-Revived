package com.example.c001apk.compose.ui.update

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.constant.Constants.URL_RELEASES
import com.example.c001apk.compose.constant.Constants.URL_TELEGRAM_CHANNEL
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.design.AlertDialog
import com.example.c001apk.compose.ui.component.design.Text
import com.example.c001apk.compose.ui.component.design.TextButton
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.component.settings.BasicListItem
import com.example.c001apk.compose.ui.component.settings.SwitchListItem
import com.example.c001apk.compose.ui.theme.isMiuix
import com.example.c001apk.compose.util.openInBrowser
import io.noties.markwon.Markwon
import io.noties.markwon.movement.MovementMethodPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutClientUpdates() {
    val context = LocalContext.current
    val updates = LocalClientUpdates.current
    val automatic = LocalUserPreferences.current.autoCheckClientUpdate
    val title = if (updates?.checking == true) "正在检查更新…" else "检查更新"
    val check = rememberHapticClick { updates?.check() }
    val channel = rememberHapticClick { context.openInBrowser(URL_TELEGRAM_CHANNEL) }
    if (isMiuix()) {
        ArrowPreference(title = title, summary = "通过 GitHub Releases 获取正式版",
            enabled = updates != null && !updates.checking, onClick = check)
        SwitchPreference(title = "自动检查客户端更新", summary = "启动时检查 GitHub 正式版",
            checked = automatic, enabled = updates != null,
            onCheckedChange = { updates?.setAutomatic(it) })
        ArrowPreference(title = "Telegram 频道", summary = "@c001apk_revived · 版本更新与项目消息", onClick = channel)
    } else {
        BasicListItem(headlineText = title, supportingText = "通过 GitHub Releases 获取正式版",
            leadingImageVector = Icons.Outlined.SystemUpdate,
            onClick = if (updates != null && !updates.checking) check else null)
        SwitchListItem(value = automatic, headlineText = "自动检查客户端更新",
            supportingText = "启动时检查 GitHub 正式版", leadingImageVector = Icons.Outlined.SystemUpdate,
            onValueChanged = { updates?.setAutomatic(it) })
        BasicListItem(headlineText = "Telegram 频道", supportingText = "@c001apk_revived · 版本更新与项目消息",
            leadingImageVector = Icons.AutoMirrored.Outlined.Send, onClick = channel)
    }
}

@Composable
fun ClientUpdateDialog(viewModel: ClientUpdateViewModel) {
    val notice = viewModel.notice ?: return
    val context = LocalContext.current
    val release = notice.release
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        modifier = Modifier.testTag("client-update-dialog"),
        title = { Text(when { notice.error != null -> "检查更新失败"; notice.hasUpdate -> "发现新版本"; else -> "当前已是最新版本" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("当前版本：${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall)
                Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (notice.error != null) Text(notice.error)
                    else if (release != null) {
                        Text("GitHub 正式版：${release.tag}", style = MaterialTheme.typography.titleSmall)
                        ReleaseNotes(release.notes)
                    }
                }
                TextButton(onClick = rememberHapticClick { context.openInBrowser(URL_TELEGRAM_CHANNEL) },
                    modifier = Modifier.fillMaxWidth().testTag("update-telegram-channel")) {
                    Text("Telegram 频道：@c001apk_revived")
                }
                TextButton(onClick = { context.openInBrowser(release?.pageUrl ?: URL_RELEASES) },
                    modifier = Modifier.fillMaxWidth()) { Text("在 GitHub 查看发布信息") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (notice.error != null) { viewModel.dismiss(); viewModel.check() }
                else {
                    if (notice.hasUpdate) context.openInBrowser(release?.apkUrl ?: release?.pageUrl ?: URL_RELEASES)
                    viewModel.dismiss()
                }
            }) { Text(when {
                notice.error != null -> "重试"
                !notice.hasUpdate -> "知道了"
                release?.apkUrl != null -> "下载更新"
                else -> "查看发布页"
            }) }
        },
        dismissButton = if (notice.hasUpdate || notice.error != null) ({
            TextButton(onClick = viewModel::dismiss) { Text(if (notice.hasUpdate) "稍后" else "关闭") }
        }) else null,
    )
}

@Composable
private fun ReleaseNotes(markdown: String) {
    val context = LocalContext.current
    val preferences = LocalUserPreferences.current
    val color = (if (isMiuix()) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface).toArgb()
    val linkColor = (if (isMiuix()) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.primary).toArgb()
    val renderer = remember(context) {
        Markwon.builder(context).usePlugin(TablePlugin.create(context))
            .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create())).build()
    }
    val content = remember(renderer, markdown) { renderer.toMarkdown(markdown) }
    AndroidView(modifier = Modifier.fillMaxWidth(), factory = { TextView(it) }, update = { text ->
        text.setTextColor(color)
        text.setLinkTextColor(linkColor)
        text.textSize = 14f * preferences.fontScale * preferences.contentScale
        renderer.setParsedMarkdown(text, content)
    })
}
