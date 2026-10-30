package com.example.c001apk.compose.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.*
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.example.c001apk.compose.UiStyle
import com.example.c001apk.compose.ui.component.SlideTransition
import com.example.c001apk.compose.ui.theme.LocalUiStyle
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.nav.core.*
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberAppNavController(): NavHostController = rememberNavController()

class AppGraphBuilder {
    internal data class Page(
        val route: String,
        val arguments: List<NamedNavArgument>,
        val content: @Composable (NavBackStackEntry) -> Unit,
    )
    internal val pages = mutableListOf<Page>()

    fun appComposable(
        route: String,
        arguments: List<NamedNavArgument> = emptyList(),
        content: @Composable (NavBackStackEntry) -> Unit,
    ) {
        pages += Page(route, arguments, content)
    }
}

private class PageKey(val entry: NavBackStackEntry) : NavKey {
    override fun equals(other: Any?) = other is PageKey && entry.id == other.entry.id
    override fun hashCode() = entry.id.hashCode()
    override fun toString() = entry.id
}

/** Routes and entry state are shared; each style owns its navigation and back gesture runtime. */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    isPane: Boolean = false,
    backEnabled: Boolean = true,
    builder: AppGraphBuilder.() -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val storeOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val definitions = remember(navController, startDestination, builder) { AppGraphBuilder().apply(builder) }
    val latestDefinitions = rememberUpdatedState(definitions)
    val navigator = navController.navigatorProvider.getNavigator<ComposeNavigator>("composable")
    val activeEntry by navController.currentBackStackEntryAsState()
    val latestEntryId = rememberUpdatedState(activeEntry?.id)
    val stateHolder = rememberSaveableStateHolder()
    val requestedStyle = LocalUiStyle.current
    var hostStyle by remember { mutableStateOf(requestedStyle) }
    // Let the old host save and detach before the other runtime attaches the same entry.
    LaunchedEffect(requestedStyle) {
        withFrameNanos { }
        hostStyle = requestedStyle
    }
    val contents = remember(hostStyle) { mutableMapOf<NavBackStackEntry, @Composable () -> Unit>() }
    val render: @Composable (NavBackStackEntry) -> Unit = { entry ->
        // A fresh host gets a fresh composition with stable saveable keys, never a detached tree.
        contents.getOrPut(entry) {
            movableContentOf {
                entry.LocalOwnersProvider(stateHolder) {
                    val isTop = latestEntryId.value == entry.id
                    val pageBackground = if (LocalUiStyle.current == UiStyle.MIUIX)
                        Modifier.background(MiuixTheme.colorScheme.surface) else Modifier
                    Box(Modifier.fillMaxSize().then(pageBackground)
                        .then(if (isTop) Modifier else Modifier.clearAndSetSemantics {})) {
                        latestDefinitions.value.pages.first { it.route == entry.destination.route }.content(entry)
                    }
                }
            }
        }.invoke()
    }
    val latestRender = rememberUpdatedState(render)
    val graph = remember(navController, startDestination, definitions) {
        navController.createGraph(startDestination = startDestination) {
            definitions.pages.forEach { page ->
                composable(page.route, page.arguments) { entry -> latestRender.value(entry) }
            }
        }
    }
    navController.setViewModelStore(storeOwner.viewModelStore)
    navController.graph = graph
    val entries by navigator.backStack.collectAsState()
    SideEffect { contents.keys.removeAll { it.lifecycle.currentState == androidx.lifecycle.Lifecycle.State.DESTROYED } }
    DisposableEffect(navController, lifecycleOwner) {
        navController.setLifecycleOwner(lifecycleOwner)
        onDispose { }
    }

    if (hostStyle != requestedStyle) {
        Box(modifier)
    } else if (hostStyle == UiStyle.MD3) {
        NavHost(
            navController = navController,
            graph = graph,
            modifier = modifier,
            enterTransition = { SlideTransition.slideLeft.enterTransition() },
            exitTransition = { SlideTransition.slideLeft.exitTransition() },
            popEnterTransition = { SlideTransition.slideRight.enterTransition() },
            popExitTransition = { SlideTransition.slideRight.exitTransition() },
        )
    } else {
        MiuixNavHost(navController, navigator, entries, modifier, isPane, backEnabled, render)
    }
}

@Composable
private fun MiuixNavHost(
    controller: NavHostController,
    navigator: ComposeNavigator,
    entries: List<NavBackStackEntry>,
    modifier: Modifier,
    isPane: Boolean,
    backEnabled: Boolean,
    render: @Composable (NavBackStackEntry) -> Unit,
) {
    val keys = entries.map(::PageKey)
    val stack = remember(controller) { navBackStackOf().apply { addAll(keys) } }
    SideEffect {
        if (stack.toList() != keys) Snapshot.withMutableSnapshot {
            stack.clear()
            stack.addAll(keys)
        }
    }
    val visible by controller.visibleEntries.collectAsState()
    val latestVisible = rememberUpdatedState(visible)
    val cornerRadius = rememberNavSystemCornerRadius()
    val parentDispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current)
    val backOwner = remember(parentDispatcher) {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = NavigationEventDispatcher(parent = parentDispatcher.navigationEventDispatcher)
        }
    }
    SideEffect {
        try { backOwner.navigationEventDispatcher.isEnabled = backEnabled }
        catch (_: IllegalStateException) { /* Activity disposal also disposes its dispatcher tree. */ }
    }
    DisposableEffect(backOwner) {
        onDispose {
            latestVisible.value.forEach(navigator::onTransitionComplete)
            try { backOwner.navigationEventDispatcher.dispose() }
            catch (_: IllegalStateException) { /* Parent disposal already released this dispatcher. */ }
        }
    }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides backOwner) {
        if (stack.isNotEmpty()) NavDisplay(
            backStack = stack,
            modifier = modifier,
            onBack = { controller.popBackStack() },
            transition = NavTransitions.MiuixDefault,
            effects = NavDisplayEffects(
                cornerClipRadius = if (isPane) 0.dp else cornerRadius,
                backdropColor = MiuixTheme.colorScheme.surface,
            ),
        ) {
            entry<PageKey>(contentKey = { it.entry.id }) { page ->
                val motion = LocalNavTransitionScope.current
                LaunchedEffect(motion, navigator) {
                    snapshotFlow { motion.isRunning }.collectLatest { running ->
                        if (running) navigator.backStack.value.forEach(navigator::prepareForTransition) else {
                            withFrameNanos { }
                            if (!motion.isRunning) latestVisible.value.forEach(navigator::onTransitionComplete)
                        }
                    }
                }
                DisposableEffect(page.entry) {
                    onDispose { navigator.onTransitionComplete(page.entry) }
                }
                render(page.entry)
            }
        }
    }
}
