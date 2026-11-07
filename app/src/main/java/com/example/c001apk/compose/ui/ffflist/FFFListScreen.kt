package com.example.c001apk.compose.ui.ffflist

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import com.example.c001apk.compose.ui.component.design.Icon
import com.example.c001apk.compose.ui.component.design.IconButton
import com.example.c001apk.compose.ui.component.design.FavoriteSearchHeader
import com.example.c001apk.compose.ui.theme.isMiuix
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.c001apk.compose.ui.component.design.HorizontalDivider
import com.example.c001apk.compose.ui.component.design.Scaffold
import com.example.c001apk.compose.ui.component.design.SecondaryScrollableTabRow
import com.example.c001apk.compose.ui.component.design.Tab
import androidx.compose.material3.TabRowDefaults
import com.example.c001apk.compose.ui.component.design.Text
import com.example.c001apk.compose.ui.component.rememberHapticClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.c001apk.compose.ui.component.settings.PreferenceTopBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.ui.carousel.CarouselContentScreen
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.ReportType
import kotlinx.coroutines.launch

/**
 * Created by bggRGjQaUbCoE on 2024/6/12
 */

enum class FFFListType {
    FEED, FOLLOW, APK, USER_FOLLOW, FAN, RECENT, LIKE, REPLY, REPLYME, FAV, HISTORY, COLLECTION, COLLECTION_ITEM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FFFListScreen(
    onBackClick: () -> Unit,
    uid: String?,
    type: String,
    id: String?,
    title: String?,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onViewFFFList: (String?, String, String?, String?) -> Unit,
) {

    val layoutDirection = LocalLayoutDirection.current
    val isMe by lazy { uid == CookieUtil.uid }
    val followList by lazy { listOf("用户", "话题", "数码", "应用") }
    val replyList by lazy { listOf("我的回复", "我收到的回复") }
    val pagerState = rememberPagerState {
        if (type == FFFListType.FOLLOW.name) followList.size
        else replyList.size
    }
    val tabList by lazy {
        if (type == FFFListType.FOLLOW.name) followList
        else replyList
    }
    var refreshState by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val searchable = type == FFFListType.COLLECTION.name
    val coordinatedRefresh = isMiuix() && (searchable || type == FFFListType.COLLECTION_ITEM.name)
    var searchMode by rememberSaveable(type, id) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(type, id) { mutableStateOf("") }
    val pageScroll = MiuixScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize().then(if (isMiuix() && !coordinatedRefresh) Modifier.nestedScroll(pageScroll.nestedScrollConnection) else Modifier),
        topBar = {
            FavoriteSearchHeader(
                enabled = searchable,
                query = searchQuery, onQueryChange = { searchQuery = it },
                expanded = searchMode, onExpandedChange = { searchMode = it },
                hint = "搜索收藏正文",
            ) {
                PreferenceTopBar(
                    scrollBehavior = pageScroll,
                    onBack = onBackClick,
                    title = title ?: when (type) {
                                FFFListType.FEED.name -> "我的动态"

                                FFFListType.FOLLOW.name -> "我的关注"

                                FFFListType.USER_FOLLOW.name -> {
                                    if (isMe)
                                        "我关注的人"
                                    else
                                        "TA关注的人"
                                }

                                FFFListType.FAN.name -> {
                                    if (isMe)
                                        "关注我的人"
                                    else
                                        "TA的粉丝"
                                }

                                FFFListType.RECENT.name -> "我的常去"

                                FFFListType.LIKE.name -> "我的赞"

                                FFFListType.REPLY.name -> "我的回复"

                                FFFListType.COLLECTION.name -> "云端收藏"

                                else -> EMPTY_STRING
                            },
                    actions = {
                        if (searchable && !isMiuix()) IconButton(onClick = { searchMode = true }) {
                            Icon(Icons.Default.Search, "搜索云端收藏")
                        }
                    },
                )
            }
        }
    ) { paddingValues ->
        val blurList = com.example.c001apk.compose.ui.component.design.isMiuixBlurEnabled() &&
            type != FFFListType.FOLLOW.name && type != FFFListType.REPLY.name

        Column(
            modifier = Modifier.padding(top = if (blurList) 0.dp else paddingValues.calculateTopPadding())
        ) {
            when (type) {
                FFFListType.FOLLOW.name, FFFListType.REPLY.name -> {
                    val onTabSelected: (Int) -> Unit = { index ->
                        if (pagerState.currentPage == index) {
                            refreshState = true
                        }
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                    SecondaryScrollableTabRow(
                        labels = tabList,
                        onTabSelected = onTabSelected,
                        modifier = Modifier.padding(
                            start = paddingValues.calculateLeftPadding(layoutDirection),
                            end = paddingValues.calculateRightPadding(layoutDirection),
                        ),
                        selectedTabIndex = pagerState.currentPage,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier
                                    .tabIndicatorOffset(
                                        pagerState.currentPage,
                                        matchContentSize = true
                                    )
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            )
                        },
                        divider = {}
                    ) {
                        tabList.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = rememberHapticClick { onTabSelected(index) },
                                text = { Text(text = tab) }
                            )
                        }
                    }

                    if (!com.example.c001apk.compose.ui.theme.isMiuix()) HorizontalDivider()

                    HorizontalPager(
                        state = pagerState
                    ) { index ->
                        if (type == FFFListType.FOLLOW.name) {
                            when (index) {
                                0, 3 -> FFFContentScreen(
                                    id = id,
                                    uid = uid,
                                    type = if (index == 0) FFFListType.FOLLOW.name
                                    else FFFListType.APK.name,
                                    paddingValues = PaddingValues(
                                        start = paddingValues.calculateLeftPadding(layoutDirection),
                                        bottom = paddingValues.calculateBottomPadding(),
                                    ),
                                    refreshState = refreshState,
                                    resetRefreshState = { refreshState = false },
                                    onViewUser = onViewUser,
                                    onViewFeed = onViewFeed,
                                    onOpenLink = onOpenLink,
                                    onCopyText = onCopyText,
                                    onReport = onReport,
                                    onViewFFFList = onViewFFFList,
                                )

                                1, 2 -> CarouselContentScreen(
                                    url = if (index == 1) "#/topic/userFollowTagList"
                                    else "#/product/followProductList",
                                    title = if (index == 1) "我关注的话题"
                                    else "我关注的数码吧",
                                    paddingValues = paddingValues,
                                    refreshState = refreshState,
                                    resetRefreshState = { refreshState = false },
                                    onViewUser = onViewUser,
                                    onViewFeed = onViewFeed,
                                    onOpenLink = onOpenLink,
                                    onCopyText = onCopyText,
                                    onReport = onReport,
                                )
                            }
                        } else if (type == FFFListType.REPLY.name) {
                            FFFContentScreen(
                                id = id,
                                uid = uid,
                                type = if (index == 0) FFFListType.REPLY.name else FFFListType.REPLYME.name,
                                paddingValues = PaddingValues(
                                    start = paddingValues.calculateLeftPadding(layoutDirection),
                                    bottom = paddingValues.calculateBottomPadding(),
                                ),
                                refreshState = refreshState,
                                resetRefreshState = { refreshState = false },
                                onViewUser = onViewUser,
                                onViewFeed = onViewFeed,
                                onOpenLink = onOpenLink,
                                onCopyText = onCopyText,
                                onReport = onReport,
                                onViewFFFList = onViewFFFList,
                            )

                        }
                    }

                }

                else -> {
                    if (!com.example.c001apk.compose.ui.theme.isMiuix()) HorizontalDivider()
                    FFFContentScreen(
                        searchQuery = if (searchable) searchQuery else "",
                        topAppBarScrollBehavior = if (coordinatedRefresh) pageScroll else null,
                        id = id,
                        uid = uid,
                        type = type,
                        paddingValues = PaddingValues(
                            top = if (blurList) paddingValues.calculateTopPadding() else 0.dp,
                            start = paddingValues.calculateLeftPadding(layoutDirection),
                            bottom = paddingValues.calculateBottomPadding(),
                        ),
                        refreshState = null,
                        resetRefreshState = {},
                        onViewUser = onViewUser,
                        onViewFeed = onViewFeed,
                        onOpenLink = onOpenLink,
                        onCopyText = onCopyText,
                        onReport = onReport,
                        onViewFFFList = onViewFFFList,
                    )
                }
            }
        }

    }

}
