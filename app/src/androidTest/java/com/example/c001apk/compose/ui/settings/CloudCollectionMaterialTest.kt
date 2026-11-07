package com.example.c001apk.compose.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.view.inspector.WindowInspector
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import com.example.c001apk.compose.UiStyle
import com.example.c001apk.compose.logic.model.CloudCollection
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.repository.*
import com.example.c001apk.compose.ui.collection.*
import com.example.c001apk.compose.ui.component.design.*
import com.example.c001apk.compose.ui.component.cards.CollectionCard
import com.example.c001apk.compose.ui.feed.FeedExportButton
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

@SdkSuppress(minSdkVersion = 29)
class CloudCollectionMaterialTest {
    @get:Rule val compose = createComposeRule()

    @Test fun longListKeepsActionsVisibleWithLargeText() {
        var state by mutableStateOf(FeedCollectionState(loaded = true,
            collections = (1..30).map { CloudCollection("$it", null, "collection", "收藏夹 $it", 0) }))
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MD3, darkTheme = true, materialYou = false, fontScale = 1.3f) {
                FeedCollectionSheet(true, state, {}, {}, { state = state.copy(selectedIds = setOf(it)) }, {}, { _, _ -> })
            }
        }
        compose.onNodeWithText("保存").assertIsNotEnabled()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("收藏夹 30"))
        compose.onNodeWithText("收藏夹 30").performClick()
        compose.onNodeWithText("保存").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("新建收藏夹").assertIsDisplayed()
        assertEquals(setOf("30"), state.selectedIds)
        capture("md3-long-list")
    }

    @Test fun createWithKeyboardKeepsFieldAndConfirmationVisible() {
        var created = ""
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MD3, darkTheme = false, materialYou = false) {
                FeedCollectionSheet(true, FeedCollectionState(loaded = true), {}, {}, {}, {}, { title, _ -> created = title })
            }
        }
        compose.onNodeWithText("新建收藏夹").performClick()
        val input = compose.onNode(hasSetTextAction())
        input.performTouchInput { click() }
        compose.waitUntil(5_000) {
            var visible = false
            compose.runOnIdle {
                visible = WindowInspector.getGlobalWindowViews().any {
                    ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.ime()) == true
                }
            }
            visible
        }
        input.performTextInput("技术资料")
        compose.waitForIdle()
        android.os.SystemClock.sleep(500)
        compose.waitForIdle()
        val complete = input.getUnclippedBoundsInRoot().let { (it.bottom - it.top).value }
        val visible = with(compose.density) { input.fetchSemanticsNode().boundsInRoot.height.toDp().value }
        assertEquals("Name field is clipped by the keyboard", complete, visible, .5f)
        compose.onNodeWithText("新建并收藏").assertIsDisplayed().assertIsEnabled()
        capture("md3-keyboard")
        compose.onNodeWithText("新建并收藏").performClick()
        assertEquals("技术资料", created)
    }

    @Test fun deleteDialogKeepsErrorAndDisablesRepeatedSubmission() {
        var deleting by mutableStateOf(false)
        var error by mutableStateOf<String?>("网络错误，请重试")
        var confirms = 0
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MD3, darkTheme = false, materialYou = false) {
                DeleteCollectionDialog("技术资料", deleting, error, {}, { confirms++; deleting = true })
            }
        }
        compose.onNodeWithText("网络错误，请重试").assertIsDisplayed()
        capture("md3-delete-error")
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("正在删除…").assertIsNotEnabled()
        compose.onNodeWithText("取消").assertIsNotEnabled()
        assertEquals(1, confirms)
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Test fun searchShowsBodyExcerptAndReturnsToFolders() {
        var query by mutableStateOf("关键字")
        var expanded by mutableStateOf(true)
        var opened = ""
        val state = CloudCollectionSearchState(started = true, entries = listOf(
            CloudCollectionSearchEntry(CloudCollectionDocument("1", "资料整理", "作者", "前文".repeat(200) + "关键字在正文末尾"), listOf("稍后阅读")),
            CloudCollectionSearchEntry(CloudCollectionDocument("2", "不应出现的结果", "关键字作者", "普通正文"), listOf("关键字收藏夹")),
        ))
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MD3, darkTheme = true, materialYou = false) {
                Scaffold(topBar = { FavoriteSearchHeader(true, query, { query = it }, expanded, { expanded = it }, "搜索收藏正文") {
                    TopAppBar(title = "云端收藏")
                } }) { padding ->
                    if (query.isBlank()) Text("收藏夹列表", Modifier.padding(padding))
                    else CloudCollectionSearchResults(query, state, padding, {}, { id, _ -> opened = id })
                }
            }
        }
        compose.onNodeWithText("找到 1 条收藏动态").assertIsDisplayed()
        compose.onNodeWithText("不应出现的结果").assertDoesNotExist()
        compose.onNodeWithText("关键字在正文末尾", substring = true).assertIsDisplayed()
        capture("md3-body-search")
        compose.onNodeWithText("资料整理").performClick()
        assertEquals("1", opened)
        compose.onNodeWithContentDescription("退出搜索").performClick()
        compose.onNodeWithText("收藏夹列表").assertIsDisplayed()
        assertTrue(query.isEmpty())
    }

    @Test fun folderCardAndExportMenuUseMaterialComponents() {
        val feed = Gson().fromJson("""{"id":"123","title":"阅读笔记与常用技术资料","is_open_title":"私密","item_num":"24"}""", HomeFeedResponse.Data::class.java)
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MD3, darkTheme = false, materialYou = false) {
                Column(Modifier.padding(16.dp).statusBarsPadding()) {
                    FeedExportButton(feed)
                    CollectionCard(data = feed)
                }
            }
        }
        compose.onNodeWithText("私密 · 24 项内容").assertIsDisplayed()
        compose.onNodeWithContentDescription("导出为…").performClick()
        compose.onNodeWithText("导出为 Markdown").assertIsDisplayed()
        compose.onNodeWithText("导出为 HTML").assertIsDisplayed()
        capture("md3-folder-export")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(350)
        val directory = File(ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null), "cloud-material-qa")
        directory.mkdirs()
        val suffix = InstrumentationRegistry.getArguments().getString("orientation") ?: "portrait"
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        image?.let { bitmap ->
            File(directory, "$name-$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
