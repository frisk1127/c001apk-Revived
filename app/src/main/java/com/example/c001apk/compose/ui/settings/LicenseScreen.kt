package com.example.c001apk.compose.ui.settings

import com.example.c001apk.compose.ui.component.design.pageScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.c001apk.compose.ui.component.design.Scaffold
import com.example.c001apk.compose.ui.component.design.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.settings.BasicListItem
import com.example.c001apk.compose.util.openInBrowser

/**
 * Created by bggRGjQaUbCoE on 2024/6/2
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    onBackClick: () -> Unit
) {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onBackClick)
                },
                title = "开源许可证",
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxSize().pageScroll(androidx.compose.foundation.rememberScrollState(), paddingValues)
        ) {
            com.example.c001apk.compose.ui.component.settings.PreferenceGroup("使用的开源组件", showTitleInMd3 = false) {
                licenseList.forEach { LicenseRow(it) }
            }
        }
    }

}

private val licenseList = listOf(
    LicenseItem("Dimitry Ivanov", "Markwon", "https://github.com/noties/Markwon", LicenseType.Apache2),
    LicenseItem("Google", "AndroidX SplashScreen", "https://developer.android.com/develop/ui/views/launch/splash-screen", LicenseType.Apache2),
    LicenseItem("kongwufang", "SZLM protocol reference", "https://github.com/kongwufang/szlm_res", LicenseType.MIT),
    LicenseItem(
        "compose-miuix-ui contributors",
        "Miuix",
        "https://github.com/compose-miuix-ui/miuix",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Jetpack Compose",
        "https://github.com/androidx/androidx",
        LicenseType.Apache2
    ),
    LicenseItem(
        "JetBrains",
        "Kotlin",
        "https://github.com/JetBrains/kotlin",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Accompanist",
        "https://github.com/google/accompanist",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Material Design 3",
        "https://m3.material.io/",
        LicenseType.Apache2
    ),
    LicenseItem(
        "Google",
        "Material Icons",
        "https://github.com/google/material-design-icons",
        LicenseType.Apache2
    ),
    LicenseItem(
        "square",
        "okhttp",
        "https://github.com/square/okhttp",
        LicenseType.Apache2
    ),
    LicenseItem(
        "square",
        "retrofit",
        "https://github.com/square/retrofit",
        LicenseType.Apache2
    ),
    LicenseItem(
        "mikaelzero",
        "mojito",
        "https://github.com/mikaelzero/mojito",
        LicenseType.Apache2
    ),
    LicenseItem(
        "plain-dev",
        "NineGridImageView",
        "https://github.com/plain-dev/NineGridImageView",
        LicenseType.MIT
    ),
    LicenseItem(
        "coil-kt",
        "coil",
        "https://github.com/coil-kt/coil",
        LicenseType.Apache2
    ),
    LicenseItem(
        "wasabeef",
        "transformers",
        "https://github.com/wasabeef/transformers",
        LicenseType.Apache2
    ),
    LicenseItem(
        "jeremyh",
        "jBCrypt",
        "https://github.com/jeremyh/jBCrypt",
        LicenseType.Apache2
    ),
    LicenseItem(
        "jhy",
        "jsoup",
        "https://github.com/jhy/jsoup",
        LicenseType.MIT
    ),
    LicenseItem(
        "zhanghai",
        "AppIconLoader",
        "https://github.com/zhanghai/AppIconLoader",
        LicenseType.MIT
    ),
    LicenseItem(
        "onebone",
        "compose-collapsing-toolbar",
        "https://github.com/onebone/compose-collapsing-toolbar",
        LicenseType.MIT
    ),
    LicenseItem(
        "AlexMofer",
        "SmoothInputLayout",
        "https://github.com/AlexMofer/SmoothInputLayout",
        LicenseType.Apache2
    ),
    LicenseItem(
        "jordond",
        "MaterialKolor",
        "https://github.com/jordond/MaterialKolor",
        LicenseType.MIT
    ),
)

data class LicenseItem(
    val author: String,
    val name: String,
    val url: String,
    val type: LicenseType
)

enum class LicenseType {
    Apache2,
    MIT,
    GPL3
}

private fun getLicense(type: LicenseType): String =
    when (type) {
        LicenseType.Apache2 -> "Apache Software License 2.0"
        LicenseType.MIT -> "MIT License"
        LicenseType.GPL3 -> "GNU general public license Version 3"
    }

@Composable
fun LicenseRow(item: LicenseItem) {
    val context = LocalContext.current
    if (com.example.c001apk.compose.ui.theme.isMiuix()) {
        top.yukonga.miuix.kmp.preference.ArrowPreference(
            title = item.name, summary = "${item.author} · ${getLicense(item.type)}",
            onClick = { context.openInBrowser(item.url) },
        )
    } else BasicListItem(
        headlineText = "${item.name} - ${item.author}",
        supportingText = "${item.url}\n${getLicense(item.type)}",
        onClick = { context.openInBrowser(item.url) },
    )
}
