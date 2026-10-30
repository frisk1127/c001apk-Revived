package com.example.c001apk.compose.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.FollowType
import com.example.c001apk.compose.R
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.blacklist.BlackListType
import com.example.c001apk.compose.ui.component.LauncherIconImage
import com.example.c001apk.compose.ui.component.settings.DropdownListItem
import com.example.c001apk.compose.ui.component.settings.PreferenceGroup
import com.example.c001apk.compose.ui.component.settings.SelectionItem
import com.example.c001apk.compose.ui.component.settings.SwitchListItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MiuixSettingsContent(
    viewModel: SettingsViewModel,
    cacheSize: String,
    onThemeSettings: () -> Unit,
    onIcon: () -> Unit,
    onDeviceId: () -> Unit,
    onParams: () -> Unit,
    onAbout: () -> Unit,
    onBlacklist: (String) -> Unit,
    onHapticStrength: () -> Unit,
    onClearCache: () -> Unit,
) {
    val prefs = LocalUserPreferences.current
    Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        Modifier.padding(horizontal = 26.dp), fontSize = 13.sp, maxLines = 1,
        overflow = TextOverflow.Ellipsis, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)

    PreferenceGroup("主题") {
        UiStyleSelector(prefs.uiStyle, viewModel::setUiStyle)
        PreferenceLink("主题设置", "配色、深浅模式与界面缩放", onClick = onThemeSettings)
        PreferenceLink(stringResource(R.string.app_icon), stringResource(viewModel.launcherIcon.labelRes),
            onClick = onIcon, trailing = { LauncherIconImage(Modifier.size(32.dp), viewModel.launcherIcon) })
    }
    PreferenceGroup("浏览") {
        DropdownListItem(value = prefs.followType.name, headlineText = "关注分组",
            selections = listOf("全部", "好友", "话题", "数码", "应用").mapIndexed { index, label ->
                SelectionItem(label, FollowType.entries[index].name)
            }, onValueChanged = { index, _ -> viewModel.setFollowType(FollowType.forNumber(index)) })
        DropdownListItem(value = prefs.imageQuality, headlineText = "图片画质",
            selections = listOf("网络自适应", "原图", "普清").mapIndexed { index, label -> SelectionItem(label, index) },
            onValueChanged = { index, _ -> viewModel.setImageQuality(index) })
        SwitchListItem(prefs.imageFilter, Icons.Outlined.PhotoLibrary, "压暗图片", onValueChanged = viewModel::setImageFilter)
        SwitchListItem(prefs.showSquare, Icons.AutoMirrored.Outlined.Feed, "头条显示广场", onValueChanged = viewModel::setShowSquare)
        SwitchListItem(prefs.showEmoji, Icons.Outlined.EmojiEmotions, "显示表情", onValueChanged = viewModel::setShowEmoji)
        SwitchListItem(prefs.recordHistory, Icons.Outlined.History, "记录浏览历史", onValueChanged = viewModel::setRecordHistory)
        SwitchListItem(prefs.openInBrowser, Icons.Outlined.TravelExplore, "使用外部浏览器打开链接", onValueChanged = viewModel::setOpenInBrowser)
    }
    PreferenceGroup("屏蔽") {
        PreferenceLink("用户黑名单", onClick = { onBlacklist(BlackListType.USER.name) })
        PreferenceLink("话题黑名单", onClick = { onBlacklist(BlackListType.TOPIC.name) })
    }
    PreferenceGroup("触感") {
        SwitchListItem(prefs.hapticFeedback, Icons.Outlined.TouchApp, "触感反馈", onValueChanged = viewModel::setHapticFeedback)
        SwitchListItem(prefs.customHaptics, Icons.Outlined.TouchApp, "自定义触感", "关闭时跟随系统触感设置",
            onValueChanged = viewModel::setCustomHaptics)
        if (prefs.customHaptics) {
            PreferenceLink("触感强度", prefs.hapticStrength.label, onClick = onHapticStrength)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SwitchListItem(prefs.reverseHapticEffects, Icons.Outlined.TouchApp, "触感兼容模式",
                    "轻重反馈与预期相反时开启", onValueChanged = viewModel::setReverseHapticEffects)
            }
        }
    }
    PreferenceGroup("高级") {
        PreferenceLink("数字联盟 ID", prefs.szlmId.takeIf { it.isNotBlank() }, onClick = onDeviceId)
        PreferenceLink("机型参数", onClick = onParams)
    }
    PreferenceGroup("应用") {
        SwitchListItem(prefs.checkUpdate, Icons.Outlined.SystemUpdate, "检查已安装应用更新", "检查酷安中的应用版本",
            onValueChanged = viewModel::setCheckUpdate)
        PreferenceLink("清理缓存", cacheSize, onClick = onClearCache)
        PreferenceLink("关于", onClick = onAbout)
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
internal fun PreferenceLink(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    top.yukonga.miuix.kmp.preference.ArrowPreference(
        title = title, summary = summary, onClick = onClick,
        endActions = { trailing?.invoke() },
    )
}
