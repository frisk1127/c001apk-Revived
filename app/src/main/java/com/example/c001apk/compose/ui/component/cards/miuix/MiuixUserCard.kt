package com.example.c001apk.compose.ui.component.cards.miuix

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.design.contentPress
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.util.DateUtils.fromToday
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
internal fun MiuixUserCard(
    modifier: Modifier,
    data: HomeFeedResponse.Data,
    onOpenLink: (String, String?) -> Unit,
    onFollowUser: ((String, Int) -> Unit)?,
) {
    val openUser = { onOpenLink(data.url.orEmpty(), data.username) }
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).contentPress(),
        insideMargin = PaddingValues(16.dp),
        pressFeedbackType = PressFeedbackType.None,
        onClick = openUser,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ContentAvatar(data.userAvatar, 48.dp, "user-result-avatar-${data.uid}", openUser)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(data.username.orEmpty(), style = MiuixTheme.textStyles.headline1,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${data.follow ?: 0} 关注 · ${data.fans ?: 0} 粉丝",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if ((data.logintime ?: 0) > 0) {
                    Text("${fromToday(data.logintime ?: 0)}活跃",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (onFollowUser != null) {
                TextButton(
                    text = if (data.isFollow == 1) "取消关注" else "关注",
                    onClick = rememberHapticClick { onFollowUser(data.uid.orEmpty(), data.isFollow ?: 0) },
                    modifier = Modifier.contentPress(pressedScale = .96f),
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    textStyle = MiuixTheme.textStyles.footnote1,
                    colors = if (data.isFollow == 1) ButtonDefaults.textButtonColors()
                        else ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
