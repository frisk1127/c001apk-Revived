package com.example.c001apk.compose.ui.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.URL_SOURCE_CODE
import com.example.c001apk.compose.constant.Constants.URL_SOURCE_CODE_FORK
import com.example.c001apk.compose.ui.component.LauncherIconImage
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.design.isMiuixWindowBlurEnabled
import com.example.c001apk.compose.util.openInBrowser
import com.example.c001apk.compose.ui.update.AboutClientUpdates
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur

@Composable
internal fun MiuixAboutScreen(onBack: () -> Unit, onLicense: () -> Unit, scroll: ScrollState) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize().testTag("miuix-about-page")) {
        val heroHeight = (maxHeight * .46f).coerceIn(if (maxWidth > maxHeight) 260.dp else 300.dp, 480.dp)
        val heroPixels = with(LocalDensity.current) { heroHeight.toPx() }
        val collapsed by remember(scroll, heroPixels) { derivedStateOf { scroll.value > heroPixels * .65f } }
        val dark = LocalUserPreferences.current.isDarkMode()
        val backdrop = if (isMiuixWindowBlurEnabled()) rememberLayerBackdrop() else null
        // Capture only the background; the glass card must remain outside its source layer.
        AboutAnimatedBackground(Modifier.matchParentSize().then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)) {
            (1f - scroll.value / heroPixels).coerceIn(0f, 1f)
        }
        val glassColors = BlurDefaults.blurColors(blendColors = if (dark) listOf(
            BlendColorEntry(Color(0x4DA9A9A9), BlurBlendMode.Luminosity),
            BlendColorEntry(Color(0x1A9C9C9C), BlurBlendMode.PlusDarker),
        ) else listOf(
            BlendColorEntry(Color(0x340034F9), BlurBlendMode.Overlay),
            BlendColorEntry(Color(0xB3FFFFFF), BlurBlendMode.HardLight),
        ))
        Scaffold(containerColor = Color.Transparent,
            topBar = { SmallTopAppBar(title = if (collapsed) "关于" else "", color = Color.Transparent,
                navigationIcon = { BackButton(onBack) }) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp)) {
                    AboutHeader(compact = false, modifier = Modifier.heightIn(min = heroHeight))
                    SmallTitle("关于", textColor = MiuixTheme.colorScheme.primary,
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp))
                    Card(Modifier.fillMaxWidth().testTag("about-links").then(if (backdrop != null)
                        Modifier.textureBlur(backdrop = backdrop, shape = RoundedCornerShape(CardDefaults.CornerRadius),
                            blurRadius = 60f, colors = glassColors) else Modifier),
                        colors = CardDefaults.defaultColors(Color.White.copy(alpha = if (dark) .18f else .65f))) {
                        AboutClientUpdates()
                        ProjectLinks()
                        ArrowPreference(title = "开源许可证", onClick = onLicense)
                        ArrowPreference(title = "反馈", summary = "报告问题或提出建议",
                            onClick = { context.openInBrowser("$URL_SOURCE_CODE_FORK/issues") })
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
internal fun MiuixAboutDialog(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    com.example.c001apk.compose.ui.component.design.GlobalPopupBlur(visible)
    WindowDialog(show = visible, modifier = Modifier.testTag("miuix-about-dialog"),
        onDismissRequest = { visible = false }, onDismissFinished = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AboutHeader(compact = true)
                Column { ProjectLinks() }
            }
            TextButton(text = "关闭", onClick = { visible = false },
                modifier = Modifier.fillMaxWidth().testTag("about-dialog-close"))
        }
    }
}

@Composable
private fun AboutHeader(compact: Boolean, modifier: Modifier = Modifier) {
    val name = stringResource(R.string.app_name)
    val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    if (compact) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            LauncherIconImage(Modifier.size(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MiuixTheme.textStyles.title4, color = MiuixTheme.colorScheme.onSurface)
                Text(version, style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    } else {
        Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
            LauncherIconImage(Modifier.size(96.dp))
            Text(name, style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text("持续维护的酷安第三方客户端", style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
            Text(version, style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProjectLinks() {
    val context = LocalContext.current
    ArrowPreference(title = "本项目地址", summary = URL_SOURCE_CODE_FORK.removePrefix("https://github.com/"),
        onClick = { context.openInBrowser(URL_SOURCE_CODE_FORK) })
    ArrowPreference(title = "原项目地址", summary = URL_SOURCE_CODE.removePrefix("https://github.com/"),
        onClick = { context.openInBrowser(URL_SOURCE_CODE) })
}
