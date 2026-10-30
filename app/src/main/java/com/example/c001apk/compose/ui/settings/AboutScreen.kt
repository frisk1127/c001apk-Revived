package com.example.c001apk.compose.ui.settings

import com.example.c001apk.compose.ui.component.design.pageScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ForkRight
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.c001apk.compose.ui.component.design.HorizontalDivider
import com.example.c001apk.compose.ui.component.design.Scaffold
import com.example.c001apk.compose.ui.component.design.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.R
import com.example.c001apk.compose.constant.Constants.URL_SOURCE_CODE
import com.example.c001apk.compose.constant.Constants.URL_SOURCE_CODE_FORK
import com.example.c001apk.compose.ui.component.BackButton
import com.example.c001apk.compose.ui.component.LauncherIconImage
import com.example.c001apk.compose.ui.component.settings.BasicListItem
import com.example.c001apk.compose.util.openInBrowser
import com.example.c001apk.compose.ui.update.AboutClientUpdates

/**
 * Created by bggRGjQaUbCoE on 2024/6/2
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onLicenseClick: () -> Unit,
) {

    val context = LocalContext.current
    val rememberScrollState = rememberScrollState()
    if (com.example.c001apk.compose.ui.theme.isMiuix()) {
        MiuixAboutScreen(onBackClick, onLicenseClick, rememberScrollState)
        return
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton { onBackClick() }
                },
                title = "关于",
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .pageScroll(rememberScrollState, paddingValues)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                LauncherIconImage(Modifier.align(Alignment.Center))
            }
            HorizontalDivider()
            BasicListItem(
                leadingImageVector = Icons.Outlined.AllInclusive,
                headlineText = stringResource(id = R.string.app_name),
                supportingText = "持续维护的酷安第三方客户端"
            ) { }
            BasicListItem(
                leadingImageVector = Icons.Default.ErrorOutline,
                headlineText = "版本",
                supportingText = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
            ) { }
            BasicListItem(
                leadingImageVector = Icons.Outlined.Code,
                headlineText = "原项目地址",
                supportingText = URL_SOURCE_CODE
            ) {
                context.openInBrowser(URL_SOURCE_CODE)
            }
            AboutClientUpdates()
            BasicListItem(
                leadingImageVector = Icons.Outlined.ForkRight,
                headlineText = "本项目地址",
                supportingText = URL_SOURCE_CODE_FORK
            ) {
                context.openInBrowser(URL_SOURCE_CODE_FORK)
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.Source,
                headlineText = "开源许可证"
            ) {
                onLicenseClick()
            }
            BasicListItem(
                leadingImageVector = Icons.Outlined.Feedback,
                headlineText = "反馈",
                supportingText = "报告问题或提出建议",
            ) {
                context.openInBrowser("$URL_SOURCE_CODE_FORK/issues")
            }

        }
    }

}
