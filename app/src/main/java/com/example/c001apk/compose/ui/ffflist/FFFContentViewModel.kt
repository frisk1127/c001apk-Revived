package com.example.c001apk.compose.ui.ffflist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.repository.BlackListRepo
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.logic.state.LoadingState
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.ui.base.BaseViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by bggRGjQaUbCoE on 2024/6/12
 */
@HiltViewModel(assistedFactory = FFFContentViewModel.ViewModelFactory::class)
class FFFContentViewModel @AssistedInject constructor(
    @Assisted("url") val url: String,
    @Assisted("uid") val uid: String?,
    @Assisted("id") val id: String?,
    @Assisted val showDefault: Int?,
    networkRepo: NetworkRepo,
    blackListRepo: BlackListRepo,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel(networkRepo, blackListRepo) {

    @AssistedFactory
    interface ViewModelFactory {
        fun create(
            @Assisted("url") url: String,
            @Assisted("uid") uid: String?,
            @Assisted("id") id: String?,
            @Assisted showDefault: Int?,
        ): FFFContentViewModel
    }

    private val deletedCollectionIds = ConcurrentHashMap.newKeySet<String>()

    init {
        fetchData()
    }

    override suspend fun customFetchData() =
        networkRepo.getFollowList(url, uid, id, showDefault, page, lastItem)

    private var collectionCursor: String? = null

    override fun handleResponse(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data>? {
        if (url.startsWith("/v6/collection/")) {
            val cursor = response.lastOrNull()?.let { it.entityId ?: it.id }
            if (page > 2 && cursor != null && cursor == collectionCursor) isEnd = true
            collectionCursor = cursor
            lastItem = cursor
        }
        return if (url == "/v6/collection/list") response.filterNot { it.id?.let(deletedCollectionIds::contains) == true } else null
    }

    override fun handleLoadMore(response: List<HomeFeedResponse.Data>): List<HomeFeedResponse.Data>? =
        if (url.startsWith("/v6/collection/")) response
            .filterNot { url == "/v6/collection/list" && it.id?.let(deletedCollectionIds::contains) == true }
            .distinctBy { it.entityId ?: it.id } else null

    var collectionToDelete by mutableStateOf<HomeFeedResponse.Data?>(null)
        private set
    var deletingCollection by mutableStateOf(false)
        private set
    var collectionDeleteError by mutableStateOf<String?>(null)
        private set
    private var deleteAccountUid: String? = null
    private var deleteSessionGeneration: Long? = null

    fun requestDeleteCollection(collection: HomeFeedResponse.Data, accountUid: String, sessionGeneration: Long) {
        if (url != "/v6/collection/list" || id != null || deletingCollection) return
        if (accountUid.isBlank() || (uid != null && uid != accountUid) ||
            (!collection.uid.isNullOrBlank() && collection.uid != accountUid) ||
            collection.id?.toLongOrNull()?.let { it > 0 } != true) return
        deleteAccountUid = accountUid
        deleteSessionGeneration = sessionGeneration
        collectionToDelete = collection
        collectionDeleteError = null
    }

    fun cancelDeleteCollection() {
        if (!deletingCollection) {
            collectionToDelete = null
            collectionDeleteError = null
        }
    }

    fun deleteCollection() {
        val collection = collectionToDelete ?: return
        val collectionId = collection.id ?: return
        if (deletingCollection) return
        deletingCollection = true
        collectionDeleteError = null
        viewModelScope.launch {
            try {
                val session = userPreferencesRepository.data.first()
                check(session.isLogin && session.uid == deleteAccountUid &&
                    session.sessionGeneration == deleteSessionGeneration) { "账号已切换，请重新打开收藏夹" }
                networkRepo.deleteCollection(collectionId)
                val current = userPreferencesRepository.data.first()
                if (current.sessionGeneration != session.sessionGeneration || current.uid != session.uid) return@launch
                deletedCollectionIds.add(collectionId)
                (loadingState as? LoadingState.Success)?.response?.let { items ->
                    val remaining = items.filterNot { it.id == collectionId }
                    loadingState = if (remaining.isEmpty()) LoadingState.Empty else LoadingState.Success(remaining)
                    if (remaining.isEmpty() && !isEnd && !isLoadMore && !isRefreshing) loadMore()
                }
                collectionToDelete = null
                toastText = "收藏夹已删除"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (userPreferencesRepository.data.first().sessionGeneration == deleteSessionGeneration) {
                    collectionDeleteError = e.message ?: "删除失败，请重试"
                }
            } finally {
                deletingCollection = false
            }
        }
    }

    enum class ActionType {
        TOP, DELETE, DELETE_ALL
    }

    var isTop = 0
    lateinit var actionId: String
    lateinit var targetId: String
    lateinit var targetType: String

    fun onHandleRecentHistory(actionType: ActionType) {
        val url = "/v6/user/" + when (actionType) {
            ActionType.TOP -> if (isTop == 0) "addToTop" else "removeFromTop"
            ActionType.DELETE -> "delete"
            ActionType.DELETE_ALL -> "clear"
        } + "RecentHistory"
        val postData = when (actionType) {
            ActionType.TOP -> if (isTop == 1) mapOf("id" to actionId)
            else mapOf(
                "targetId" to targetId,
                "targetType" to targetType,
            )

            ActionType.DELETE -> mapOf("id" to actionId)
            ActionType.DELETE_ALL -> mapOf()
        }
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.postDelete(url, postData)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            toastText = data.message
                        } else if (data.data?.count?.contains("成功") == true) {
                            var response = (loadingState as LoadingState.Success).response
                            response = when (actionType) {
                                ActionType.TOP -> {
                                    if (isTop == 0) {
                                        val actionItem = response.find { it.targetId == targetId }
                                        response.toMutableList().also {
                                            it.remove(actionItem)
                                            actionItem?.let { item ->
                                                item.isTop = 1
                                                it.add(0, item)
                                            }
                                        }
                                    } else {
                                        response.map {
                                            if (it.targetId == targetId) it.copy(isTop = 0)
                                            else it
                                        }
                                    }
                                }

                                ActionType.DELETE -> response.filterNot { item -> item.targetId == targetId }
                                ActionType.DELETE_ALL -> emptyList()
                            }
                            loadingState = LoadingState.Success(response)
                            toastText = data.data.count
                        }
                    } else {
                        toastText = result.exceptionOrNull()?.message ?: "response is null"
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

}
