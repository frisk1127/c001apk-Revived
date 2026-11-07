package com.example.c001apk.compose.ui.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.repository.CloudCollectionDocument
import com.example.c001apk.compose.logic.repository.CloudCollectionSearchRepo
import com.example.c001apk.compose.logic.repository.CloudCollectionSearchState
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class CloudCollectionSearchViewModel @Inject constructor(
    private val repository: CloudCollectionSearchRepo,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    var state by mutableStateOf(CloudCollectionSearchState())
        private set
    private var job: Job? = null
    private var account: Pair<String, Long>? = null
    private var targetUid: String? = null
    private var active = false
    private var cache = ConcurrentHashMap<String, CloudCollectionDocument>()

    fun updateQuery(query: String, uid: String?, accountUid: String, generation: Long) {
        val session = accountUid to generation
        if (account != session || targetUid != uid) {
            stop()
            account = session
            targetUid = uid
        }
        if (query.isBlank()) stop()
        else if (!active) {
            active = true
            search()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        active = false
        cache = ConcurrentHashMap()
        state = CloudCollectionSearchState()
    }

    fun retry() { if (active && job?.isActive != true) search() }

    private fun search() {
        val expected = account ?: return
        val bodyCache = cache
        val previousEntries = state.entries
        state = state.copy(loading = true, error = null)
        job = viewModelScope.launch {
            try {
                delay(300)
                val session = preferences.data.first()
                if ((session.uid to session.sessionGeneration) != expected) { stop(); return@launch }
                check(targetUid != null || session.isLogin) { "请先登录" }
                repository.scan(targetUid, bodyCache).collect { result ->
                    val current = preferences.data.first()
                    if ((current.uid to current.sessionGeneration) != expected) {
                        stop()
                        throw CancellationException("Session changed")
                    }
                    state = if (previousEntries.isNotEmpty() && (result.loading || result.error != null)) {
                        result.copy(entries = (previousEntries.associateBy { it.document.id } +
                            result.entries.associateBy { it.document.id }).values.toList())
                    } else result
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { state = state.copy(loading = false, started = true, error = e.message ?: "搜索失败，请重试") }
        }
    }
}
