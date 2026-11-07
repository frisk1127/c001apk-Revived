package com.example.c001apk.compose.ui.ffflist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import com.example.c001apk.compose.ui.component.design.ElevatedCard
import androidx.compose.material3.MaterialTheme
import com.example.c001apk.compose.ui.component.design.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.c001apk.compose.ui.component.design.MenuAction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.constant.Constants.EMPTY_STRING
import com.example.c001apk.compose.ui.component.CommonScreen
import com.example.c001apk.compose.ui.collection.DeleteCollectionDialog
import com.example.c001apk.compose.ui.collection.CloudCollectionSearchViewModel
import com.example.c001apk.compose.ui.collection.CloudCollectionSearchResults
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast

/**
 * Created by bggRGjQaUbCoE on 2024/6/12
 */
@Composable
fun FFFContentScreen(
    uid: String?,
    id: String?,
    type: String,
    paddingValues: PaddingValues,
    refreshState: Boolean?,
    resetRefreshState: () -> Unit,
    onViewUser: (String) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onCopyText: (String?) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onViewFFFList: (String?, String, String?, String?) -> Unit,
    searchQuery: String = "",
    topAppBarScrollBehavior: ScrollBehavior? = null,
) {

    val viewModel =
        hiltViewModel<FFFContentViewModel, FFFContentViewModel.ViewModelFactory>(key = id + uid + type) { factory ->
            factory.create(
                url = when (type) {
                    FFFListType.FEED.name -> "/v6/user/feedList?showAnonymous=0&isIncludeTop=1"
                    FFFListType.FOLLOW.name, FFFListType.USER_FOLLOW.name -> "/v6/user/followList"
                    FFFListType.APK.name -> "/v6/user/apkFollowList"
                    FFFListType.FAN.name -> "/v6/user/fansList"
                    FFFListType.RECENT.name -> "/v6/user/recentHistoryList"
                    FFFListType.LIKE.name -> "/v6/user/likeList"
                    FFFListType.REPLY.name -> "/v6/user/replyList"
                    FFFListType.REPLYME.name -> "/v6/user/replyToMeList"
                    FFFListType.COLLECTION.name -> "/v6/collection/list"
                    FFFListType.COLLECTION_ITEM.name -> "/v6/collection/itemList"
                    else -> EMPTY_STRING
                },
                uid = uid, id = id, showDefault = if (type == FFFListType.COLLECTION.name) 1 else null
            )
        }

    var showRecentDialog by remember { mutableStateOf(false) }
    val prefs = LocalUserPreferences.current
    val canManageCollections = type == FFFListType.COLLECTION.name && id == null &&
        prefs.isLogin && (uid == null || uid == prefs.uid)
    val folderListState = rememberLazyListState()
    val searchViewModel = if (type == FFFListType.COLLECTION.name) {
        hiltViewModel<CloudCollectionSearchViewModel>(key = "collection-search:$uid:${prefs.sessionGeneration}")
    } else null
    LaunchedEffect(searchQuery, uid, prefs.uid, prefs.sessionGeneration) {
        searchViewModel?.updateQuery(searchQuery, uid, prefs.uid, prefs.sessionGeneration)
    }
    DisposableEffect(searchViewModel) { onDispose { searchViewModel?.stop() } }

    if (searchViewModel != null && searchQuery.isNotBlank()) {
        Box(Modifier.fillMaxSize().then(topAppBarScrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier)) {
            CloudCollectionSearchResults(searchQuery, searchViewModel.state, paddingValues,
                searchViewModel::retry, onViewFeed)
        }
    } else CommonScreen(
        lazyListState = folderListState,
        topAppBarScrollBehavior = topAppBarScrollBehavior,
        needTopPadding = com.example.c001apk.compose.ui.component.design.isMiuixBlurEnabled(),
        viewModel = viewModel,
        refreshState = refreshState,
        resetRefreshState = resetRefreshState,
        paddingValues = paddingValues,
        onViewUser = onViewUser,
        onViewFeed = onViewFeed,
        onOpenLink = onOpenLink,
        onCopyText = onCopyText,
        onReport = onReport,
        onViewFFFList = onViewFFFList,
        onDeleteCollection = if (canManageCollections) ({ collection ->
            viewModel.requestDeleteCollection(collection, prefs.uid, prefs.sessionGeneration)
        }) else null,
        onHandleRecent = { actionId, targetId, targetType, isTop ->
            viewModel.actionId = actionId
            viewModel.targetId = targetId
            viewModel.targetType = targetType
            viewModel.isTop = isTop
            showRecentDialog = true
        },
    )

    if (canManageCollections) viewModel.collectionToDelete?.let { collection ->
        DeleteCollectionDialog(
            title = collection.title.orEmpty(),
            deleting = viewModel.deletingCollection,
            error = viewModel.collectionDeleteError,
            onDismiss = viewModel::cancelDeleteCollection,
            onConfirm = viewModel::deleteCollection,
        )
    }

    val context = LocalContext.current
    viewModel.toastText?.let {
        viewModel.resetToastText()
        context.makeToast(it)
    }

    when {
        showRecentDialog -> {
            if (com.example.c001apk.compose.ui.theme.isMiuix()) {
                com.example.c001apk.compose.ui.component.design.ActionDialog("常去操作", listOf(
                    MenuAction(if (viewModel.isTop == 1) "移除置顶" else "置顶") { viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.TOP) },
                    MenuAction("删除") { viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.DELETE) },
                    MenuAction("清空全部") { viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.DELETE_ALL) },
                ), { showRecentDialog = false })
            } else Dialog(onDismissRequest = { showRecentDialog = false }) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors()
                        .copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (viewModel.isTop == 1) "移除置顶" else "置顶",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRecentDialog = false
                                    viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.TOP)
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "删除",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRecentDialog = false
                                    viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.DELETE)
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "清空全部",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRecentDialog = false
                                    viewModel.onHandleRecentHistory(FFFContentViewModel.ActionType.DELETE_ALL)
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }

}
