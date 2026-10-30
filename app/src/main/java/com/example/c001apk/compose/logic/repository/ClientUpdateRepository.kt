package com.example.c001apk.compose.logic.repository

import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.constant.Constants.GITHUB_LATEST_RELEASE_API
import com.example.c001apk.compose.logic.model.ClientRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientUpdateRepository @Inject constructor() {
    // GitHub requests are independent of the authenticated Coolapk client.
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build()

    suspend fun latest(): ClientRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(GITHUB_LATEST_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2026-03-10")
            .header("User-Agent", "c001apk-Revived/${BuildConfig.VERSION_NAME}").build()
        client.newCall(request).execute().use { response ->
            when (response.code) {
                403, 429 -> error("GitHub 请求受限，请稍后重试，或直接打开发布页。")
                404 -> error("暂未找到正式发布版本，可直接打开发布页查看。")
            }
            check(response.isSuccessful) { "GitHub 暂不可用（${response.code}），请稍后重试。" }
            val source = response.body?.source() ?: error("GitHub 未返回更新信息")
            check(!source.request(1_048_577)) { "更新信息过大，请在 GitHub 查看。" }
            ClientRelease.parse(source.readUtf8())
        }
    }
}
