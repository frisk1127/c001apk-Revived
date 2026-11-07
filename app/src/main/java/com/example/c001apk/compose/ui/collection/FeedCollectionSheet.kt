package com.example.c001apk.compose.ui.collection

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import com.example.c001apk.compose.ui.component.design.*
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.theme.isMiuix
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCollectionSheet(
    show: Boolean,
    state: FeedCollectionState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onToggle: (String) -> Unit,
    onSave: () -> Unit,
    onCreate: (String, Boolean) -> Unit,
) {
    val miuix = isMiuix()
    val keyboard = LocalSoftwareKeyboardController.current
    val compactForm = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val performHaptic = rememberHapticClick {}
    var creating by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var isPublic by rememberSaveable { mutableStateOf(false) }
    val canSave = state.loaded && !state.saving && if (creating) title.isNotBlank()
        else state.selectedIds.isNotEmpty() || state.originalIds.isNotEmpty()
    val confirm = {
        if (canSave) {
            keyboard?.hide()
            if (creating) onCreate(title.trim(), isPublic) else onSave()
        }
    }
    LaunchedEffect(state.createdId) {
        if (state.createdId != null) {
            creating = false
            title = ""
        }
    }
    fun resetForm() {
        creating = false
        title = ""
        isPublic = false
    }
    val cancel = {
        keyboard?.hide()
        if (creating) resetForm() else onDismiss()
    }
    if (miuix) {
        GlobalPopupBlur(show)
        WindowBottomSheet(
            show = show, title = if (creating) "新建收藏夹" else "收藏到云端",
            allowDismiss = !state.saving, onDismissRequest = onDismiss,
            onDismissFinished = { resetForm() },
            startAction = {
                CollectionSheetAction(enabled = !state.saving, compact = compactForm, onClick = cancel) {
                    top.yukonga.miuix.kmp.basic.Icon(MiuixIcons.Close,
                        if (creating) "取消新建" else "关闭", tint = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp))
                }
            },
            endAction = {
                CollectionSheetAction(enabled = canSave, compact = compactForm, onClick = confirm) {
                    top.yukonga.miuix.kmp.basic.Icon(MiuixIcons.Ok,
                        if (creating) "新建并收藏" else "确认收藏",
                        modifier = Modifier.size(24.dp),
                        tint = if (canSave) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.disabledOnSecondaryVariant)
                }
            },
        ) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.captionBar).only(WindowInsetsSides.Bottom),
            )) {
                LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(bottom = 8.dp)) {
                    if (state.loading || state.saving) item {
                        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            top.yukonga.miuix.kmp.basic.CircularProgressIndicator(Modifier.size(24.dp))
                            top.yukonga.miuix.kmp.basic.Text(if (state.saving) "正在保存…" else "正在加载收藏夹")
                        }
                    }
                    state.error?.let { error -> item {
                        top.yukonga.miuix.kmp.basic.Text(error, color = MiuixTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 12.dp))
                    } }
                    if (!state.loading && !state.loaded) item {
                        BasicComponent(title = "重试", modifier = Modifier.collectionSheetRow(),
                            onClick = rememberHapticClick(onClick = onRetry))
                    }
                    if (state.loaded) {
                        if (creating) {
                            item {
                                top.yukonga.miuix.kmp.basic.TextField(
                                    value = title, onValueChange = { title = it }, label = "收藏夹名称",
                                    singleLine = true, enabled = !state.saving,
                                    useLabelAsPlaceholder = true,
                                    insideMargin = DpSize(TextFieldDefaults.InsideMargin.width, if (compactForm) 8.dp else 14.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                                )
                            }
                            item {
                                CompositionLocalProvider(LocalHapticFeedback provides NoNativeHaptics) {
                                    SwitchPreference(
                                        title = "公开收藏夹", summary = "关闭后仅自己可见",
                                        checked = isPublic, onCheckedChange = { performHaptic(); isPublic = it }, enabled = !state.saving,
                                        modifier = Modifier.collectionSheetRow(enabled = !state.saving),
                                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    )
                                }
                            }
                        } else {
                            if (state.collections.isEmpty()) item {
                                top.yukonga.miuix.kmp.basic.Text("暂无收藏夹", modifier = Modifier.padding(vertical = 16.dp),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            items(state.collections, key = { requireNotNull(it.id) }) { collection ->
                                CompositionLocalProvider(LocalHapticFeedback provides NoNativeHaptics) {
                                    CheckboxPreference(
                                        title = collection.title?.takeIf { it.isNotBlank() } ?: "默认收藏夹",
                                        checked = collection.id in state.selectedIds,
                                        onCheckedChange = { performHaptic(); onToggle(requireNotNull(collection.id)) },
                                        checkboxLocation = CheckboxLocation.End, enabled = !state.saving,
                                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        modifier = Modifier.collectionSheetRow(enabled = !state.saving),
                                    )
                                }
                            }
                        }
                    }
                }
                if (state.loaded && !creating) {
                    BasicComponent(
                        title = "新建收藏夹", onClick = rememberHapticClick { creating = true }, enabled = !state.saving,
                        modifier = Modifier.padding(bottom = 8.dp).collectionSheetRow(enabled = !state.saving),
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        startAction = { top.yukonga.miuix.kmp.basic.Icon(MiuixIcons.Add, null,
                            tint = MiuixTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp).size(24.dp)) },
                    )
                }
            }
        }
    } else if (show) {
        MaterialCollectionSheet(
            state = state, creating = creating, title = title, isPublic = isPublic, canSave = canSave,
            onTitleChange = { title = it }, onPublicChange = { isPublic = it },
            onCreateMode = { creating = true }, onBack = cancel,
            onDismiss = { onDismiss(); resetForm() }, onRetry = onRetry,
            onToggle = onToggle, onConfirm = confirm,
        )
    }
}

@Composable
private fun Modifier.collectionSheetRow(enabled: Boolean = true): Modifier =
    contentPress(enabled = enabled).squircleClip(CardDefaults.CornerRadius)

@Composable
private fun CollectionSheetAction(enabled: Boolean, compact: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    top.yukonga.miuix.kmp.basic.IconButton(
        onClick = rememberHapticClick(onClick = onClick), enabled = enabled,
        minWidth = 48.dp, minHeight = if (compact) IconButtonDefaults.MinHeight else 48.dp,
        modifier = Modifier.contentPress(enabled = enabled, pressedScale = .92f)
            .semantics(mergeDescendants = true) { if (!enabled) disabled() },
        content = content,
    )
}
