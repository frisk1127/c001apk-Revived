package com.example.c001apk.compose.ui.settings

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.example.c001apk.compose.UiStyle
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.component.cards.FeedReplySortCard
import com.example.c001apk.compose.ui.component.design.*
import com.example.c001apk.compose.ui.component.settings.PreferenceTopBar
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import java.io.File

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@SdkSuppress(minSdkVersion = 33)
class MiuixScrollRefreshTest {
    @get:Rule val compose = createComposeRule()

    @Composable private fun Theme(blur: Boolean = true, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalUserPreferences provides UserPreferencesCompat.default().copy(
            uiStyle = UiStyle.MIUIX, blurEnabled = blur,
        )) {
            C001apkComposeTheme(uiStyle = UiStyle.MIUIX, darkTheme = true, materialYou = false, content = content)
        }
    }

    @Test fun commentSortRefreshIsBelowPinnedHeader() = commentRefresh(true)
    @Test fun commentSortRefreshWithoutBlurKeepsItsPosition() = commentRefresh(false)
    @Test fun commentSortRefreshIsBelowUnpinnedHeader() = commentRefresh(true, pinned = false)

    private fun commentRefresh(blur: Boolean, pinned: Boolean = true) {
        var refreshing by mutableStateOf(false)
        var selected by mutableIntStateOf(0)
        lateinit var list: LazyListState
        compose.setContent {
            Theme(blur) {
                list = rememberLazyListState()
                Scaffold(topBar = { TopAppBar(title = "动态") }) { padding ->
                    RefreshContainer(false, {},
                        modifier = Modifier.padding(top = if (blur) 0.dp else padding.calculateTopPadding()),
                        indicatorTopInset = if (blur) padding.calculateTopPadding() else 0.dp) {
                        LazyColumn(Modifier.fillMaxSize().testTag("comments"), state = list,
                            contentPadding = PaddingValues(top = if (blur) padding.calculateTopPadding() else 0.dp)) {
                            item { Text("动态正文", Modifier.fillMaxWidth().height(420.dp)) }
                            stickyHeader(key = "sort") {
                                FeedReplySortCard(replyCount = "30", selected = selected,
                                    pinnedTopInset = if (blur) padding.calculateTopPadding() else null,
                                    isRefreshing = refreshing,
                                    updateSortReply = { selected = it; refreshing = true })
                            }
                            items(30) { Text("评论 $it", Modifier.fillMaxWidth().height(72.dp).padding(16.dp)) }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        if (pinned) compose.onNodeWithTag("comments").performScrollToIndex(5)
        compose.waitForIdle()
        val initial = compose.onNodeWithTag("miuix-comment-sort").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("热门").performClick()
        compose.waitForIdle()
        val indicator = compose.onNodeWithTag("comment-sort-refresh").fetchSemanticsNode().boundsInRoot
        val sort = compose.onNodeWithTag("miuix-comment-sort").fetchSemanticsNode().boundsInRoot
        capture("comment-sort-below-${if (blur) "blur" else "solid"}-${if (pinned) "pinned" else "inline"}")
        assertTrue("Comment refresh must be below the sort controls: $indicator / $sort", indicator.top >= sort.bottom - 1f)
        assertEquals("Sort controls must not move during refresh", initial.top, sort.top, 1f)
        assertEquals("Sort controls must retain their height", initial.bottom, sort.bottom, 1f)
        compose.onAllNodesWithText("正在刷新").assertCountEquals(1)
        compose.onNodeWithText("正在刷新").assertIsDisplayed()
        compose.onNodeWithText("正在刷新评论").assertDoesNotExist()
        compose.onNodeWithText("最新").performClick()
        assertEquals(2, selected)
        compose.runOnIdle { refreshing = false }
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("comment-sort-refresh").fetchSemanticsNode().boundsInRoot.height <= 1f
        }
        compose.onNodeWithText("正在刷新").assertDoesNotExist()
        compose.onNodeWithText("刷新完成").assertDoesNotExist()
        assertEquals(initial.top, compose.onNodeWithTag("miuix-comment-sort").fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.onNodeWithText("最新").performClick()
        compose.waitForIdle()
        val second = compose.onNodeWithTag("comment-sort-refresh").fetchSemanticsNode().boundsInRoot
        assertTrue(second.top >= compose.onNodeWithTag("miuix-comment-sort").fetchSemanticsNode().boundsInRoot.bottom - 1f)
        assertEquals(1, selected)
    }

    @Test fun cloudTitleExpandsBeforePullToRefresh() {
        lateinit var scroll: ScrollBehavior
        var progress by mutableFloatStateOf(0f)
        var refreshing by mutableStateOf(false)
        var requests = 0
        compose.setContent {
            Theme {
                scroll = MiuixScrollBehavior(snapAnimationSpec = null, flingAnimationSpec = null)
                Scaffold(topBar = {
                    FavoriteSearchHeader(true, "", {}, false, {}, "搜索收藏正文") {
                        PreferenceTopBar("云端收藏", scroll)
                    }
                }) { padding ->
                    RefreshContainer(refreshing, { requests++; refreshing = true },
                        indicatorTopInset = padding.calculateTopPadding(), onPullProgress = { progress = it },
                        topAppBarScrollBehavior = scroll) {
                        LazyColumn(Modifier.fillMaxSize().testTag("folders"), contentPadding = padding) {
                            items(30) { Text("收藏夹 $it", Modifier.fillMaxWidth().height(72.dp).padding(16.dp)) }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        var initial = 0f
        compose.runOnIdle { scroll.state.heightOffset = scroll.state.heightOffsetLimit; initial = scroll.state.heightOffset }
        compose.waitForIdle()
        assertTrue("Expected a collapsible portrait header", initial < -1f)
        compose.onNodeWithTag("folders").performTouchInput {
            down(Offset(centerX, height * .45f))
            moveBy(Offset(0f, -initial * .6f), delayMillis = 500)
        }
        compose.waitForIdle()
        val expandedOffset = scroll.state.heightOffset
        val pullProgress = progress
        capture("cloud-title-during-pull")
        compose.onNodeWithTag("folders").performTouchInput { up() }
        assertTrue("The title should expand before refreshing: $initial -> $expandedOffset", expandedOffset > initial + 1f)
        assertEquals("Refresh started while the title was still collapsed", 0f, pullProgress, .001f)
        assertEquals(0, requests)
        compose.runOnIdle { scroll.state.heightOffset = 0f }
        compose.onNodeWithTag("folders").performTouchInput {
            swipe(Offset(centerX, height * .35f), Offset(centerX, height * .93f), durationMillis = 700)
        }
        compose.waitUntil(5_000) { requests > 0 }
        assertEquals(1, requests)
        assertEquals(0f, scroll.state.heightOffset, 1f)
        capture("cloud-refresh-expanded")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null), "scroll-refresh-qa")
        directory.mkdirs()
        compose.onRoot().captureToImage().asAndroidBitmap().let {
            File(directory, "$name.png").outputStream().use { output -> it.compress(Bitmap.CompressFormat.PNG, 100, output) }
        }
    }
}
