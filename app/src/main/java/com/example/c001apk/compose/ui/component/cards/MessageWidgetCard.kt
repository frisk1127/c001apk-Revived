package com.example.c001apk.compose.ui.component.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.c001apk.compose.ui.component.design.contentCard
import com.example.c001apk.compose.ui.ffflist.FFFListType
import com.example.c001apk.compose.util.CookieUtil

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */
@Composable
fun MessageWidgetCard(
    modifier: Modifier = Modifier,
    onViewFFFList: (String?, String) -> Unit,
    onViewHistory: (String) -> Unit,
) {
    if (com.example.c001apk.compose.ui.theme.isMiuix()) {
        com.example.c001apk.compose.ui.component.cards.miuix.MiuixAccountShortcuts(modifier, onViewFFFList, onViewHistory)
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .contentCard()

    ) {
        FFFCardRow(
            dataList = listOf(
                FFFCardItem(
                    title = "本地收藏",
                    value = null,
                    imageVector = Icons.Outlined.Archive,
                    FFFListType.FAV.name
                ),
                FFFCardItem(
                    title = "浏览历史",
                    value = null,
                    imageVector = Icons.Outlined.History,
                    FFFListType.HISTORY.name
                ),
                FFFCardItem(
                    title = "我的常去",
                    value = null,
                    imageVector = Icons.Outlined.MyLocation,
                    FFFListType.RECENT.name
                )
            ),
            onViewFFFList = { type ->
                onViewFFFList(CookieUtil.uid, type)
            },
            onViewHistory = onViewHistory
        )
        FFFCardRow(
            dataList = listOf(
                FFFCardItem(
                    title = "云端收藏",
                    value = null,
                    imageVector = Icons.Outlined.StarOutline,
                    FFFListType.COLLECTION.name
                ),
                FFFCardItem(
                    title = "我的赞",
                    value = null,
                    imageVector = Icons.Outlined.FavoriteBorder,
                    FFFListType.LIKE.name
                ),
                FFFCardItem(
                    title = "我的回复",
                    value = null,
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    FFFListType.REPLY.name
                )
            ),
            onViewFFFList = { type ->
                onViewFFFList(
                    if (type == FFFListType.COLLECTION.name) null else CookieUtil.uid,
                    type
                )
            },
            onViewHistory = {}
        )
    }

}
