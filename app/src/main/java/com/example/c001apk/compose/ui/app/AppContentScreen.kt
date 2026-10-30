package com.example.c001apk.compose.ui.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.ui.component.CommonScreen
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast

/**
 * Created by bggRGjQaUbCoE on 2024/6/10
 */
@Composable
fun AppContentScreen(
    refreshState: Boolean,
    resetRefreshState: () -> Unit,
    id: String,
    appCommentSort: String,
    appCommentTitle: String,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
    isScrollingUp: ((Boolean) -> Unit)? = null,
    beforeRefreshScrollConnection: NestedScrollConnection? = null,
    onPullProgress: ((Float) -> Unit)? = null,
) {

    val viewModel =
        hiltViewModel<AppContentViewModel, AppContentViewModel.ViewModelFactory>(key = id + appCommentSort) { factory ->
            factory.create(
                url = "/page?url=/feed/apkCommentList?id=$id$appCommentSort",
                appCommentTitle = appCommentTitle
            )
        }

    val windowInsets =
        WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)

    CommonScreen(
        viewModel = viewModel,
        refreshState = refreshState,
        resetRefreshState = resetRefreshState,
        paddingValues = windowInsets.asPaddingValues(),
        onViewUser = onViewUser,
        onViewFeed = onViewFeed,
        onOpenLink = onOpenLink,
        onCopyText = onCopyText,
        onReport = onReport,
        isScrollingUp = isScrollingUp,
        beforeRefreshScrollConnection = beforeRefreshScrollConnection,
        onPullProgress = onPullProgress,
    )

    val context = LocalContext.current
    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }

}
