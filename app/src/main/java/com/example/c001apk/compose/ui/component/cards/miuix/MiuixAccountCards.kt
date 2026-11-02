package com.example.c001apk.compose.ui.component.cards.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import top.yukonga.miuix.kmp.icon.extended.Contacts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.ui.history.HistoryType
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.decode
import top.yukonga.miuix.kmp.basic.*
import com.example.c001apk.compose.ui.component.design.AppGlyphs
import com.example.c001apk.compose.ui.component.design.contentPress
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixAccountHeader(modifier: Modifier, loggedIn: Boolean, avatar: String, name: String,
    level: String, experience: String, nextLevel: String, onLogin: () -> Unit,
    onAccounts: () -> Unit, onUser: (String) -> Unit) {
    Card(modifier.fillMaxWidth().padding(16.dp).contentPress(enabled = loggedIn), insideMargin = PaddingValues(16.dp)) {
        if (!loggedIn) {
            BasicComponent(title = "登录酷安", summary = "查看关注、私信和互动消息",
                insideMargin = PaddingValues(0.dp), endActions = {
                    TextButton("登录", onClick = rememberHapticClick(onClick = onLogin),
                        colors = ButtonDefaults.textButtonColorsPrimary())
                })
        } else Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.contentPress(pressedScale = .92f)) {
                ContentAvatar(avatar, 48.dp, "account-avatar") { onUser(CookieUtil.uid) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name.decode, style = MiuixTheme.textStyles.title3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Lv.$level · $experience/$nextLevel", style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                val total = nextLevel.toFloatOrNull()?.takeIf { it > 0f } ?: 1f
                LinearProgressIndicator(progress = ((experience.toFloatOrNull() ?: 0f) / total).coerceIn(0f, 1f))
            }
            IconButton(modifier = Modifier.contentPress(pressedScale = .92f),
                onClick = rememberHapticClick(onClick = onAccounts)) {
                Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.Medium.Contacts, "账号管理", tint = MiuixTheme.colorScheme.onSurfaceVariantActions)
            }
        }
    }
}

@Composable
fun MiuixAccountStats(modifier: Modifier, values: List<String>, onOpen: (String, String) -> Unit) {
    val destinations = listOf("动态" to FFFListType.FEED, "关注" to FFFListType.FOLLOW, "粉丝" to FFFListType.FAN)
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            destinations.forEachIndexed { index, (label, type) ->
                Column(Modifier.weight(1f).contentPress(enabled = CookieUtil.isLogin, pressedScale = .96f)
                    .clickable(enabled = CookieUtil.isLogin, role = Role.Button) {
                    onOpen(CookieUtil.uid, type.name)
                }.padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(values.getOrNull(index) ?: "—", style = MiuixTheme.textStyles.title3)
                    Text(label, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
    }
}

@Composable
fun MiuixAccountShortcuts(modifier: Modifier, onOpen: (String?, String) -> Unit, onHistory: (String) -> Unit) {
    val labels = listOf("本地收藏", "云端收藏", "浏览历史", "我的常去", "我的收藏", "我的赞", "我的回复")
    val icons = listOf(MiuixIcons.Medium.Folder, MiuixIcons.Regular.Recent, MiuixIcons.Medium.Pin,
        MiuixIcons.Medium.Favorites, AppGlyphs.Like, MiuixIcons.Medium.Reply)
    Card(modifier.fillMaxWidth()) {
        repeat((labels.size + 2) / 3) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(3) { col ->
                    val index = row * 3 + col
                    if (index >= labels.size) {
                        Spacer(Modifier.weight(1f))
                        return@repeat
                    }
                    val enabled = index < 3 || CookieUtil.isLogin
                    Column(Modifier.weight(1f).contentPress(enabled = enabled, pressedScale = .96f)
                        .clickable(enabled = enabled, role = Role.Button) {
                        when (index) {
                            0 -> onHistory(HistoryType.FAV.name)
                            1 -> onHistory(HistoryType.CLOUD.name)
                            2 -> onHistory(HistoryType.HISTORY.name)
                            3 -> onOpen(CookieUtil.uid, FFFListType.RECENT.name)
                            4 -> onOpen(null, FFFListType.COLLECTION.name)
                            5 -> onOpen(CookieUtil.uid, FFFListType.LIKE.name)
                            6 -> onOpen(CookieUtil.uid, FFFListType.REPLY.name)
                        }
                    }.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val color = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.disabledOnSecondaryVariant
                        Icon(icons[index], null, tint = color)
                        Text(labels[index], style = MiuixTheme.textStyles.body2,
                            color = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.disabledOnSecondaryVariant)
                    }
                }
            }
        }
    }
}
