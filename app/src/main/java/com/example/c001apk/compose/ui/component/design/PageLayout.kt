package com.example.c001apk.compose.ui.component.design

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.c001apk.compose.ui.theme.isMiuix
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    topBarSurfaceAlpha: Float = .82f,
    content: @Composable (PaddingValues) -> Unit,
) {
    val miuix = isMiuix()
    val backdrop = rememberPageBackdrop()
    val navigationPadding = LocalNavigationPadding.current
    val view = androidx.compose.ui.platform.LocalView.current
    val tabBarHost = remember(backdrop, view) { TabBarHost(view, backdrop) }
    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides contentColor) {
        top.yukonga.miuix.kmp.basic.Scaffold(
            modifier = modifier,
            topBar = { BlurredBar(backdrop, surfaceAlpha = topBarSurfaceAlpha) { topBar() } },
            bottomBar = { BlurredBar(backdrop) { bottomBar() } }, snackbarHost = snackbarHost,
            floatingActionButton = { Box(Modifier.padding(bottom = navigationPadding)) { floatingActionButton() } }, contentWindowInsets = contentWindowInsets,
            containerColor = if (containerColor != Color.Unspecified) containerColor
                else if (miuix) MiuixTheme.colorScheme.surface else MaterialTheme.colorScheme.background,
        ) { padding ->
            val bodyPadding = PaddingValues(
                start = padding.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                bottom = padding.calculateBottomPadding() + navigationPadding,
            )
            Box(Modifier.consumeWindowInsets(padding)) {
                Box(Modifier.pageBackdrop(backdrop)) {
                    CompositionLocalProvider(LocalPageBackdrop provides backdrop, LocalTabBarHost provides tabBarHost) {
                        content(bodyPadding)
                    }
                }
                TabBarOverlayHost(tabBarHost, Modifier.matchParentSize())
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorTopInset: Dp = 0.dp,
    onPullProgress: ((Float) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    val body = remember { movableContentOf { Box(Modifier.fillMaxSize()) { currentContent() } } }
    if (isMiuix()) {
        val state = top.yukonga.miuix.kmp.basic.rememberPullToRefreshState()
        val headerInset = indicatorTopInset
        top.yukonga.miuix.kmp.basic.PullToRefresh(
            isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier,
            pullToRefreshState = state, contentPadding = PaddingValues(top = headerInset),
            onPullProgress = onPullProgress,
            color = MiuixTheme.colorScheme.primary,
            refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新", "刷新完成"),
            refreshTextStyle = MiuixTheme.textStyles.footnote1.copy(color = MiuixTheme.colorScheme.onSurfaceVariantSummary),
        ) { body() }
    } else androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier,
    ) { body() }
}
