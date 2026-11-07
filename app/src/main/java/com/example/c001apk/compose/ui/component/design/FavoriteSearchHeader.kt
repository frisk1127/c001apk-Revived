package com.example.c001apk.compose.ui.component.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.c001apk.compose.ui.theme.isMiuix
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FavoriteSearchHeader(
    enabled: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    hint: String,
    titleBar: @Composable () -> Unit,
) {
    if (!enabled) {
        titleBar()
        return
    }
    val keyboard = LocalSoftwareKeyboardController.current
    if (isMiuix()) {
        val interactionSource = remember { MutableInteractionSource() }
        val focused by interactionSource.collectIsFocusedAsState()
        LaunchedEffect(expanded, focused) {
            // InputField owns the collapse animation; clear an unfocused restored query as well.
            if (!expanded && !focused && query.isNotEmpty()) onQueryChange("")
        }
        val setExpanded: (Boolean) -> Unit = {
            onExpandedChange(it)
            if (!it) keyboard?.hide()
        }
        Column(Modifier.fillMaxWidth()) {
            titleBar()
            SearchBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .padding(bottom = 12.dp),
                expanded = expanded,
                onExpandedChange = setExpanded,
                inputField = {
                    InputField(
                        query = query, onQueryChange = onQueryChange,
                        onSearch = { keyboard?.hide() }, label = hint,
                        expanded = expanded, onExpandedChange = setExpanded,
                        interactionSource = interactionSource,
                    )
                },
                outsideEndAction = {
                    Box(
                        modifier = Modifier.padding(end = 12.dp)
                            .heightIn(min = SearchBarDefaults.InputFieldMinHeight)
                            .clickable(interactionSource = null, indication = null, role = Role.Button) {
                                setExpanded(false)
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        top.yukonga.miuix.kmp.basic.Text(
                            text = "取消", color = MiuixTheme.colorScheme.primary,
                            style = MiuixTheme.textStyles.main.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                        )
                    }
                },
            ) {}
        }
    } else {
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
        val close = {
            onExpandedChange(false)
            onQueryChange("")
            keyboard?.hide()
            focusManager.clearFocus()
        }
        PageBackAction(expanded && lifecycleState == Lifecycle.State.RESUMED, close)
        if (expanded) {
            Row(Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = close) {
                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowBack, "退出搜索")
                }
                androidx.compose.material3.Surface(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    androidx.compose.material3.SearchBarDefaults.InputField(
                        query = query, onQueryChange = onQueryChange, onSearch = { keyboard?.hide() },
                        expanded = true, onExpandedChange = {},
                        placeholder = { androidx.compose.material3.Text(hint, maxLines = 1) },
                        leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) androidx.compose.material3.IconButton(onClick = { onQueryChange("") }) {
                                androidx.compose.material3.Icon(Icons.Default.Close, "清空搜索词")
                            }
                        }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        } else titleBar()
    }
}
