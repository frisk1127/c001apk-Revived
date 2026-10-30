package com.example.c001apk.compose.ui.component.design

import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.theme.isMiuix
import java.util.WeakHashMap
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal class ModalBlurState {
    val overlays = mutableStateMapOf<Any, Unit>()
}
private val activityBlurStates = WeakHashMap<Activity, ModalBlurState>()
internal val LocalModalBlur = staticCompositionLocalOf<ModalBlurState?> { null }
internal val LocalBlurredBar = compositionLocalOf { false }
internal val LocalNavigationPadding = compositionLocalOf { 0.dp }
internal val LocalPageBackdrop = compositionLocalOf<PageBackdrop?> { null }
internal val LocalHomePadding = compositionLocalOf { PaddingValues() }

@Composable
fun isMiuixBlurEnabled(): Boolean = isMiuix() && LocalUserPreferences.current.blurEnabled && isRuntimeShaderSupported()

@Composable
fun isMiuixWindowBlurEnabled(): Boolean = isMiuixBlurEnabled() &&
    LocalUserPreferences.current.blurScope != com.example.c001apk.compose.BlurScope.BARS_ONLY

@Composable
internal fun rememberModalBlurState(): ModalBlurState {
    val activity = LocalActivity.current
    return remember(activity) {
        if (activity == null) ModalBlurState() else activityBlurStates.getOrPut(activity) { ModalBlurState() }
    }
}

@Composable
fun GlobalPopupBlur(visible: Boolean) {
    val state = LocalModalBlur.current ?: rememberModalBlurState()
    val token = remember { Any() }
    val active = visible && isMiuixWindowBlurEnabled()
    DisposableEffect(state, active) {
        if (active) state.overlays[token] = Unit
        onDispose { state.overlays.remove(token) }
    }
}

@Composable
internal fun Modifier.modalBackgroundBlur(state: ModalBlurState): Modifier {
    val enabled = isMiuixWindowBlurEnabled()
    val radius by animateFloatAsState(if (enabled && state.overlays.isNotEmpty()) 18f else 0f,
        animationSpec = tween(200), label = "modal-background-blur")
    if (!enabled || radius <= .1f) return this
    return graphicsLayer {
        renderEffect = if (enabled && Build.VERSION.SDK_INT >= 33 && radius > .1f) {
            RenderEffect.createBlurEffect(radius * density, radius * density, Shader.TileMode.CLAMP).asComposeRenderEffect()
        } else null
    }
}

internal class PageBackdrop(val layer: LayerBackdrop) {
    var position by mutableStateOf(Offset.Zero)
}

internal fun Modifier.pageBackdrop(backdrop: PageBackdrop?): Modifier = if (backdrop == null) this else
    onGloballyPositioned { backdrop.position = it.positionInWindow() }.layerBackdrop(backdrop.layer)

@Composable
internal fun rememberPageBackdrop(): PageBackdrop? {
    if (!isMiuixBlurEnabled()) return null
    val background = MiuixTheme.colorScheme.surface
    val layer = rememberLayerBackdrop { drawRect(background); drawContent() }
    return remember(layer) { PageBackdrop(layer) }
}

@Composable
internal fun BlurredBar(
    backdrop: PageBackdrop?,
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = .82f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        if (backdrop != null && Build.VERSION.SDK_INT >= 33) {
            val layer = rememberGraphicsLayer()
            val density = LocalDensity.current
            val radius = with(density) { 32.dp.toPx() }
            val effect = remember(radius) {
                RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()
            }
            val surface = MiuixTheme.colorScheme.surface
            val tint = surface.copy(alpha = surfaceAlpha.coerceIn(0f, 1f))
            var position by remember { mutableStateOf(Offset.Zero) }
            Box(Modifier.matchParentSize().onGloballyPositioned { position = it.positionInWindow() }
                .drawWithContent {
                    val margin = (radius * 3).toInt()
                    val offset = backdrop.position - position
                    // Capture only the bar and its blur margin; foreground controls stay sharp.
                    layer.record(IntSize(size.width.toInt() + margin * 2, size.height.toInt() + margin * 2)) {
                        drawRect(surface)
                        translate(margin + offset.x, margin + offset.y) { drawLayer(backdrop.layer.graphicsLayer) }
                    }
                    layer.renderEffect = effect
                    clipRect {
                        drawRect(surface)
                        translate(-margin.toFloat(), -margin.toFloat()) { drawLayer(layer) }
                        drawRect(tint)
                    }
                })
        }
        CompositionLocalProvider(LocalBlurredBar provides (backdrop != null)) { content() }
    }
}

@Composable
internal fun barBackground(color: Color): Color = if (LocalBlurredBar.current) Color.Transparent else color

/** Translucent native activities do not share the Compose popup host. */
internal fun Window.setMiuixBlurBehind(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < 31) return
    val radius = if (enabled) (24 * context.resources.displayMetrics.density).toInt() else 0
    val current = attributes
    val active = current.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND != 0
    if (active == enabled && current.blurBehindRadius == radius) return
    attributes = current.apply {
        flags = if (enabled) flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            else flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        blurBehindRadius = radius
    }
}

@Composable
internal fun Modifier.pageScroll(state: ScrollState, padding: PaddingValues): Modifier =
    if (isMiuixBlurEnabled()) verticalScroll(state).padding(padding)
    else padding(padding).verticalScroll(state)
