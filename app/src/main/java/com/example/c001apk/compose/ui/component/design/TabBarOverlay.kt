package com.example.c001apk.compose.ui.component.design

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal val LocalTabBarHost = staticCompositionLocalOf<TabBarHost?> { null }

internal class TabBarHost(val view: View, val backdrop: PageBackdrop?) {
    val entries = mutableStateListOf<TabBarEntry>()
    var coordinates: LayoutCoordinates? = null

    fun update(entry: TabBarEntry) {
        val root = coordinates?.takeIf { it.isAttached } ?: return
        val child = entry.coordinates?.takeIf { it.isAttached } ?: return
        try {
            val position = root.localPositionOf(child, Offset.Zero)
            val bounds = root.localBoundingBoxOf(child, clipBounds = true)
            val pinnedTop = entry.pinnedTop.value
            val y = if (pinnedTop != null) position.y.coerceAtLeast(pinnedTop.toFloat()) else position.y
            entry.position = IntOffset(position.x.roundToInt(), y.roundToInt())
            entry.clip = if (pinnedTop != null && position.y < pinnedTop) {
                Rect((-position.x).coerceAtLeast(0f), 0f,
                    minOf(entry.width.toFloat(), root.size.width - position.x),
                    minOf(entry.height.toFloat(), root.size.height - y).coerceAtLeast(0f))
            } else Rect(bounds.left - position.x, bounds.top - position.y,
                bounds.right - position.x, bounds.bottom - position.y)
        } catch (_: IllegalStateException) {
            entry.clip = Rect.Zero
        } catch (_: IllegalArgumentException) {
            entry.clip = Rect.Zero
        }
    }
}

internal class TabBarEntry(
    val backdrop: PageBackdrop,
    val locals: State<CompositionLocalContext>,
    val content: State<@Composable BoxScope.() -> Unit>,
    val pinnedTop: State<Int?>,
) {
    var width by mutableIntStateOf(0)
    var height by mutableIntStateOf(0)
    var position by mutableStateOf(IntOffset.Zero)
    var clip by mutableStateOf(Rect.Zero)
    var coordinates: LayoutCoordinates? = null
}

/** The captured subtree contains only a spacer; its blurred bar is drawn in a sibling layer. */
@Composable
internal fun TabBarSurface(
    modifier: Modifier = Modifier,
    estimatedHeight: Dp = 52.dp,
    pinnedTopInset: Dp? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val host = LocalTabBarHost.current
    val backdrop = LocalPageBackdrop.current
    if (LocalBlurredBar.current || backdrop == null || host == null || host.backdrop !== backdrop || host.view !== LocalView.current) {
        Box(modifier, content = content)
        return
    }
    val latestContent = rememberUpdatedState(content)
    val locals = rememberUpdatedState(currentCompositionLocalContext)
    val refreshOffset = LocalRefreshContentOffset.current
    val pinnedTop = rememberUpdatedState(with(LocalDensity.current) { pinnedTopInset?.let { (it + refreshOffset).roundToPx() } })
    val entry = remember(host, backdrop) { TabBarEntry(backdrop, locals, latestContent, pinnedTop) }
    LaunchedEffect(entry, pinnedTop.value) { host.update(entry) }
    DisposableEffect(host, entry) {
        host.entries.add(entry)
        onDispose {
            entry.coordinates = null
            host.entries.remove(entry)
        }
    }
    Box(modifier) {
        Layout(content = {}, modifier = Modifier
            .onSizeChanged { entry.width = it.width }
            .onGloballyPositioned { entry.coordinates = it; host.update(entry) }) { _, constraints ->
            val height = if (entry.height > 0) entry.height else estimatedHeight.roundToPx()
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
            layout(width, constraints.constrainHeight(height)) { }
        }
    }
}

@Composable
internal fun TabBarOverlayHost(host: TabBarHost, modifier: Modifier = Modifier) {
    DisposableEffect(host) {
        onDispose { host.coordinates = null }
    }
    val entries = host.entries.filter { it.width > 0 }
    Layout(modifier = modifier.onGloballyPositioned {
        host.coordinates = it
        host.entries.forEach(host::update)
    }, content = {
        entries.forEach { entry ->
            key(entry) {
                CompositionLocalProvider(entry.locals.value) {
                    BlurredBar(entry.backdrop, Modifier
                        .onSizeChanged { entry.height = it.height; host.update(entry) }
                        .drawWithContent {
                            val clip = entry.clip
                            clipRect(clip.left, clip.top, clip.right, clip.bottom) { this@drawWithContent.drawContent() }
                        }) { entry.content.value(this) }
                }
            }
        }
    }) { measurables, constraints ->
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints(minWidth = entries[index].width, maxWidth = entries[index].width,
                maxHeight = constraints.maxHeight))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val entry = entries[index]
                if (entry.clip.width > 0f && entry.clip.height > 0f) {
                    placeable.place(entry.position.x, entry.position.y)
                }
            }
        }
    }
}
