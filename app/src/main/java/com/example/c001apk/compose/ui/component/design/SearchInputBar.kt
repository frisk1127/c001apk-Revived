package com.example.c001apk.compose.ui.component.design

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.ui.component.BackButton
import top.yukonga.miuix.kmp.basic.InputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInputBar(query: String, onQueryChange: (String) -> Unit, onSearch: (String) -> Unit,
    hint: String, onBack: () -> Unit, focusRequester: FocusRequester) {
    if (!com.example.c001apk.compose.ui.theme.isMiuix()) {
        TopAppBar(
            navigationIcon = { BackButton(onBack) },
            title = {
                androidx.compose.material3.TextField(
                    value = query, onValueChange = onQueryChange, singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { androidx.compose.material3.Text(hint, maxLines = 1) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    trailingIcon = {
                        if (query.isNotEmpty()) androidx.compose.material3.IconButton(onClick = { onQueryChange("") }) {
                            androidx.compose.material3.Icon(Icons.Default.Close, "清空搜索词")
                        }
                    },
                )
            },
            actions = {
                androidx.compose.material3.IconButton(onClick = { onSearch(query) }) {
                    androidx.compose.material3.Icon(Icons.Default.Search, "搜索")
                }
            },
        )
        return
    }
    Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onBack)
        InputField(query = query, onQueryChange = onQueryChange, onSearch = onSearch,
            expanded = true, onExpandedChange = {}, label = hint,
            modifier = Modifier.weight(1f).focusRequester(focusRequester))
    }
}
