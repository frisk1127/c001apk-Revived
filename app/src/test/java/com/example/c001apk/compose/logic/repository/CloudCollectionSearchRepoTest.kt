package com.example.c001apk.compose.logic.repository

import com.example.c001apk.compose.logic.network.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CloudCollectionSearchRepoTest {
    private val gson = Gson()
    private fun page(vararg rows: Map<String, String>) = gson.toJson(mapOf("data" to rows.toList()))
    private fun folder(id: String, title: String) = mapOf("id" to id, "entityId" to "collection$id", "entityType" to "collection", "title" to title)
    private fun feed(id: String) = mapOf("id" to id, "entityId" to "feed$id", "entityType" to "feed", "message" to "列表摘要")
    private fun detail(id: String, message: String, raw: String? = null) = gson.toJson(mapOf("data" to mapOf(
        "id" to id, "entityType" to "feed", "message" to message,
        "title" to "关键字标题", "username" to "关键字作者", "message_raw_output" to raw,
    )))
    private fun repository(respond: (Request) -> String): CloudCollectionSearchRepo {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(respond(request).toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://example.invalid/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        return CloudCollectionSearchRepo(api)
    }

    @Test fun searchesCompleteBodiesAcrossAllPagesAndFoldersOncePerPost() = runBlocking {
        val requests = CopyOnWriteArrayList<Request>()
        val repo = repository { request ->
            requests += request
            val id = request.url.queryParameter("id")
            val pageNumber = request.url.queryParameter("page")
            when (request.url.encodedPath) {
                "/v6/collection/list" -> when (pageNumber) {
                    "1" -> page(folder("a", "关键字资料"))
                    "2" -> page(folder("b", "稍后阅读"))
                    else -> page()
                }
                "/v6/collection/itemList" -> when (id to pageNumber) {
                    "a" to "1" -> page(feed("1"))
                    "a" to "2" -> page(feed("2"))
                    "b" to "1" -> page(feed("1"), feed("3"))
                    else -> page()
                }
                else -> when (id) {
                    "1" -> detail("1", "前文".repeat(200) + "关键字在正文末尾")
                    "2" -> detail("2", "文章摘要", gson.toJson(listOf(
                        mapOf("type" to "text", "message" to "<p>长文章的<b>关键字</b>与 A&amp;B</p>"),
                        mapOf("type" to "image", "url" to "https://example.invalid/图片关键字.jpg"),
                    )))
                    else -> detail("3", "没有匹配内容")
                }
            }
        }
        val states = repo.scan(null, mutableMapOf()).toList()
        val last = states.last()
        assertFalse(last.loading)
        assertNull(last.error)
        assertEquals(3, last.entries.size)
        val matched = last.entries.filter { it.document.matches("关键字") }
        assertEquals(setOf("1", "2"), matched.map { it.document.id }.toSet())
        assertTrue(matched.first { it.document.id == "1" }.document.excerpt("关键字").contains("关键字在正文末尾"))
        assertTrue(matched.first { it.document.id == "2" }.document.matches("A&B"))
        assertEquals(listOf("关键字资料", "稍后阅读"), last.entries.first { it.document.id == "1" }.folders)
        assertEquals(1, requests.count { it.url.encodedPath == "/v6/feed/detail" && it.url.queryParameter("id") == "1" })
        assertEquals("collectiona", requests.first { it.url.encodedPath == "/v6/collection/list" && it.url.queryParameter("page") == "2" }.url.queryParameter("lastItem"))
        assertEquals("feed1", requests.first { it.url.encodedPath == "/v6/collection/itemList" && it.url.queryParameter("page") == "2" }.url.queryParameter("lastItem"))
    }

    @Test fun partialFailureRetainsResultsAndRetryReusesOnlySuccessfulBodies() = runBlocking {
        var fail = true
        val details = CopyOnWriteArrayList<String>()
        val repo = repository { request ->
            when (request.url.encodedPath) {
                "/v6/collection/list" -> if (request.url.queryParameter("page") == "1") page(folder("a", "资料")) else page()
                "/v6/collection/itemList" -> if (request.url.queryParameter("page") == "1") page(feed("1"), feed("2")) else page()
                else -> {
                    val id = request.url.queryParameter("id")!!
                    details += id
                    if (id == "2" && fail) throw IOException("网络不可用")
                    detail(id, "完整正文")
                }
            }
        }
        val cache = mutableMapOf<String, CloudCollectionDocument>()
        val first = repo.scan(null, cache).toList().last()
        assertNotNull(first.error)
        assertEquals(listOf("1"), first.entries.map { it.document.id })
        fail = false
        val retried = repo.scan(null, cache).toList().last()
        assertNull(retried.error)
        assertEquals(2, retried.entries.size)
        assertEquals(1, details.count { it == "1" })
        assertEquals(2, details.count { it == "2" })
    }

    @Test fun folderPaginationFailureDoesNotClaimCompleteSearch() = runBlocking {
        val repo = repository { request ->
            when (request.url.encodedPath) {
                "/v6/collection/list" -> if (request.url.queryParameter("page") == "1") page(folder("a", "资料"))
                    else """{"message":"列表读取失败","data":null}"""
                "/v6/collection/itemList" -> if (request.url.queryParameter("page") == "1") page(feed("1")) else page()
                else -> detail("1", "可搜索的正文")
            }
        }
        val last = repo.scan(null, mutableMapOf()).toList().last()
        assertNotNull(last.error)
        assertEquals(1, last.entries.size)
    }

    @Test fun repeatedLastPageTerminatesWithoutDuplicateResults() = runBlocking {
        var calls = 0
        val repo = repository { request ->
            calls++
            if (request.url.encodedPath == "/v6/collection/list") page(folder("a", "资料")) else page()
        }
        val last = repo.scan(null, mutableMapOf()).toList().last()
        assertEquals(3, calls)
        assertNull(last.error)
        assertTrue(last.entries.isEmpty())
    }

    @Test fun cancellationStopsPaginationAfterAnInFlightRequest() = runBlocking {
        val requests = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val responseGate = CountDownLatch(1)
        val repo = repository {
            requests.incrementAndGet()
            started.complete(Unit)
            responseGate.await(5, TimeUnit.SECONDS)
            page(folder("a", "资料"))
        }
        val job = launch { repo.scan(null, mutableMapOf()).toList() }
        try {
            withTimeout(5_000) { started.await() }
            job.cancel()
        } finally { responseGate.countDown() }
        job.join()
        assertTrue(job.isCancelled)
        assertEquals(1, requests.get())
    }
}
