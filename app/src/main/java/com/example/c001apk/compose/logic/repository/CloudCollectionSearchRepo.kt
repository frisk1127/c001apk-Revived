package com.example.c001apk.compose.logic.repository

import com.example.c001apk.compose.di.Api1Service
import com.example.c001apk.compose.logic.model.HomeFeedResponse
import com.example.c001apk.compose.logic.network.ApiService
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jsoup.Jsoup
import java.io.IOException
import javax.inject.Inject

data class CloudCollectionDocument(val id: String, val title: String, val author: String, val body: String) {
    fun matches(query: String): Boolean = query.isNotBlank() && body.contains(query.trim(), ignoreCase = true)

    fun excerpt(query: String): String {
        val index = body.indexOf(query.trim(), ignoreCase = true).coerceAtLeast(0)
        val start = (index - 36).coerceAtLeast(0)
        val end = (index + query.trim().length + 120).coerceAtMost(body.length)
        return (if (start > 0) "…" else "") + body.substring(start, end) + if (end < body.length) "…" else ""
    }

    companion object {
        fun from(feed: HomeFeedResponse.Data): CloudCollectionDocument {
            val parts = mutableListOf(feed.message.orEmpty())
            feed.messageRawOutput?.takeIf { it.isNotBlank() && it != "null" }?.let { raw ->
                val content = JsonParser.parseString(raw)
                if (!content.isJsonArray) throw IOException("无法读取文章正文")
                content.asJsonArray.forEach { element ->
                    if (element.isJsonObject) {
                        val row = element.asJsonObject
                        fun text(key: String) = row[key]?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                        when (text("type")) {
                            "text" -> parts += text("message")
                            "shareUrl" -> parts += listOf(text("title"), text("description"))
                        }
                    }
                }
            }
            return CloudCollectionDocument(
                id = requireNotNull(feed.id), title = feed.title.orEmpty(),
                author = feed.username ?: feed.userInfo?.username.orEmpty(),
                body = parts.joinToString(" ") { Jsoup.parse(it).text() }.replace(Regex("\\s+"), " ").trim(),
            )
        }
    }
}

data class CloudCollectionSearchEntry(val document: CloudCollectionDocument, val folders: List<String>)

data class CloudCollectionSearchState(
    val entries: List<CloudCollectionSearchEntry> = emptyList(),
    val loading: Boolean = false,
    val started: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val folders: Int = 0,
    val error: String? = null,
)

class CloudCollectionSearchRepo @Inject constructor(@param:Api1Service private val api: ApiService) {
    fun scan(uid: String?, cache: MutableMap<String, CloudCollectionDocument>) = flow {
        val entries = mutableListOf<CloudCollectionSearchEntry>()
        val memberships = linkedMapOf<String, MutableSet<String>>()
        val folders = linkedMapOf<String, String>()
        var scanned = 0
        var failures = 0
        var firstError: String? = null
        suspend fun progress(loading: Boolean = true) = emit(CloudCollectionSearchState(
            entries = entries.toList(), loading = loading, started = true,
            scanned = scanned, total = memberships.size, folders = folders.size,
            error = if (failures > 0) "有 $failures 项未能完成搜索：${firstError.orEmpty()}" else null,
        ))
        fun failed(e: Exception) {
            if (e is CancellationException) throw e
            failures++
            if (firstError == null) firstError = e.message ?: "网络请求失败"
        }
        progress()
        try {
            pages("/v6/collection/list", uid, null, 1) { items ->
                items.filter { it.entityType == "collection" }.forEach { folder ->
                    folder.id?.takeIf(String::isNotBlank)?.let { folders[it] = folder.title?.takeIf(String::isNotBlank) ?: "默认收藏夹" }
                }
                progress()
            }
        } catch (e: Exception) { failed(e) }
        for ((id, title) in folders) {
            try {
                pages("/v6/collection/itemList", null, id, null) { items ->
                    items.filter { it.entityType == "feed" }.forEach { feed ->
                        feed.id?.takeIf(String::isNotBlank)?.let { memberships.getOrPut(it) { linkedSetOf() }.add(title) }
                    }
                    progress()
                }
            } catch (e: Exception) { failed(e) }
        }
        for (batch in memberships.entries.chunked(3)) {
            val results = coroutineScope {
                batch.map { (id, names) -> async {
                    try {
                        val document = cache[id] ?: api.getCollectionFeedDetail(id).let { response ->
                            if (!response.message.isNullOrBlank() || response.error?.let { it != 0 } == true ||
                                response.status?.let { it < 0 } == true) throw IOException(response.message ?: "读取正文失败")
                            val feed = response.data ?: throw IOException("正文不可用")
                            if (feed.id != id) throw IOException("正文与收藏记录不匹配")
                            CloudCollectionDocument.from(feed)
                        }
                        currentCoroutineContext().ensureActive()
                        Result.success(CloudCollectionSearchEntry(document, names.toList()))
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { Result.failure(e) }
                } }.awaitAll()
            }
            results.forEach { result ->
                scanned++
                result.fold(onSuccess = { cache[it.document.id] = it.document; entries += it },
                    onFailure = { failed(it as Exception) })
            }
            progress()
        }
        progress(loading = false)
    }.flowOn(Dispatchers.IO)

    private suspend fun pages(
        url: String, uid: String?, id: String?, showDefault: Int?,
        onPage: suspend (List<HomeFeedResponse.Data>) -> Unit,
    ) {
        var page = 1
        var cursor: String? = null
        val cursors = mutableSetOf<String>()
        while (true) {
            currentCoroutineContext().ensureActive()
            val response = api.getCollectionPage(url, uid, id, showDefault, page, cursor)
            if (!response.message.isNullOrBlank() || response.error?.let { it != 0 } == true ||
                response.status?.let { it < 0 } == true) throw IOException(response.message ?: "读取收藏列表失败")
            val items = response.data ?: throw IOException("服务器未返回收藏列表")
            if (items.isEmpty()) return
            onPage(items)
            val next = items.lastOrNull()?.let { it.entityId ?: it.id }
                ?: throw IOException("收藏列表缺少分页信息")
            if (!cursors.add(next)) return
            cursor = next
            page++
        }
    }
}
