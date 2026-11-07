package com.example.c001apk.compose.ui.component.design

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import com.example.c001apk.compose.ui.theme.isMiuix
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalRefreshContentOffset = staticCompositionLocalOf { 0.dp }

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
    topAppBarScrollBehavior: ScrollBehavior? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val miuix = isMiuix()
    val density = LocalDensity.current
    var contentOffset by remember(miuix) { mutableStateOf(0.dp) }
    val currentOffset by rememberUpdatedState(if (miuix) contentOffset else 0.dp)
    val currentInset by rememberUpdatedState(if (miuix) indicatorTopInset else 0.dp)
    val updateOffset by rememberUpdatedState<(Float) -> Unit> { y ->
        contentOffset = if (miuix) with(density) { y.coerceAtLeast(0f).toDp() } else 0.dp
    }
    val currentContent by rememberUpdatedState(content)
    val body = remember { movableContentOf {
        // Pinned overlays must share the displacement introduced by the native refresh header.
        Box(Modifier.fillMaxSize()
            .onGloballyPositioned { updateOffset(it.positionInParent().y) }
            .drawWithContent {
                val top = if (currentOffset > 0.dp) currentInset.toPx() else 0f
                clipRect(top = top.coerceIn(0f, size.height)) { this@drawWithContent.drawContent() }
            }) {
            CompositionLocalProvider(LocalRefreshContentOffset provides currentOffset) { currentContent() }
        }
    } }
    if (miuix) {
        val state = top.yukonga.miuix.kmp.basic.rememberPullToRefreshState()
        val headerInset = indicatorTopInset
        top.yukonga.miuix.kmp.basic.PullToRefresh(
            isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier,
            pullToRefreshState = state, contentPadding = PaddingValues(top = headerInset),
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            onPullProgress = onPullProgress,
            color = MiuixTheme.colorScheme.primary,
            refreshTexts = listOf("下拉刷新", "松开刷新", "正在刷新", "刷新完成"),
            refreshTextStyle = MiuixTheme.textStyles.footnote1.copy(color = MiuixTheme.colorScheme.onSurfaceVariantSummary),
        ) { body() }
    } else androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier,
    ) { body() }
}
