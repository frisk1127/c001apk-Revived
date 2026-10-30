package com.example.c001apk.compose.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import com.example.c001apk.compose.ui.component.design.AlertDialog
import com.example.c001apk.compose.ui.component.design.DropdownMenu
import com.example.c001apk.compose.ui.component.design.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.c001apk.compose.ui.component.design.Icon
import com.example.c001apk.compose.ui.component.design.IconButton
import com.example.c001apk.compose.ui.component.design.Scaffold
import com.example.c001apk.compose.ui.component.design.Text
import com.example.c001apk.compose.ui.component.design.TextButton
import com.example.c001apk.compose.ui.component.design.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.c001apk.compose.ui.component.design.RefreshContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.FooterCard
import com.example.c001apk.compose.ui.component.ItemCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.ui.component.cards.UserInfoCard
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.DateUtils.timeStamp2Date
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.ShareType
import com.example.c001apk.compose.util.copyText
import com.example.c001apk.compose.util.getShareText
import com.example.c001apk.compose.util.makeToast
import com.example.c001apk.compose.util.shareText
import com.example.c001apk.compose.ui.component.MoreMenuButton
import com.example.c001apk.compose.ui.component.design.isMiuixBlurEnabled
import com.example.c001apk.compose.ui.theme.isMiuix
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Created by bggRGjQaUbCoE on 2024/6/4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    uid: String,
    onBackClick: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onSearch: (String, String, String) -> Unit,
    onViewFFFList: (String, String) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onPMUser: (String, String) -> Unit,
) {

    val layoutDirection = LocalLayoutDirection.current
    val miuix = isMiuix()
    val blur = isMiuixBlurEnabled()

    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel =
        hiltViewModel<UserViewModel, UserViewModel.ViewModelFactory>(key = uid) { factory ->
            factory.create(uid)
        }
    var showUserInfoDialog by remember { mutableStateOf(false) }
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val firstVisibleItemIndex by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val userData = (viewModel.userState as? LoadingState.Success)?.response
    val extendCover = blur && !userData?.cover.isNullOrBlank()
    val topBarSurfaceAlpha by animateFloatAsState(
        targetValue = if (extendCover && firstVisibleItemIndex == 0) .65f else .82f,
        animationSpec = tween(180), label = "profile-cover-scrim",
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBarSurfaceAlpha = topBarSurfaceAlpha,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = if (firstVisibleItemIndex > 0)
                            (viewModel.userState as? LoadingState.Success)?.response?.username
                                ?: uid
                        else EMPTY_STRING,
                actions = {
                    if (viewModel.userState is LoadingState.Success) {
                        Row(Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(
                                onClick = {
                                    onSearch(viewModel.username, "user", viewModel.uid)
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                            Box {
                                MoreMenuButton { dropdownMenuExpanded = true }
                                DropdownMenu(
                                    expanded = dropdownMenuExpanded,
                                    onDismissRequest = { dropdownMenuExpanded = false }
                                ) {
                                    listOf("复制", "分享", "用户信息")
                                        .forEachIndexed { index, menu ->
                                            DropdownMenuItem(
                                                text = { Text(menu) },
                                                onClick = {
                                                    dropdownMenuExpanded = false
                                                    when (index) {
                                                        0 -> context.copyText(
                                                            getShareText(ShareType.USER, uid)
                                                        )

                                                        1 -> context.shareText(
                                                            getShareText(ShareType.USER, uid)
                                                        )

                                                        2 -> showUserInfoDialog = true
                                                    }
                                                }
                                            )
                                        }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (viewModel.isBlocked) {
                                                    stringResource(id = R.string.menu_unblock)
                                                } else {
                                                    "拉黑"
                                                }
                                            )
                                        },
                                        onClick = {
                                            dropdownMenuExpanded = false
                                            viewModel.onBlockUser(viewModel.uid)
                                        }
                                    )
                                    if (isLogin) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.menu_report)) },
                                            onClick = {
                                                dropdownMenuExpanded = false
                                                onReport(uid, ReportType.USER)
                                            }
                                        )
                                    }
                                }
                            }

                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (miuix && !blur) MiuixTheme.colorScheme.surface else Color.Transparent),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->

        RefreshContainer(
            modifier = Modifier.padding(
                start = paddingValues.calculateLeftPadding(layoutDirection),
                end = paddingValues.calculateRightPadding(layoutDirection),
                top = if (miuix && !blur) paddingValues.calculateTopPadding() else 0.dp,
            ),
            indicatorTopInset = if (miuix) {
                12.dp + if (blur) paddingValues.calculateTopPadding() else 0.dp
            } else 0.dp,
            isRefreshing = viewModel.isRefreshing,
            onRefresh = {
                viewModel.isPull = true
                viewModel.refresh()
            },

        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = if (blur && !extendCover) paddingValues.calculateTopPadding() else 0.dp,
                    bottom = 10.dp + paddingValues.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                state = lazyListState
            ) {

                when (viewModel.userState) {
                    LoadingState.Loading, LoadingState.Empty, is LoadingState.Error -> {
                        item(key = "userState") {
                            Box(modifier = Modifier.fillParentMaxSize()) {
                                LoadingCard(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 10.dp),
                                    state = viewModel.userState,
                                    onClick = if (viewModel.userState is LoadingState.Loading) null
                                    else viewModel::refresh
                                )
                            }
                        }
                    }

                    is LoadingState.Success -> {
                        item(key = "userInfo") {
                            UserInfoCard(
                                data = (viewModel.userState as LoadingState.Success).response,
                                onFollow = viewModel::onFollowUser,
                                onPMUser = onPMUser,
                                onViewFFFList = onViewFFFList,
                                headerTopInset = if (extendCover) paddingValues.calculateTopPadding() else 0.dp,
                            )
                        }
                    }
                }

                if (viewModel.userState is LoadingState.Success) {

                    ItemCard(
                        loadingState = viewModel.loadingState,
                        loadMore = viewModel::loadMore,
                        isEnd = viewModel.isEnd,
                        onViewUser = { uid ->
                            if (uid != viewModel.uid) {
                                onViewUser(uid)
                            }
                        },
                        onViewFeed = onViewFeed,
                        onOpenLink = onOpenLink,
                        onCopyText = onCopyText,
                        onReport = onReport,
                        onLike = viewModel::onLike,
                        onDelete = { id, deleteType, _ ->
                            viewModel.onDelete(id, deleteType)
                        },
                        onBlockUser = { uid, _ ->
                            viewModel.onBlockUser(uid)
                        },
                    )

                    FooterCard(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        footerState = viewModel.footerState,
                        loadMore = viewModel::loadMore,
                    )
                }

            }
        }


    }

    when {
        showUserInfoDialog -> {
            val data = (viewModel.userState as LoadingState.Success).response
            AlertDialog(
                onDismissRequest = { showUserInfoDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = { showUserInfoDialog = false }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = data.username.orEmpty(),
                    )
                },
                text = {
                    Text(
                        text = """
                                UID: ${data.uid}

                                等级: Lv.${data.level}

                                性别: ${if (data.gender == 0) "女" else if (data.gender == 1) "男" else "未知"}

                                注册时长: ${((System.currentTimeMillis() / 1000 - (data.regdate ?: 0)) / 24 / 3600)} 天

                                注册时间: ${timeStamp2Date(data.regdate ?: 0)}
                            """.trimIndent()
                    )
                },
            )
        }
    }

    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }

}
