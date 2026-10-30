package com.example.c001apk.compose.ui.component.cards.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.component.ImageView
import com.example.c001apk.compose.ui.component.design.contentPress
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.makeToast
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixUserInfoCard(data: HomeFeedResponse.Data, onFollow: (String, Int) -> Unit,
    onPMUser: (String, String) -> Unit, onViewFFFList: (String, String) -> Unit) {
    val context = LocalContext.current
    val coverButtonColors = ButtonDefaults.textButtonColors(
        color = Color.Black.copy(alpha = .24f), textColor = Color.White)
    Column(Modifier.fillMaxWidth().testTag("miuix-user-profile")) {
        Box(Modifier.fillMaxWidth().heightIn(min = 230.dp).background(MiuixTheme.colorScheme.surfaceContainer)) {
            ImageView(url = data.cover.orEmpty(), modifier = Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = .20f), Color.Black.copy(alpha = .68f))
            )))
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ImageView(url = data.userAvatar.orEmpty(), isRound = true, modifier = Modifier.size(72.dp))
                    FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(text = "私信", onClick = rememberHapticClick {
                            if (CookieUtil.isLogin) onPMUser(data.uid.orEmpty(), data.username.orEmpty())
                            else context.makeToast("请先登录")
                        }, modifier = Modifier.contentPress(pressedScale = .96f),
                            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            textStyle = MiuixTheme.textStyles.footnote1, colors = coverButtonColors)
                        TextButton(text = if (data.isFollow == 1) "取消关注" else "关注",
                            modifier = Modifier.contentPress(pressedScale = .96f),
                            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            textStyle = MiuixTheme.textStyles.footnote1,
                            colors = if (data.isFollow == 1) coverButtonColors else ButtonDefaults.textButtonColorsPrimary(),
                            onClick = rememberHapticClick { onFollow(data.uid.orEmpty(), data.isFollow ?: 0) })
                    }
                }
                Text(data.username.orEmpty(), Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = rememberHapticClick { context.copyText(data.username.orEmpty()) }),
                    style = MiuixTheme.textStyles.title3, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("UID: ${data.uid}", Modifier.weight(1f).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClick = rememberHapticClick { context.copyText(data.uid.orEmpty()) }),
                        style = MiuixTheme.textStyles.footnote1, color = Color.White.copy(alpha = .85f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Lv.${data.level}", style = MiuixTheme.textStyles.footnote1, color = Color.White.copy(alpha = .85f))
                }
                if (!data.bio.isNullOrEmpty()) Text(data.bio, style = MiuixTheme.textStyles.body1, color = Color.White)
                Text("${fromToday(data.logintime ?: 0)}活跃", style = MiuixTheme.textStyles.footnote1,
                    color = Color.White.copy(alpha = .85f))
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    val values = listOf((data.feed?.id ?: 0).toString(), (data.beLikeNum ?: 0).toString(), (data.follow ?: 0).toString(), (data.fans ?: 0).toString())
                    listOf("动态", "获赞", "关注", "粉丝").forEachIndexed { index, label ->
                        Column(Modifier.weight(1f).clickable(enabled = index >= 2) {
                            onViewFFFList(data.uid.orEmpty(), if (index == 2) FFFListType.USER_FOLLOW.name else FFFListType.FAN.name)
                        }.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(values[index], style = MiuixTheme.textStyles.headline1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(label, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                }
            }
        }
    }
}
