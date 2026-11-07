package com.example.c001apk.compose.ui.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.model.CloudCollection
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.util.CookieUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class FeedCollectionState(
    val collections: List<CloudCollection> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val originalIds: Set<String> = emptySet(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val createdId: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel(assistedFactory = FeedCollectionViewModel.Factory::class)
class FeedCollectionViewModel @AssistedInject constructor(
    @Assisted private val feedId: String,
    private val networkRepo: NetworkRepo,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(feedId: String): FeedCollectionViewModel
    }

    var state by mutableStateOf(FeedCollectionState())
        private set

    fun load() {
        if (state.loading || state.saving) return
        if (!CookieUtil.isLogin) {
            state = FeedCollectionState(error = "请先登录")
            return
        }
        state = FeedCollectionState(loading = true)
        viewModelScope.launch {
            try {
                val collections = networkRepo.getFeedCollections(feedId)
                val selected = collections.filter { it.isCollected == 1 }.mapNotNull { it.id }.toSet()
                state = FeedCollectionState(collections, selected, selected, loaded = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "加载收藏夹失败，请重试")
            } finally {
                state = state.copy(loading = false)
            }
        }
    }

    fun toggle(id: String) {
        if (!state.loaded || state.saving || state.collections.none { it.id == id }) return
        val selected = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
        state = state.copy(selectedIds = selected, error = null)
    }

    fun save() {
        if (!state.loaded || state.loading || state.saving) return
        if (state.selectedIds == state.originalIds) {
            state = state.copy(saved = true)
            return
        }
        state = state.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                saveSelection()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "收藏失败，请重试")
            } finally {
                state = state.copy(saving = false)
            }
        }
    }

    fun createAndSave(title: String, isPublic: Boolean) {
        if (!state.loaded || state.saving || title.isBlank()) return
        state = state.copy(saving = true, error = null)
        viewModelScope.launch {
            var created = false
            try {
                val collection = networkRepo.createCollection(title, isPublic)
                val id = checkNotNull(collection.id)
                // Retain the new folder if adding the feed fails, so retrying does not create duplicates.
                state = state.copy(
                    collections = state.collections + collection,
                    selectedIds = state.selectedIds + id,
                    createdId = id,
                )
                created = true
                saveSelection()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "操作失败，请重试"
                state = state.copy(error = if (created) "收藏夹已创建，动态尚未收藏：$message" else message)
            } finally {
                state = state.copy(saving = false)
            }
        }
    }

    private suspend fun saveSelection() {
        networkRepo.updateFeedCollections(feedId, state.selectedIds, state.originalIds - state.selectedIds)
        state = state.copy(originalIds = state.selectedIds, saved = true)
    }

    fun consumeSaved() {
        state = state.copy(saved = false)
    }
}
