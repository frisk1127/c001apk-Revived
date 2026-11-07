package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.example.c001apk.compose.ui.component.design.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.example.c001apk.compose.ui.component.design.Text
import com.example.c001apk.compose.ui.component.design.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.theme.isMiuix
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Created by bggRGjQaUbCoE on 2024/6/12
 */
@Composable
fun FeedReplySortCard(
    modifier: Modifier = Modifier,
    replyCount: String,
    selected: Int = 0,
    pinnedTopInset: Dp? = null,
    isRefreshing: Boolean = false,
    updateSortReply: (Int) -> Unit,
) {
    if (isMiuix()) {
        com.example.c001apk.compose.ui.component.design.TabBarSurface(modifier.fillMaxWidth(), estimatedHeight = 100.dp,
            pinnedTopInset = pinnedTopInset) {
            Column(Modifier.fillMaxWidth().background(com.example.c001apk.compose.ui.component.design.barBackground(MiuixTheme.colorScheme.surface))) {
                Column(Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp).testTag("miuix-comment-sort"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        top.yukonga.miuix.kmp.basic.Text("评论", Modifier.weight(1f), fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.onSurface)
                        top.yukonga.miuix.kmp.basic.Text("共 $replyCount 回复", fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    val haptic = rememberHapticClick {}
                    com.example.c001apk.compose.ui.component.design.FilledTabs(
                        tabs = listOf("默认", "最新", "热门", "楼主"),
                        selectedTabIndex = selected.coerceIn(0, 3), modifier = Modifier.fillMaxWidth(),
                        onTabSelected = { if (!isRefreshing) { haptic(); updateSortReply(it) } },
                    )
                }
                // Sorting drives this native indicator; pull gestures belong to the page container.
                top.yukonga.miuix.kmp.basic.PullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = {},
                    modifier = Modifier.fillMaxWidth().background(MiuixTheme.colorScheme.surface)
                        .testTag("comment-sort-refresh"),
                    color = MiuixTheme.colorScheme.primary,
                    refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新", "刷新完成"),
                    refreshTextStyle = MiuixTheme.textStyles.footnote1.copy(color = MiuixTheme.colorScheme.onSurfaceVariantSummary),
                ) {}
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "共 $replyCount 回复",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .padding(vertical = 2.dp),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            VerticalDivider()

            FeedReplySortCardItem(
                title = "默认",
                isSelected = selected == 0,
                updateSortReply = {
                    updateSortReply(0)
                }
            )

            FeedReplySortCardItem(
                title = "最新",
                isSelected = selected == 1,
                updateSortReply = {
                    updateSortReply(1)
                }
            )

            FeedReplySortCardItem(
                title = "热门",
                isSelected = selected == 2,
                updateSortReply = {
                    updateSortReply(2)
                }
            )

            FeedReplySortCardItem(
                modifier = Modifier.padding(end = 16.dp),
                title = "楼主",
                isSelected = selected == 3,
                updateSortReply = {
                    updateSortReply(3)
                }
            )

        }

        HorizontalDivider()
    }

}

@Composable
fun FeedReplySortCardItem(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
    updateSortReply: () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clickable(onClick = rememberHapticClick { updateSortReply() })
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )

    VerticalDivider(modifier = modifier)
}
