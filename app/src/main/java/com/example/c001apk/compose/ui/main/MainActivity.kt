package com.example.c001apk.compose.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import com.example.c001apk.compose.ui.component.design.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.example.c001apk.compose.ui.component.SuppressWindowFocusHighlight
import com.example.c001apk.compose.ui.search.SearchViewModel
import com.example.c001apk.compose.ui.update.ClientUpdateViewModel
import com.example.c001apk.compose.ui.update.ClientUpdateDialog
import com.example.c001apk.compose.ui.update.LocalClientUpdates
import com.example.c001apk.compose.util.CookieUtil.apiVersion
import com.example.c001apk.compose.util.CookieUtil.customHaptics
import com.example.c001apk.compose.util.CookieUtil.hapticFeedback
import com.example.c001apk.compose.util.CookieUtil.hapticStrength
import com.example.c001apk.compose.util.CookieUtil.imageFilter
import com.example.c001apk.compose.util.CookieUtil.imageQuality
import com.example.c001apk.compose.util.CookieUtil.isDarkMode
import com.example.c001apk.compose.util.CookieUtil.isLogin
import com.example.c001apk.compose.util.CookieUtil.materialYou
import com.example.c001apk.compose.util.CookieUtil.openInBrowser
import com.example.c001apk.compose.util.CookieUtil.recordHistory
import com.example.c001apk.compose.util.CookieUtil.reverseHapticEffects
import com.example.c001apk.compose.util.CookieUtil.themeType
import com.example.c001apk.compose.util.CookieUtil.paletteStyle
import com.example.c001apk.compose.util.CookieUtil.seedColor
import com.example.c001apk.compose.util.CookieUtil.pureBlack
import com.example.c001apk.compose.util.CookieUtil.fontScale
import com.example.c001apk.compose.util.CookieUtil.contentScale
import com.example.c001apk.compose.util.CookieUtil.sdkInt
import com.example.c001apk.compose.util.CookieUtil.showEmoji
import com.example.c001apk.compose.util.CookieUtil.showSquare
import com.example.c001apk.compose.util.CookieUtil.szlmId
import com.example.c001apk.compose.util.CookieUtil.token
import com.example.c001apk.compose.util.CookieUtil.uid
import com.example.c001apk.compose.util.CookieUtil.userAgent
import com.example.c001apk.compose.util.CookieUtil.username
import com.example.c001apk.compose.util.CookieUtil.versionCode
import com.example.c001apk.compose.util.CookieUtil.versionName
import com.example.c001apk.compose.util.CookieUtil.xAppDevice
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import kotlinx.coroutines.flow.first
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Created by bggRGjQaUbCoE on 2024/5/29
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    private val pendingDeepLinks = mutableStateListOf<String>()
    private val viewModel by viewModels<MainViewModel>()
    private val searchViewModel by viewModels<SearchViewModel>()
    private val clientUpdates by viewModels<ClientUpdateViewModel>()
    private var stableStatusBarInset: Int? = null
    private var initialThemeReady = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enqueueDeepLink(intent)
    }

    private fun enqueueDeepLink(intent: Intent) {
        intent.dataString?.let(pendingDeepLinks::add)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(PENDING_DEEP_LINKS, ArrayList(pendingDeepLinks))
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !initialThemeReady }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            enqueueDeepLink(intent)
        } else {
            pendingDeepLinks.addAll(savedInstanceState.getStringArrayList(PENDING_DEEP_LINKS).orEmpty())
        }
        enableEdgeToEdge()
        setupStableStatusBarInsets()

        setContent {

            // 窗口重新获得焦点时，Compose 会把焦点补到第一个可聚焦节点上，
            // 导致左上角按钮/Tab 长时间显示 Focus 高亮（看起来像被按住）。
            SuppressWindowFocusHighlight()

            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()

            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(
                    initialValue = null,
                    lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                )

            val preferences = if (userPreferences == null) {
                return@setContent
            } else {
                checkNotNull(userPreferences)
            }

            val navController = androidx.compose.runtime.key(preferences.sessionGeneration) { rememberAppNavController() }
            val navigationOwner = androidx.compose.runtime.remember(preferences.sessionGeneration) { viewModel.navigationOwner(preferences.sessionGeneration) }

            CompositionLocalProvider(
                LocalUserPreferences provides preferences,
                LocalClientUpdates provides clientUpdates,
            ) {
                if (preferences.xAppDevice.isEmpty())
                    viewModel.regenerateParams()

                isLogin = preferences.isLogin
                szlmId = preferences.szlmId
                xAppDevice = preferences.xAppDevice
                uid = preferences.uid
                username = preferences.username
                token = preferences.token
                userAgent = preferences.userAgent
                sdkInt = preferences.sdkInt
                versionName = preferences.versionName
                versionCode = preferences.versionCode
                apiVersion = preferences.apiVersion
                imageQuality = preferences.imageQuality
                showEmoji = preferences.showEmoji
                showSquare = preferences.showSquare
                openInBrowser = preferences.openInBrowser
                imageFilter = preferences.imageFilter
                recordHistory = preferences.recordHistory
                materialYou = preferences.materialYou
                isDarkMode = preferences.isDarkMode()
                themeType = preferences.themeType
                paletteStyle = preferences.paletteStyle
                seedColor = preferences.seedColor
                pureBlack = preferences.pureBlack
                fontScale = preferences.fontScale
                contentScale = preferences.contentScale
                hapticFeedback = preferences.hapticFeedback
                hapticStrength = preferences.hapticStrength
                customHaptics = preferences.customHaptics
                reverseHapticEffects = preferences.reverseHapticEffects
                com.example.c001apk.compose.util.CookieUtil.uiStyle = preferences.uiStyle

                C001apkComposeTheme(
                    darkTheme = preferences.isDarkMode(),
                    themeType = preferences.themeType,
                    seedColor = preferences.seedColor,
                    materialYou = preferences.materialYou,
                    pureBlack = preferences.pureBlack,
                    paletteStyle = preferences.paletteStyle,
                    fontScale = preferences.fontScale,
                    contentScale = preferences.contentScale,
                    uiStyle = preferences.uiStyle,
                ) {
                    androidx.compose.runtime.SideEffect { initialThemeReady = true }
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (preferences.pendingDeviceIdLogin > 0 && preferences.pendingDeviceIdLogin == preferences.sessionGeneration) {
                            LoginInitialization()
                        } else androidx.compose.runtime.key(preferences.sessionGeneration) {
                            CompositionLocalProvider(androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner provides navigationOwner) {
                                MainNavigation(
                                    navController = navController,
                                    badge = viewModel.badge,
                                    resetBadge = viewModel::resetBadge,
                                    widthSizeClass = widthSizeClass,
                                )
                            }
                        }
                        viewModel.loginDeviceIdNotice?.takeIf { it.generation == preferences.sessionGeneration }?.let {
                            LoginDeviceIdNotice(it, viewModel::dismissDeviceIdNotice)
                        }
                        if (lifecycleState == androidx.lifecycle.Lifecycle.State.RESUMED &&
                            preferences.pendingDeviceIdLogin == 0L && viewModel.loginDeviceIdNotice == null) {
                            ClientUpdateDialog(clientUpdates)
                        }
                    }
                }
            }

            LaunchedEffect(navController) {
                // 冷启动的新 Intent 可能早于首次组合，等导航图就绪后再消费链接。
                navController.currentBackStackEntryFlow.first()
                snapshotFlow { pendingDeepLinks.toList() }.collect { links ->
                    links.forEach { url ->
                        navController.onOpenLink(
                            context = this@MainActivity,
                            url = url,
                            title = null,
                            needConvert = true,
                            onSearch = searchViewModel::saveHistory,
                        )
                        pendingDeepLinks.removeAt(0)
                    }
                }
            }

        }

    }

    private fun setupStableStatusBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val currentTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val stableTop = stableStatusBarInset
                ?: currentTop.takeIf { it > 0 }
                ?: fallbackStatusBarHeight()
            if (stableStatusBarInset == null && stableTop > 0) {
                stableStatusBarInset = stableTop
            }
            if (stableTop == 0) {
                return@setOnApplyWindowInsetsListener insets
            }
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, stableTop, 0, 0))
                .build()
        }
    }

    private fun fallbackStatusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    private companion object {
        const val PENDING_DEEP_LINKS = "pendingDeepLinks"
    }
}
