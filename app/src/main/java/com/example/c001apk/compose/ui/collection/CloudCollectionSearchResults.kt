package com.example.c001apk.compose.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.c001apk.compose.logic.repository.CloudCollectionSearchState
import com.example.c001apk.compose.ui.component.design.*
import com.example.c001apk.compose.ui.component.rememberHapticClick

@Composable
fun CloudCollectionSearchResults(
    query: String,
    state: CloudCollectionSearchState,
    paddingValues: PaddingValues,
    onRetry: () -> Unit,
    onViewFeed: (String, Boolean) -> Unit,
) {
    val results = remember(query, state.entries) { state.entries.filter { it.document.matches(query) } }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues).testTag("cloud-body-results"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("status") {
            Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        state.loading || !state.started -> "正在搜索正文 · 已找到 ${results.size} 条"
                        state.error != null -> "搜索未完成 · 已找到 ${results.size} 条"
                        else -> "找到 ${results.size} 条收藏动态"
                    }, style = MaterialTheme.typography.titleSmall,
                )
                if (state.loading || !state.started) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(if (state.total == 0) "正在读取收藏夹中的动态…"
                        else "已检索 ${state.scanned} / ${state.total} 条正文", style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor())
                }
                state.error?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor())
                    TextButton(onClick = rememberHapticClick(onClick = onRetry), enabled = !state.loading) { Text("重试未完成的搜索") }
                }
            }
        }
        if (results.isEmpty() && state.started && !state.loading && state.error == null) item("empty") {
            Text("收藏的动态正文中没有找到“${query.trim()}”", Modifier.padding(vertical = 24.dp),
                color = secondaryTextColor())
        }
        items(results, key = { it.document.id }) { entry ->
            val document = entry.document
            val excerpt = remember(document, query) { document.excerpt(query) }
            val matchStart = excerpt.indexOf(query.trim(), ignoreCase = true)
            val highlighted = buildAnnotatedString {
                append(excerpt)
                if (matchStart >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary), matchStart, matchStart + query.trim().length)
            }
            Column(Modifier.fillMaxWidth().contentPress().contentCard()
                .clickable(onClick = rememberHapticClick { onViewFeed(document.id, false) }).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(document.title.ifBlank { document.author.ifBlank { "收藏动态" } },
                    style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (document.title.isNotBlank() && document.author.isNotBlank()) Text(document.author,
                    style = MaterialTheme.typography.labelMedium, color = secondaryTextColor())
                Text(highlighted, style = MaterialTheme.typography.bodyMedium)
                Text(entry.folders.joinToString(" · "), style = MaterialTheme.typography.labelMedium,
                    color = secondaryTextColor(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
