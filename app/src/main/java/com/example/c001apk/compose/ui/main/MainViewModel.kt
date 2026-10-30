package com.example.c001apk.compose.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.c001apk.compose.logic.repository.AccountRepository
import com.example.c001apk.compose.logic.repository.NetworkRepo
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.ui.base.PrefsViewModel
import com.example.c001apk.compose.util.CookieUtil
import com.example.c001apk.compose.util.encode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val networkRepo: NetworkRepo,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accounts: AccountRepository,
    private val deviceIdentity: com.example.c001apk.compose.logic.repository.DeviceIdentityRepository,
) : PrefsViewModel(userPreferencesRepository) {
    data class LoginDeviceIdNotice(val generation: Long, val success: Boolean, val message: String)
    var loginDeviceIdNotice by mutableStateOf<LoginDeviceIdNotice?>(null)
        private set
    fun dismissDeviceIdNotice() { loginDeviceIdNotice = null }
    var badge by mutableIntStateOf(0)
        private set
    private var navigationGeneration = -1L
    private var navigationStore = androidx.lifecycle.ViewModelStore()
    fun navigationOwner(
        generation: Long,
        parent: androidx.lifecycle.HasDefaultViewModelProviderFactory,
    ): androidx.lifecycle.ViewModelStoreOwner {
        if (navigationGeneration != generation) {
            navigationStore.clear()
            navigationStore = androidx.lifecycle.ViewModelStore()
            navigationGeneration = generation
        }
        return NavigationViewModelStoreOwner(navigationStore, parent)
    }

    override fun onCleared() {
        navigationStore.clear()
        super.onCleared()
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.data.map { it.pendingDeviceIdLogin }.distinctUntilChanged().collect { generation ->
                if (generation <= 0) return@collect
                val session = userPreferencesRepository.data.first()
                if (!session.isLogin || session.sessionGeneration != generation) {
                    accounts.finishLoginInitialization(generation)
                    return@collect
                }
                loginDeviceIdNotice = null
                val notice = try {
                    deviceIdentity.acquire(expectedSession = generation)
                    LoginDeviceIdNotice(generation, true, "已自动获取并保存数字联盟 ID。后续需要刷新时，可在“设置 → 数字联盟 ID”中手动操作。")
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) {
                    LoginDeviceIdNotice(generation, false, "数字联盟 ID 自动获取失败，已保留原值。你可以稍后在“设置 → 数字联盟 ID”中手动获取，或粘贴导入。")
                }
                accounts.finishLoginInitialization(generation)
                if (userPreferencesRepository.data.first().sessionGeneration == generation) loginDeviceIdNotice = notice
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.data.map { it.sessionGeneration to it.pendingDeviceIdLogin }.distinctUntilChanged().collectLatest { (generation, pending) ->
                badge = 0
                CookieUtil.SESSID = ""
                CookieUtil.atme = null; CookieUtil.atcommentme = null; CookieUtil.feedlike = null
                CookieUtil.contacts_follow = null; CookieUtil.message = null
                val session = userPreferencesRepository.data.first()
                if (pending > 0 && pending == generation) return@collectLatest
                networkRepo.checkLoginInfo().collect { result ->
                    if (userPreferencesRepository.data.first().sessionGeneration != generation) return@collect
                    val response = result.getOrNull() ?: return@collect
                    response.body()?.data?.takeIf { session.isLogin && it.uid == session.uid }?.let { login ->
                        badge = login.notifyCount.badge
                        CookieUtil.atme = login.notifyCount.atme
                        CookieUtil.atcommentme = login.notifyCount.atcommentme
                        CookieUtil.feedlike = login.notifyCount.feedlike
                        CookieUtil.contacts_follow = login.notifyCount.contactsFollow
                        CookieUtil.message = login.notifyCount.message
                        try { accounts.updateProfile(generation, login.uid, login.username.encode, login.userAvatar, token = login.token) }
                        catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (_: Exception) { /* Keep the last stored profile if secure storage is unavailable. */ }
                    }
                    if (session.isLogin && response.body()?.message == "登录信息有误") accounts.expire(generation)
                    if (userPreferencesRepository.data.first().sessionGeneration != generation) return@collect
                    response.headers().values("Set-Cookie").firstOrNull { it.startsWith("SESSID=") }
                        ?.let { CookieUtil.SESSID = it.substringBefore(';') }
                }
            }
        }
    }

    fun resetBadge() { badge = 0 }
}
