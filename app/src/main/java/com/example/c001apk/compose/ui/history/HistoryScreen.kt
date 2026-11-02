package com.example.c001apk.compose.ui.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Search
import com.example.c001apk.compose.ui.component.design.AlertDialog
import com.example.c001apk.compose.ui.component.design.CircularProgressIndicator
import com.example.c001apk.compose.ui.component.design.DropdownMenu
import com.example.c001apk.compose.ui.component.design.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.c001apk.compose.ui.component.design.Icon
import com.example.c001apk.compose.ui.component.design.IconButton
import com.example.c001apk.compose.ui.component.design.Scaffold
import com.example.c001apk.compose.ui.component.design.Text
import com.example.c001apk.compose.ui.component.design.TextButton
import com.example.c001apk.compose.ui.component.design.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.cards.HistoryCard
import com.example.c001apk.compose.ui.component.cards.LoadingCard
import com.example.c001apk.compose.ui.component.design.SearchInputBar
import androidx.compose.ui.focus.FocusRequester
import com.example.c001apk.compose.util.ReportType
import com.example.c001apk.compose.util.makeToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by bggRGjQaUbCoE on 2024/6/17
 */

enum class HistoryType { FAV, CLOUD, HISTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit,
    type: String,
    onViewUser: (String) -> Unit,
    onReport: (String, ReportType) -> Unit,
    onOpenLink: (String, String?) -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
    onCopyText: (String?) -> Unit,
) {

    val viewModel =
        hiltViewModel<HistoryViewModel, HistoryViewModel.ViewModelFactory>(key = type) { factory ->
            factory.create(HistoryType.valueOf(type))
        }
    val dataList by viewModel.dataList.collectAsStateWithLifecycle(initialValue = emptyList())

    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showBackupMenu by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    val visibleData = remember(dataList, searchQuery) {
        if (searchQuery.isBlank()) dataList else dataList.filter {
            listOf(it.message, it.uname, it.device).joinToString(" ").contains(searchQuery, ignoreCase = true)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportFavorites) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importFavorites) }
    var exportHtml by remember { mutableStateOf(false) }
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> uri?.let { viewModel.exportFavoritesDocument(it, exportHtml) } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (type in listOf(HistoryType.FAV.name, HistoryType.CLOUD.name) && searchMode) {
                SearchInputBar(query = searchQuery, onQueryChange = { searchQuery = it },
                    onSearch = { searchQuery = it }, hint = "搜索本地收藏",
                    onBack = { searchMode = false; searchQuery = "" }, focusRequester = searchFocus)
            } else {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .only(WindowInsetsSides.Start + WindowInsetsSides.Top),
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = when (type) {
                        HistoryType.FAV.name -> "本地收藏"
                        HistoryType.CLOUD.name -> "云端收藏"
                        HistoryType.HISTORY.name -> "浏览历史"
                        else -> type
                    },
                actions = {
                    if (type in listOf(HistoryType.FAV.name, HistoryType.CLOUD.name)) {
                        IconButton(onClick = { searchMode = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索收藏")
                        }
                    }
                    AnimatedVisibility(
                        visible = dataList.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            enabled = !viewModel.isBackupBusy,
                            onClick = com.example.c001apk.compose.ui.component.rememberHapticClick { showClearDialog = true }
                        ) {
                            Icon(imageVector = Icons.Default.ClearAll, contentDescription = "清空列表")
                        }
                    }
                    if (type in listOf(HistoryType.FAV.name, HistoryType.CLOUD.name)) {
                        Box {
                            IconButton(
                                enabled = !viewModel.isBackupBusy,
                                onClick = com.example.c001apk.compose.ui.component.rememberHapticClick { showBackupMenu = true }
                            ) {
                                if (viewModel.isBackupBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(Icons.Default.MoreVert, contentDescription = "收藏备份")
                                }
                            }
                            DropdownMenu(
                                expanded = showBackupMenu,
                                onDismissRequest = { showBackupMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("导入收藏") },
                                    enabled = type == HistoryType.FAV.name,
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    onClick = com.example.c001apk.compose.ui.component.rememberHapticClick {
                                        showBackupMenu = false
                                        try {
                                            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                                        } catch (e: Exception) {
                                            context.makeToast("无法打开文件选择器")
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("导出收藏") },
                                    leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) },
                                    enabled = type == HistoryType.FAV.name && dataList.isNotEmpty(),
                                    onClick = com.example.c001apk.compose.ui.component.rememberHapticClick {
                                        showBackupMenu = false
                                        val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date())
                                        try {
                                            exportLauncher.launch("c001apk_favorites_$date.json")
                                        } catch (e: Exception) {
                                            context.makeToast("无法打开文件选择器")
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("导出 Markdown") },
                                    enabled = dataList.isNotEmpty(),
                                    onClick = com.example.c001apk.compose.ui.component.rememberHapticClick {
                                        showBackupMenu = false; exportHtml = false
                                        documentLauncher.launch("c001apk_favorites.md")
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("导出 HTML") },
                                    enabled = dataList.isNotEmpty(),
                                    onClick = com.example.c001apk.compose.ui.component.rememberHapticClick {
                                        showBackupMenu = false; exportHtml = true
                                        documentLauncher.launch("c001apk_favorites.html")
                                    },
                                )
                            }
                        }
                    }
                }
            )
            }
        },
    ) { paddingValues ->
        val blur = com.example.c001apk.compose.ui.component.design.isMiuixBlurEnabled()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (blur) 0.dp else paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(layoutDirection),
                ),
            contentPadding = PaddingValues(
                start = 10.dp,
                top = 10.dp + if (blur) paddingValues.calculateTopPadding() else 0.dp,
                end = 10.dp,
                bottom = 10.dp + paddingValues.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(visibleData, key = { it.id }) {
                androidx.compose.runtime.LaunchedEffect(it.id, it.mediaJson) {
                    if (it.mediaJson == null) viewModel.loadMedia(it.id)
                }
                HistoryCard(
                    data = it,
                    onViewUser = onViewUser,
                    onReport = onReport,
                    onDelete = { id ->
                        viewModel.delete(id)
                    },
                    onBlockUser = { uid ->
                        viewModel.blockUser(uid)
                    },
                    onOpenLink = onOpenLink,
                    onViewFeed = onViewFeed,
                    onCopyText = onCopyText,
                )
            }

        }

        if (visibleData.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingCard(state = LoadingState.Empty)
            }
        }

    }

    when {
        showClearDialog -> {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearAll()
                        }) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                        }) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                },
                title = {
                    Text(text = "确定清除全部？", modifier = Modifier.fillMaxWidth())
                }
            )
        }
    }

    LaunchedEffect(viewModel.toastText) {
        viewModel.toastText?.let {
            context.makeToast(it)
            viewModel.resetToastText()
        }
    }

}
