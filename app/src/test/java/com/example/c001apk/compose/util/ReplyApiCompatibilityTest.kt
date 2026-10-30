package com.example.c001apk.compose.util

import com.example.c001apk.compose.logic.network.ApiService
import okhttp3.Request
import okhttp3.FormBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ReplyApiCompatibilityTest {
    @Test
    fun privateFeedUsesModernHeadersWithoutChangingVisibilityOrUploadedImages() {
        for (host in listOf("api.coolapk.com", "api2.coolapk.com")) {
            val service = Retrofit.Builder().baseUrl("https://$host")
                .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
            val original = legacyHeaders(service.postCreateFeed(hashMapOf(
                "id" to "", "message" to "offline privacy fixture", "type" to "feed", "status" to "-1",
                "pic" to "image-a.jpg,image-b.jpg", "targetType" to "apk", "targetId" to "123",
            )).request())
            val updated = original.withFeedApiCompatibility()
            assertEquals("2609291", updated.header("X-App-Code"))
            assertEquals("16", updated.header("X-Api-Version"))
            assertEquals("16.6.4", updated.header("X-App-Version"))
            assertEquals("2609291", updated.header("X-App-Supported"))
            assertEquals("Dalvik/test +CoolMarket/16.6.4-2609291-universal", updated.header("User-Agent"))
            assertEquals(original.url, updated.url)
            assertEquals("POST", updated.method)
            assertSame(original.body, updated.body)
            val form = updated.body as FormBody
            val fields = (0 until form.size).associate { form.name(it) to form.value(it) }
            assertEquals("-1", fields["status"])
            assertEquals("image-a.jpg,image-b.jpg", fields["pic"])
            assertEquals("123", fields["targetId"])
            for (name in listOf("Cookie", "X-App-Token", "X-App-Device")) {
                assertEquals(original.header(name), updated.header(name))
            }
            assertEquals(updated.headers, updated.withFeedApiCompatibility().headers)
        }
    }

    @Test
    fun publicFeedsAndOtherPostingEndpointsKeepTheirExistingProtocol() {
        val privateForm = FormBody.Builder().add("status", "-1").build()
        val requests = listOf(
            Request.Builder().url("https://api.coolapk.com/v6/feed/createFeed")
                .post(FormBody.Builder().add("status", "1").build()).build(),
            Request.Builder().url("https://api.coolapk.com/v6/feed/createFeed")
                .post(FormBody.Builder().add("message", "no visibility supplied").build()).build(),
            Request.Builder().url("https://api.coolapk.com/v6/feed/createFeed?status=-1").build(),
            Request.Builder().url("https://api.coolapk.com/v6/feed/createFeed")
                .post("status=-1".toRequestBody()).build(),
            Request.Builder().url("https://api.coolapk.com/v6/feed/reply").post(privateForm).build(),
            Request.Builder().url("https://example.com/v6/feed/createFeed").post(privateForm).build(),
        )
        requests.forEach { request ->
            val original = legacyHeaders(request)
            assertSame(original, original.withFeedApiCompatibility())
        }
    }

    @Test
    fun upgradesFeedAndNestedRepliesOnBothApiHostsWithoutChangingPaginationOrCredentials() {
        for (host in listOf("api.coolapk.com", "api2.coolapk.com")) {
            val service = Retrofit.Builder()
                .baseUrl("https://$host")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
            val requests = listOf(
                service.getFeedContentReply(
                    "123", "lastupdate_desc", 2, "456", "789", 0, "feed", 0, 1
                ).request(),
                service.getReply2Reply("456", 2, "789").request()
            )

            for (request in requests) {
                val original = legacyHeaders(request)
                val updated = original.withFeedApiCompatibility()

                assertEquals(original.url, updated.url)
                assertEquals(original.method, updated.method)
                assertEquals("16.6.4", updated.header("X-App-Version"))
                assertEquals("2609291", updated.header("X-App-Code"))
                assertEquals("2609291", updated.header("X-App-Supported"))
                assertEquals("16", updated.header("X-Api-Version"))
                assertEquals("Dalvik/test +CoolMarket/16.6.4-2609291-universal", updated.header("User-Agent"))
                for (name in listOf("Cookie", "X-App-Token", "X-App-Device")) {
                    assertEquals(original.header(name), updated.header(name))
                }
                for (name in listOf("X-App-Version", "X-App-Code", "X-App-Supported", "X-Api-Version", "User-Agent")) {
                    assertEquals(1, updated.headers.values(name).size)
                }
            }
        }
    }

    @Test
    fun leavesTopicHomeAndOtherRequestsUnchanged() {
        for (url in listOf(
            "https://api2.coolapk.com/v6/topic/newTagDetail?tag=Android",
            "https://api2.coolapk.com/v6/main/indexV8?page=1",
            "https://api.coolapk.com/v6/feed/detail?id=123",
            "https://api.coolapk.com/v6/user/replyList?page=1",
            "https://example.com/v6/feed/replyList?id=123",
            "https://api.coolapk.com/v6/feed/replyListOther?id=123"
        )) {
            val original = legacyHeaders(Request.Builder().url(url).build())
            assertSame(original, original.withFeedApiCompatibility())
        }
    }

    @Test
    fun preservesNewerCustomVersions() {
        val original = legacyHeaders(replyRequest()).newBuilder()
            .header("X-App-Version", "17.0.0")
            .header("X-App-Code", "2701011")
            .header("X-App-Supported", "2701011")
            .header("X-Api-Version", "17")
            .header("User-Agent", "Custom +CoolMarket/17.0.0-2701011-universal")
            .build()

        assertEquals(original.headers, original.withFeedApiCompatibility().headers)
    }

    @Test
    fun upgradesOldApiVersionWithoutDowngradingNewerAppVersion() {
        val original = legacyHeaders(replyRequest()).newBuilder()
            .header("X-App-Version", "17.0.0")
            .header("X-App-Code", "2701011")
            .header("X-App-Supported", "2701011")
            .header("User-Agent", "Custom +CoolMarket/17.0.0-2701011-universal")
            .build()

        val expected = original.newBuilder().header("X-Api-Version", "16").build()
        assertEquals(expected.headers, original.withFeedApiCompatibility().headers)
    }

    @Test
    fun handlesCustomUserAgentAndInvalidVersionNumbers() {
        val original = legacyHeaders(replyRequest()).newBuilder()
            .header("X-App-Code", "")
            .header("X-Api-Version", "invalid")
            .header("User-Agent", "CustomAgent/1.0")
            .build()

        val updated = original.withFeedApiCompatibility()
        assertEquals("2609291", updated.header("X-App-Code"))
        assertEquals("16", updated.header("X-Api-Version"))
        assertEquals("CustomAgent/1.0 +CoolMarket/16.6.4-2609291-universal", updated.header("User-Agent"))
        assertEquals(updated.headers, updated.withFeedApiCompatibility().headers)
    }

    private fun replyRequest() = Request.Builder()
        .url("https://api2.coolapk.com/v6/feed/replyList?id=123")
        .build()

    private fun legacyHeaders(request: Request) = request.newBuilder()
        .header("X-App-Version", "13.4.1")
        .header("X-App-Code", "2312121")
        .header("X-App-Supported", "2312121")
        .header("X-Api-Version", "13")
        .header("User-Agent", "Dalvik/test +CoolMarket/13.4.1-2312121-universal")
        .header("X-App-Device", "test-device")
        .header("X-App-Token", "test-token")
        .header("Cookie", "test-session")
        .build()
}
