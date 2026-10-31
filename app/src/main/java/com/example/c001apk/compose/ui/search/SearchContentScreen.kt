package com.example.c001apk.compose.ui.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.isVisible
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.ui.component.CommonScreen
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast

/**
 * Created by bggRGjQaUbCoE on 2024/6/9
 */
@Composable
fun SearchContentScreen(
    searchType: SearchType,
    keyword: String,
    pageType: String?,
    pageParam: String?,
    refreshState: Boolean,
    resetRefreshState: () -> Unit,
    feedType: SearchFeedType,
    orderType: SearchOrderType,
    paddingValues: PaddingValues,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    updateInitPage: () -> Unit,
    onReport: (String, ReportType) -> Unit,
) {

    val view = LocalView.current
    if (view.isVisible) {
        updateInitPage()
    }

    val viewModel =
        hiltViewModel<SearchContentViewModel, SearchContentViewModel.ViewModelFactory>(key = keyword + searchType.name) { factory ->
            factory.create(
                type = when (searchType) {
                    SearchType.FEED -> "feed"
                    SearchType.APP -> "apk"
                    SearchType.GAME -> "game"
                    SearchType.PRODUCT -> "product"
                    SearchType.USER -> "user"
                    SearchType.TOPIC -> "feedTopic"
                },
                keyword = keyword, pageType = pageType, pageParam = pageParam,
            )
        }

    if (searchType == SearchType.FEED) {
        LaunchedEffect(feedType) {
            if (feedType != viewModel.searchFeedType) {
                viewModel.searchFeedType = feedType
                viewModel.feedType = when (feedType) {
                    SearchFeedType.ALL -> "all"
                    SearchFeedType.FEED -> "feed"
                    SearchFeedType.ARTICLE -> "feedArticle"
                    SearchFeedType.COOLPIC -> "picture"
                    SearchFeedType.COMMENT -> "comment"
                    SearchFeedType.RATING -> "rating"
                    SearchFeedType.ANSWER -> "answer"
                    SearchFeedType.QUESTION -> "question"
                    SearchFeedType.VOTE -> "vote"
                }
                viewModel.refresh()
            }
        }
        LaunchedEffect(orderType) {
            if (orderType != viewModel.sortType) {
                viewModel.sortType = orderType
                viewModel.sort = orderType.apiValue
                viewModel.refresh()
            }
        }
    }

    CommonScreen(
        viewModel = viewModel,
        refreshState = refreshState,
        resetRefreshState = resetRefreshState,
        paddingValues = paddingValues,
        onViewUser = onViewUser,
        onViewFeed = onViewFeed,
        onOpenLink = onOpenLink,
        onCopyText = onCopyText,
        onReport = onReport,
    )

    val context = LocalContext.current
    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }

}
