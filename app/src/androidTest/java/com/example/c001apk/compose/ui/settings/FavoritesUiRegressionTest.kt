package com.example.c001apk.compose.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.view.inspector.WindowInspector
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import com.example.c001apk.compose.UiStyle
import com.example.c001apk.compose.logic.model.CloudCollection
import com.example.c001apk.compose.ui.collection.FeedCollectionSheet
import com.example.c001apk.compose.ui.collection.FeedCollectionState
import com.example.c001apk.compose.ui.collection.DeleteCollectionDialog
import com.example.c001apk.compose.ui.component.cards.CollectionCard
import com.example.c001apk.compose.logic.datastore.UserPreferencesCompat
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.google.gson.Gson
import com.example.c001apk.compose.ui.component.design.*
import com.example.c001apk.compose.ui.theme.C001apkComposeTheme
import com.example.c001apk.compose.ui.component.settings.PreferenceTopBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@SdkSuppress(minSdkVersion = 29)
class FavoritesUiRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun favoriteSearchKeepsSpacing() = searchSpacing(1f, true)
    @Test fun favoriteSearchKeepsSpacingWithLargeText() = searchSpacing(1.3f, false)
    @Test fun cloudSearchKeepsSpacing() = searchSpacing(1f, true, cloud = true)

    private fun searchSpacing(fontScale: Float, dark: Boolean, cloud: Boolean = false) {
        var query by mutableStateOf("")
        var expanded by mutableStateOf(false)
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MIUIX, darkTheme = dark, materialYou = false, fontScale = fontScale) {
                Scaffold(topBar = {
                    FavoriteSearchHeader(true, query, { query = it }, expanded, { expanded = it }, "搜索本地收藏") {
                        if (cloud) PreferenceTopBar(title = "云端收藏", scrollBehavior = MiuixScrollBehavior())
                        else TopAppBar(title = "本地收藏")
                    }
                }) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize().testTag("results")) { Text("收藏内容") }
                }
            }
        }
        compose.waitForIdle()
        fun top() = compose.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot.top
        val initialTop = top()
        val name = if (cloud) "cloud-search" else "local-search"
        capture("$name-${fontScale}-collapsed")
        compose.mainClock.autoAdvance = false
        compose.onNode(hasSetTextAction()).performClick()
        val opening = mutableListOf<Float>()
        repeat(25) { compose.mainClock.advanceTimeBy(16); compose.waitForIdle(); opening += top() }
        capture("$name-${fontScale}-expanded")
        compose.onNodeWithText("取消").performClick()
        val closing = mutableListOf<Float>()
        repeat(25) { compose.mainClock.advanceTimeBy(16); compose.waitForIdle(); closing += top() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertFalse(expanded)
        assertTrue("Opening changed content top: initial=$initialTop frames=$opening", opening.all { kotlin.math.abs(it - initialTop) <= 1f })
        assertTrue("Closing changed content top: initial=$initialTop frames=$closing", closing.all { kotlin.math.abs(it - initialTop) <= 1f })
    }

    @Test fun topicSearchHasOneSearchIconAndSubmits() {
        var query by mutableStateOf("")
        var submitted = ""
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MIUIX, darkTheme = true, materialYou = false) {
                SearchInputBar(query, { query = it }, { submitted = it }, "搜索话题", {}, remember { FocusRequester() })
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("手机")
        capture("topic-search")
        compose.onNode(hasSetTextAction()).performImeAction()
        assertEquals("手机", submitted)
        compose.onAllNodes(hasContentDescription("搜索") or hasContentDescription("Search"), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test fun collectionSheetsDark() = collectionSheets(true, 1f)
    @Test fun collectionSheetsLightLargeText() = collectionSheets(false, 1.3f)

    private fun collectionSheets(dark: Boolean, fontScale: Float) {
        var state by mutableStateOf(FeedCollectionState(
            collections = listOf(CloudCollection("1", "collection1", "collection", "常用收藏夹", 0),
                CloudCollection("2", "collection2", "collection", "默认收藏单", 0)), loaded = true,
        ))
        var show by mutableStateOf(true)
        var saved = 0
        var created: Pair<String, Boolean>? = null
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MIUIX, darkTheme = dark, materialYou = false, fontScale = fontScale) {
                Text("动态详情")
                FeedCollectionSheet(show, state, { show = false }, {}, { id ->
                    state = state.copy(selectedIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id)
                }, { saved++ }, { title, public -> created = title to public })
            }
        }
        compose.waitForIdle()
        val tag = if (dark) "dark" else "light-large"
        capture("sheet-$tag-list", dialog = true)
        compose.onNodeWithContentDescription("确认收藏").assertIsNotEnabled()
        compose.onNodeWithText("常用收藏夹").performClick()
        compose.onNodeWithContentDescription("确认收藏").performClick()
        assertEquals(1, saved)
        compose.onNodeWithText("新建收藏夹").performClick()
        capture("sheet-$tag-create", dialog = true)
        compose.onNode(hasSetTextAction()).performClick().performTextInput("稍后阅读")
        compose.waitForIdle()
        compose.waitUntil(5_000) {
            var visible = false
            compose.runOnIdle {
                visible = WindowInspector.getGlobalWindowViews().any {
                    ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.ime()) == true
                }
            }
            visible
        }
        android.os.SystemClock.sleep(500)
        capture("sheet-$tag-keyboard", dialog = true)
        val input = compose.onNode(hasSetTextAction())
        val completeHeight = input.getUnclippedBoundsInRoot().let { (it.bottom - it.top).value }
        val visibleHeight = with(compose.density) { input.fetchSemanticsNode().boundsInRoot.height.toDp().value }
        assertEquals("Keyboard must not clip the name field", completeHeight, visibleHeight, .5f)
        compose.onNodeWithContentDescription("新建并收藏").performClick()
        assertEquals("稍后阅读" to false, created)
        compose.runOnIdle { state = state.copy(error = "保存失败，请重试") }
        compose.onNode(hasSetTextAction()).assertTextContains("稍后阅读")
        compose.onNodeWithText("保存失败，请重试").assertIsDisplayed()
        compose.onNodeWithContentDescription("取消新建").performClick()
        compose.onNodeWithText("常用收藏夹").assertIsDisplayed()
        assertEquals(setOf("1"), state.selectedIds)
        compose.onNodeWithContentDescription("关闭").performClick()
        compose.waitUntil { !show }
    }

    @Test fun longCollectionListScrollsWithoutHidingActions() {
        var navigationBottom = 0
        var state by mutableStateOf(FeedCollectionState(
            collections = (1..30).map { CloudCollection("$it", "collection$it", "collection", "收藏夹 $it", 0) },
            loaded = true,
        ))
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MIUIX, darkTheme = true, materialYou = false) {
                val inset = WindowInsets.navigationBars.getBottom(LocalDensity.current)
                SideEffect { navigationBottom = inset }
                FeedCollectionSheet(true, state, {}, {}, { state = state.copy(selectedIds = setOf(it)) }, {}, { _, _ -> })
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("收藏夹 30"))
        compose.onNodeWithText("收藏夹 30").performClick()
        compose.onNodeWithContentDescription("确认收藏").assertIsDisplayed().assertIsEnabled()
        assertEquals(setOf("30"), state.selectedIds)
        val last = compose.onNodeWithText("收藏夹 30").fetchSemanticsNode().boundsInRoot
        val dialog = compose.onNode(isDialog()).fetchSemanticsNode().boundsInRoot
        assertTrue("Last row overlaps navigation bar: $last / $dialog / $navigationBottom",
            last.bottom <= dialog.bottom - navigationBottom)
        capture("sheet-long-list", dialog = true)
    }

    @Test fun materialCollectionFlowStillWorks() {
        var state by mutableStateOf(FeedCollectionState(
            collections = listOf(CloudCollection("1", "collection1", "collection", "默认收藏单", 0)), loaded = true,
        ))
        var saved = false
        var created = ""
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MD3, darkTheme = false, materialYou = false) {
                FeedCollectionSheet(true, state, {}, {}, { state = state.copy(selectedIds = setOf(it)) },
                    { saved = true }, { title, _ -> created = title })
            }
        }
        compose.onNodeWithText("默认收藏单").performClick()
        compose.onNodeWithText("保存").performClick()
        assertTrue(saved)
        compose.onNodeWithText("新建收藏夹").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("资料")
        capture("sheet-md3-form", dialog = true)
        compose.onNodeWithText("新建并收藏").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("资料", created) }
    }

    @Test fun collectionSheetPressedCornersAreClipped() {
        compose.setContent {
            C001apkComposeTheme(uiStyle = UiStyle.MIUIX, darkTheme = true, materialYou = false) {
                FeedCollectionSheet(true, FeedCollectionState(
                    collections = listOf(CloudCollection("1", null, "collection", "按住检查圆角", 0)), loaded = true,
                ), {}, {}, {}, {}, { _, _ -> })
            }
        }
        val row = compose.onNodeWithText("按住检查圆角")
        val before = row.captureToImage().asAndroidBitmap()
        row.performTouchInput { down(center); advanceEventTime(600) }
        compose.waitForIdle()
        val pressed = row.captureToImage().asAndroidBitmap()
        assertEquals("Pressed highlight must keep its rounded corner", before.getPixel(2, 2), pressed.getPixel(2, 2))
        assertNotEquals("Held row must still show press feedback", before.getPixel(before.width / 2, 8),
            pressed.getPixel(pressed.width / 2, 8))
        capture("sheet-held-row", dialog = true)
        row.performTouchInput { up() }
    }

    @Test fun ownCollectionLongPressRequiresConfirmationAndKeepsFailure() {
        val collection = Gson().fromJson("""{"id":"123","uid":"owner","title":"稍后阅读","is_open_title":"私密","item_num":"2","follow_num":"0"}""",
            HomeFeedResponse.Data::class.java)
        var selected by mutableStateOf(false)
        var deleting by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        var confirmed = 0
        var opened = 0
        compose.setContent {
            CompositionLocalProvider(LocalUserPreferences provides UserPreferencesCompat.default().copy(isLogin = true, uid = "owner")) {
                C001apkComposeTheme(uiStyle = UiStyle.MIUIX, darkTheme = true, materialYou = false) {
                    CollectionCard(data = collection, onViewFFFList = { _, _, _, _ -> opened++ },
                        onDeleteCollection = { selected = true })
                    if (selected) DeleteCollectionDialog(collection.title.orEmpty(), deleting, error, { selected = false },
                        { confirmed++; deleting = true })
                }
            }
        }
        compose.onNodeWithText("稍后阅读").performTouchInput { longClick() }
        compose.onNodeWithText("删除收藏夹").assertIsDisplayed()
        assertEquals(0, opened)
        assertEquals(0, confirmed)
        compose.onNodeWithText("取消").performClick()
        compose.waitForIdle()
        compose.waitUntil(5_000) { !selected }
        compose.onNodeWithText("稍后阅读").performTouchInput { longClick() }
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithText("正在删除…").assertIsNotEnabled()
        compose.onNodeWithText("取消").assertIsNotEnabled()
        compose.runOnIdle { deleting = false; error = "网络错误，请重试" }
        compose.onNodeWithText("网络错误，请重试").assertIsDisplayed()
        compose.onNodeWithText("删除").assertIsEnabled()
        capture("delete-collection-error", dialog = true)
        compose.onNodeWithText("删除").performClick()
        assertEquals(2, confirmed)
    }

    @Test fun otherPeoplesCollectionsHaveNoDeleteAction() {
        val collection = Gson().fromJson("""{"id":"123","uid":"other","title":"他人的收藏夹"}""", HomeFeedResponse.Data::class.java)
        compose.setContent {
            CompositionLocalProvider(LocalUserPreferences provides UserPreferencesCompat.default().copy(isLogin = true, uid = "owner")) {
                C001apkComposeTheme(uiStyle = UiStyle.MIUIX, darkTheme = false, materialYou = false) {
                    CollectionCard(data = collection, onDeleteCollection = { fail("Must not delete another user's collection") })
                }
            }
        }
        compose.onNodeWithText("他人的收藏夹").assert(SemanticsMatcher.keyNotDefined(androidx.compose.ui.semantics.SemanticsActions.OnLongClick))
    }

    private fun capture(name: String, dialog: Boolean = false) {
        val bitmap = (if (dialog) compose.onNode(isDialog()) else compose.onRoot()).captureToImage().asAndroidBitmap()
        if (InstrumentationRegistry.getArguments().getString("orientation") == "landscape") {
            assertTrue("Expected a landscape window, got ${bitmap.width} x ${bitmap.height}", bitmap.width > bitmap.height)
        }
        val directory = File(ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null), "favorites-qa")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (dialog) InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { screen ->
            File(directory, "$name-screen.png").outputStream().use { screen.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
