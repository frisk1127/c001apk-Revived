package com.example.c001apk.compose.logic.repository

import com.example.c001apk.compose.logic.network.ApiService
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class DeleteCollectionTest {
    private fun repository(json: String? = null): NetworkRepo {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            if (json == null) throw IOException("网络不可用")
            val request = chain.request()
            assertEquals("POST", request.method)
            assertEquals("/v6/collection/delete", request.url.encodedPath)
            assertNull("Collection deletion reads the form body, not URL parameters", request.url.queryParameter("id"))
            assertTrue("Expected application/x-www-form-urlencoded body", request.body is FormBody)
            val form = request.body as FormBody
            assertEquals("application/x-www-form-urlencoded", form.contentType().toString())
            assertEquals(1, form.size)
            assertEquals("id", form.name(0))
            assertEquals("123", form.value(0))
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://example.invalid/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        return NetworkRepo(api, api, api, api)
    }

    @Test fun deletionPostsFolderIdInFormBody() = runBlocking {
        repository("""{"data":"删除成功"}""").deleteCollection("123")
    }

    @Test fun serverRejectionAndMissingResultAreNotSuccess() = runBlocking {
        for (json in listOf(
            """{"data":0}""", """{"data":false}""", """{"data":null}""",
            """{"data":1,"status":-1,"message":"不可删除默认收藏夹"}""",
        )) {
            assertTrue(runCatching { repository(json).deleteCollection("123") }.exceptionOrNull() is IOException)
        }
    }

    @Test fun networkFailureIsPreserved() = runBlocking {
        assertEquals("网络不可用", runCatching { repository().deleteCollection("123") }.exceptionOrNull()?.message)
    }

    @Test fun missingSelectionMessageIsPreserved() = runBlocking {
        val error = runCatching {
            repository("""{"data":null,"message":"请选择要删除的内容"}""").deleteCollection("123")
        }.exceptionOrNull()
        assertEquals("请选择要删除的内容", error?.message)
    }
}
