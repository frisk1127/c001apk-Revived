package com.example.c001apk.compose.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.c001apk.compose.ui.component.design.RefreshContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import com.example.c001apk.compose.ui.base.BaseViewModel
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.isScrollingUp
import com.example.c001apk.compose.util.Utils.richToString
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.logic.state.FooterState
import com.example.c001apk.compose.ui.component.design.Text
import com.example.c001apk.compose.ui.component.design.secondaryTextColor
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.ScrollBehavior

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonScreen(
    viewModel: BaseViewModel,
    refreshState: Boolean?,
    resetRefreshState: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    needTopPadding: Boolean = false,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    isHomeFeed: Boolean = false,
    onReport: ((String, ReportType) -> Unit)? = null,
    onViewFFFList: ((String?, String, String?, String?) -> Unit)? = null,
    onHandleRecent: ((String, String, String, Int) -> Unit)? = null,
    onHandleMessage: ((String, Int) -> Unit)? = null,
    onViewChat: ((String, String, String) -> Unit)? = null,
    onDeleteNotice: ((String) -> Unit)? = null,
    isScrollingUp: ((Boolean) -> Unit)? = null,
    productConfigRows: List<HomeFeedResponse.ProductConfig> = emptyList(),
    selectedProductConfigIds: Set<String> = emptySet(),
    onViewProductConfig: ((HomeFeedResponse.ProductConfig) -> Unit)? = null,
    onToggleProductConfigComparison: ((HomeFeedResponse.ProductConfig) -> Unit)? = null,
    onShowProductConfigComparison: (() -> Unit)? = null,
    onShowGlobalProductComparison: (() -> Unit)? = null,
    onToggleProductEntityComparison: ((HomeFeedResponse.Entities) -> Unit)? = null,
    lazyListState: LazyListState = rememberLazyListState(),
    beforeRefreshScrollConnection: NestedScrollConnection? = null,
    onPullProgress: ((Float) -> Unit)? = null,
    topAppBarScrollBehavior: ScrollBehavior? = null,
    searchQuery: String = "",
    onDeleteCollection: ((HomeFeedResponse.Data) -> Unit)? = null,
) {

    val view = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val query = searchQuery.trim()
    val sourceState = viewModel.loadingState
    val displayState = remember(sourceState, query) {
        if (query.isEmpty() || sourceState !is LoadingState.Success) sourceState
        else LoadingState.Success(sourceState.response.filter { item ->
            listOf(item.title, item.message, item.username, item.description, item.deviceTitle)
                .any { it?.richToString()?.contains(query, ignoreCase = true) == true }
        })
    }
    LaunchedEffect(viewModel, query, sourceState, viewModel.isLoadMore, viewModel.isRefreshing,
        viewModel.isEnd, viewModel.footerState) {
        if (query.isNotEmpty() && sourceState is LoadingState.Success &&
            !viewModel.isLoadMore && !viewModel.isRefreshing && !viewModel.isEnd
        ) {
            delay(250)
            viewModel.loadMore()
        }
    }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) lazyListState.scrollToItem(0)
    }

    LaunchedEffect(refreshState) {
        if (refreshState == true) {
            resetRefreshState()
            if (view.isVisible) {
                viewModel.refresh()
                lazyListState.scrollToItem(0)
            }
        }
    }

    isScrollingUp?.let {
        it(lazyListState.isScrollingUp())
    }

    val blur = com.example.c001apk.compose.ui.component.design.isMiuixBlurEnabled()
    val navigationPadding = com.example.c001apk.compose.ui.component.design.LocalNavigationPadding.current
    RefreshContainer(
        modifier = Modifier.padding(
            start = paddingValues.calculateLeftPadding(layoutDirection),
            end = paddingValues.calculateRightPadding(layoutDirection),
            top = if (needTopPadding && !blur) paddingValues.calculateTopPadding() else 0.dp
        ),
        indicatorTopInset = if (needTopPadding && blur) paddingValues.calculateTopPadding() else 0.dp,
        isRefreshing = viewModel.isRefreshing,
        onRefresh = viewModel::refresh,
        onPullProgress = onPullProgress,
        topAppBarScrollBehavior = topAppBarScrollBehavior,

    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(beforeRefreshScrollConnection?.let { Modifier.nestedScroll(it) } ?: Modifier),
            contentPadding = PaddingValues(
                top = 10.dp + if (needTopPadding && blur) paddingValues.calculateTopPadding() else 0.dp,
                bottom = 10.dp + maxOf(paddingValues.calculateBottomPadding(), navigationPadding)
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            state = lazyListState
        ) {
            if (query.isNotEmpty() && displayState is LoadingState.Success) {
                item(key = "search-result-summary") {
                    val count = displayState.response.size
                    val searching = !viewModel.isEnd || viewModel.isLoadMore
                    Text(
                        when {
                            searching -> "已找到 $count 项，正在搜索…"
                            viewModel.footerState is FooterState.Error -> "已找到 $count 项，搜索未完成"
                            count == 0 -> "没有匹配的内容"
                            else -> "找到 $count 项"
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = secondaryTextColor(),
                    )
                }
            }

            ItemCard(
                loadingState = displayState,
                loadMore = viewModel::loadMore,
                isEnd = viewModel.isEnd || query.isNotEmpty(),
                onViewUser = onViewUser,
                onViewFeed = onViewFeed,
                onOpenLink = onOpenLink,
                onCopyText = onCopyText,
                isHomeFeed = isHomeFeed,
                onReport = onReport,
                onViewFFFList = onViewFFFList,
                onDeleteCollection = onDeleteCollection,
                onLike = viewModel::onLike,
                onDelete = { id, deleteType, _ ->
                    viewModel.onDelete(id, deleteType)
                },
                onBlockUser = { uid, _ ->
                    viewModel.onBlockUser(uid)
                },
                onFollowUser = viewModel::onFollowUser,
                onHandleRecent = onHandleRecent,
                onHandleMessage = onHandleMessage,
                onViewChat = onViewChat,
                onDeleteNotice = onDeleteNotice,
                productConfigRows = productConfigRows,
                selectedProductConfigIds = selectedProductConfigIds,
                onViewProductConfig = onViewProductConfig,
                onToggleProductConfigComparison = onToggleProductConfigComparison,
                onShowProductConfigComparison = onShowProductConfigComparison,
                onShowGlobalProductComparison = onShowGlobalProductComparison,
                onToggleProductEntityComparison = onToggleProductEntityComparison,
            )

            FooterCard(
                modifier = Modifier.padding(horizontal = 10.dp),
                footerState = viewModel.footerState,
                loadMore = viewModel::loadMore,
                isFeed = false
            )

        }
    }

}
