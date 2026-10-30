package com.example.c001apk.compose.ui.component.cards.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.ImageView
import com.example.c001apk.compose.ui.component.CoilLoader
import com.example.c001apk.compose.ui.component.design.contentPress
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.util.DateUtils.fromToday
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.makeToast
import com.example.c001apk.compose.util.ImageShowUtil
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixUserInfoCard(data: HomeFeedResponse.Data, onFollow: (String, Int) -> Unit,
    onPMUser: (String, String) -> Unit, onViewFFFList: (String, String) -> Unit,
    headerTopInset: Dp = 0.dp) {
    val context = LocalContext.current
    val preferences = LocalUserPreferences.current
    val ownProfile = preferences.isLogin && data.uid == preferences.uid
    val coverButtonColors = ButtonDefaults.textButtonColors(
        color = Color.Black.copy(alpha = .24f), textColor = Color.White)
    Column(Modifier.fillMaxWidth().testTag("miuix-user-profile")) {
        Box(Modifier.fillMaxWidth().heightIn(min = 180.dp + headerTopInset)
            .background(MiuixTheme.colorScheme.surfaceContainer)) {
            CoilLoader(url = data.cover, modifier = Modifier.matchParentSize()
                .testTag("miuix-user-cover").semantics { contentDescription = "查看主页背景图" }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                    enabled = !data.cover.isNullOrBlank()) {
                    ImageShowUtil.startBigImgViewSimple(context, listOf(data.cover.orEmpty()),
                        userAgent = preferences.userAgent)
                })
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = .12f), Color.Black.copy(alpha = .64f))
            )))
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp,
                top = 20.dp + headerTopInset, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ImageView(url = data.userAvatar.orEmpty(), isRound = true, modifier = Modifier.size(64.dp))
                    if (!ownProfile) FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(text = "私信", onClick = rememberHapticClick {
                            if (preferences.isLogin) onPMUser(data.uid.orEmpty(), data.username.orEmpty())
                            else context.makeToast("请先登录")
                        }, modifier = Modifier.contentPress(pressedScale = .96f),
                            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = coverButtonColors)
                        TextButton(text = if (data.isFollow == 1) "取消关注" else "关注",
                            modifier = Modifier.contentPress(pressedScale = .96f),
                            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = if (data.isFollow == 1) coverButtonColors else ButtonDefaults.textButtonColorsPrimary(),
                            onClick = rememberHapticClick { onFollow(data.uid.orEmpty(), data.isFollow ?: 0) })
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(data.username.orEmpty(), Modifier.weight(1f, fill = false).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClick = rememberHapticClick { context.copyText(data.username.orEmpty()) }),
                        style = MiuixTheme.textStyles.title3, color = Color.White,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = .14f)) {
                        Text("Lv.${data.level ?: 0}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MiuixTheme.textStyles.footnote2, color = Color.White,
                            maxLines = 1)
                    }
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("UID: ${data.uid.orEmpty()}", Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        onClick = rememberHapticClick { context.copyText(data.uid.orEmpty()) }),
                        style = MiuixTheme.textStyles.footnote1, color = Color.White.copy(alpha = .78f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if ((data.logintime ?: 0) > 0) {
                        Text("${fromToday(data.logintime ?: 0)}活跃", style = MiuixTheme.textStyles.footnote1,
                            color = Color.White.copy(alpha = .78f), maxLines = 1)
                    }
                }
                if (!data.bio.isNullOrBlank()) Text(data.bio, style = MiuixTheme.textStyles.body2,
                    color = Color.White.copy(alpha = .9f))
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
            Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(0.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    val values = listOf((data.feed?.id ?: 0).toString(), (data.beLikeNum ?: 0).toString(), (data.follow ?: 0).toString(), (data.fans ?: 0).toString())
                    listOf("动态", "获赞", "关注", "粉丝").forEachIndexed { index, label ->
                        val action = if (index >= 2) Modifier.contentPress().clickable(onClick = rememberHapticClick {
                            onViewFFFList(data.uid.orEmpty(), if (index == 2) FFFListType.USER_FOLLOW.name else FFFListType.FAN.name)
                        }) else Modifier
                        Column(Modifier.weight(1f).then(action).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(values[index], style = MiuixTheme.textStyles.headline1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                        }
                    }
                }
            }
        }
    }
}
