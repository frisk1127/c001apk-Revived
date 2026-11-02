package com.example.c001apk.compose.ui.history

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.first
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.HistoryFavoriteRepo
import com.example.c001apk.compose.logic.backup.FavoriteBackup
import com.example.c001apk.compose.logic.backup.InvalidFavoriteBackupException
import com.example.c001apk.compose.logic.backup.FavoriteDocumentExport
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Created by bggRGjQaUbCoE on 2024/6/17
 */
@HiltViewModel(assistedFactory = HistoryViewModel.ViewModelFactory::class)
class HistoryViewModel @AssistedInject constructor(
    @Assisted val type: HistoryType = HistoryType.HISTORY,
    private val blackListRepo: BlackListRepo,
    private val historyFavoriteRepo: HistoryFavoriteRepo,
    private val networkRepo: com.example.c001apk.compose.logic.repository.NetworkRepo,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(type: HistoryType): HistoryViewModel
    }

    private val cloudData = MutableStateFlow<List<com.example.c001apk.compose.logic.model.FeedEntity>>(emptyList())
    val dataList = when (type) {
        HistoryType.FAV -> historyFavoriteRepo.loadAllFavoriteListFlow()
        HistoryType.HISTORY -> historyFavoriteRepo.loadAllHistoryListFlow()
        HistoryType.CLOUD -> cloudData.asStateFlow()
    }

    init {
        if (type == HistoryType.CLOUD) loadCloudFavorites()
    }

    private fun loadCloudFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = mutableListOf<com.example.c001apk.compose.logic.model.FeedEntity>()
            var page = 1
            var lastItem: String? = null
            while (page <= 100) {
                val state = networkRepo.getFavoriteList(page, null, lastItem).first {
                    it !is com.example.c001apk.compose.logic.state.LoadingState.Loading
                }
                val response = (state as? com.example.c001apk.compose.logic.state.LoadingState.Success)?.response.orEmpty()
                if (response.isEmpty()) break
                all += response.mapNotNull { item ->
                    val id = item.id ?: return@mapNotNull null
                    com.example.c001apk.compose.logic.model.FeedEntity(
                        id = id, uid = item.uid.orEmpty(), uname = item.username.orEmpty(),
                        avatar = item.userAvatar.orEmpty(), device = item.deviceTitle.orEmpty(),
                        message = item.message.orEmpty(), pubDate = item.dateline.toString(),
                        mediaJson = com.example.c001apk.compose.logic.model.FeedMediaJson.encode(item),
                    )
                }
                lastItem = response.lastOrNull()?.id
                if (response.size < 20 || lastItem.isNullOrBlank()) break
                page++
            }
            cloudData.value = all.distinctBy { it.id }
        }
    }

    private val mediaRequests = mutableSetOf<String>()
    private val mediaSlots = Semaphore(2)

    suspend fun loadMedia(id: String) {
        if (type !in listOf(HistoryType.FAV, HistoryType.CLOUD) || !mediaRequests.add(id)) return
        try {
            mediaSlots.withPermit {
                withContext(Dispatchers.IO) {
                    val response = networkRepo.getFeedContent("/v6/feed/detail?id=$id").first {
                        it !is com.example.c001apk.compose.logic.state.LoadingState.Loading
                    }
                    if (response is com.example.c001apk.compose.logic.state.LoadingState.Success) {
                        historyFavoriteRepo.updateMedia(id, com.example.c001apk.compose.logic.model.FeedMediaJson.encode(response.response))
                    }
                }
            }
        } catch (e: CancellationException) {
            mediaRequests.remove(id)
            throw e
        } catch (_: Exception) {
            // Keep the local text available when a post is unavailable or the network fails.
        }
    }

    var toastText by mutableStateOf<String?>(null)
        private set

    var isBackupBusy by mutableStateOf(false)
        private set

    fun exportFavorites(uri: Uri) = runBackup("导出") {
        val favorites = historyFavoriteRepo.loadAllFavorites()
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open backup output")
        output.bufferedWriter(Charsets.UTF_8).use { FavoriteBackup.write(favorites, it) }
        "已导出 ${favorites.size} 条收藏"
    }

    fun importFavorites(uri: Uri) = runBackup("导入") {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open backup input")
        val favorites = input.bufferedReader(Charsets.UTF_8).use(FavoriteBackup::read)
        val imported = historyFavoriteRepo.importFavorites(favorites)
        "已导入 $imported 条收藏，跳过 ${favorites.size - imported} 条重复记录"
    }

    fun exportFavoritesDocument(uri: Uri, html: Boolean) = runBackup("导出", allowCloud = true) {
        val favorites = if (type == HistoryType.CLOUD) cloudData.value else historyFavoriteRepo.loadAllFavorites()
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open export output")
        output.bufferedWriter(Charsets.UTF_8).use {
            if (html) FavoriteDocumentExport.writeHtml(favorites, it)
            else FavoriteDocumentExport.writeMarkdown(favorites, it)
        }
        "已导出 ${favorites.size} 条收藏"
    }

    private fun runBackup(action: String, allowCloud: Boolean = false, operation: suspend () -> String) {
        if ((type != HistoryType.FAV && !(allowCloud && type == HistoryType.CLOUD)) || isBackupBusy) return
        isBackupBusy = true
        viewModelScope.launch {
            try {
                toastText = withContext(Dispatchers.IO) { operation() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidFavoriteBackupException) {
                toastText = e.message
            } catch (e: Exception) {
                toastText = "${action}失败，请检查文件或存储位置后重试"
            } finally {
                isBackupBusy = false
            }
        }
    }

    fun resetToastText() {
        toastText = null
    }

    fun blockUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!blackListRepo.checkUid(uid)) {
                blackListRepo.saveUid(uid)
            }
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteFavByUid(uid)
                HistoryType.HISTORY -> historyFavoriteRepo.deleteHistoryByUid(uid)
                HistoryType.CLOUD -> cloudData.value = cloudData.value.filterNot { it.uid == uid }
            }
            toastText = "已拉黑并移除相关记录"
        }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteFavorite(id)
                HistoryType.HISTORY -> historyFavoriteRepo.deleteHistory(id)
                HistoryType.CLOUD -> networkRepo.unfavoriteFeed(id).collect {
                    if (it.isSuccess) cloudData.value = cloudData.value.filterNot { item -> item.id == id }
                }
            }
            toastText = "删除成功"
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                HistoryType.FAV -> historyFavoriteRepo.deleteAllFavorite()
                HistoryType.HISTORY -> historyFavoriteRepo.deleteAllHistory()
                HistoryType.CLOUD -> toastText = "云端收藏请逐条取消，避免误操作"
            }
            toastText = "清除成功"
        }
    }

}
