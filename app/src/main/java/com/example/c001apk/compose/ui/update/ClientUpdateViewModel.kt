package com.example.c001apk.compose.ui.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.logic.model.ClientRelease
import com.example.c001apk.compose.logic.repository.ClientUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

val LocalClientUpdates = staticCompositionLocalOf<ClientUpdateViewModel?> { null }

data class ClientUpdateNotice(val release: ClientRelease? = null, val error: String? = null) {
    val hasUpdate: Boolean get() = (release?.versionCode ?: 0) > BuildConfig.VERSION_CODE
}

@HiltViewModel
class ClientUpdateViewModel @Inject constructor(
    private val repository: ClientUpdateRepository,
    private val store: DataStore<UserPreferencesCompat>,
) : ViewModel() {
    var checking by mutableStateOf(false)
        private set
    var notice by mutableStateOf<ClientUpdateNotice?>(null)
        private set

    init { check(manual = false) }

    fun check(manual: Boolean = true) {
        if (checking) return
        checking = true
        viewModelScope.launch {
            try {
                val prefs = store.data.first()
                val now = System.currentTimeMillis()
                if (!manual && (!prefs.autoCheckClientUpdate ||
                            now - prefs.lastClientUpdateCheck in 0 until 6 * 60 * 60 * 1000L)) return@launch
                store.updateData { it.copy(lastClientUpdateCheck = now) }
                val release = repository.latest()
                val result = ClientUpdateNotice(release = release)
                if (manual || (result.hasUpdate && store.data.first().autoCheckClientUpdate)) notice = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (manual) notice = ClientUpdateNotice(error =
                    (e as? IllegalStateException)?.message ?: "检查失败，请检查网络连接，或直接打开 GitHub 发布页。")
            } finally {
                checking = false
            }
        }
    }

    fun setAutomatic(enabled: Boolean) {
        viewModelScope.launch { store.updateData { it.copy(autoCheckClientUpdate = enabled) } }
    }

    fun dismiss() { notice = null }
}
