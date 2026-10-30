package com.example.c001apk.compose.util

import com.example.c001apk.compose.constant.Constants.APP_ID
import com.example.c001apk.compose.constant.Constants.CHANNEL
import com.example.c001apk.compose.constant.Constants.DARK_MODE
import com.example.c001apk.compose.constant.Constants.LOCALE
import com.example.c001apk.compose.constant.Constants.MODE
import com.example.c001apk.compose.constant.Constants.REQUEST_WITH
import com.example.c001apk.compose.util.CookieUtil.SESSID
import com.example.c001apk.compose.util.TokenDeviceUtils.getTokenV2
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import com.example.c001apk.compose.logic.repository.UserPreferencesRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Created by bggRGjQaUbCoE on 2024/6/3
 */
class AddCookiesInterceptor @Inject constructor(private val preferences: UserPreferencesRepository) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()
        val session = runBlocking { preferences.data.first() }
        val host = chain.request().url.host
        if (host != "coolapk.com" && !host.endsWith(".coolapk.com")) {
            return chain.proceed(builder.header("User-Agent", session.userAgent).build())
        }
        val deviceCode = session.xAppDevice
        val token = deviceCode.getTokenV2()
        builder.apply {
            addHeader("User-Agent", session.userAgent)
            addHeader("X-Requested-With", REQUEST_WITH)
            addHeader("X-Sdk-Int", session.sdkInt)
            addHeader("X-Sdk-Locale", LOCALE)
            addHeader("X-App-Id", APP_ID)
            addHeader("X-App-Token", token)
            addHeader("X-App-Version", session.versionName)
            addHeader("X-App-Code", session.versionCode)
            addHeader("X-Api-Version", session.apiVersion)
            addHeader("X-App-Device", deviceCode)
            addHeader("X-Dark-Mode", DARK_MODE)
            addHeader("X-App-Channel", CHANNEL)
            addHeader("X-App-Mode", MODE)
            addHeader("X-App-Supported", session.versionCode)
            addHeader("Content-Type", "application/x-www-form-urlencoded")
            if (session.isLogin)
                addHeader(
                    "Cookie",
                    "uid=${session.uid}; username=${session.username}; token=${session.token}"
                )
            else addHeader("Cookie", SESSID)
        }
        val response = chain.proceed(builder.build().withFeedApiCompatibility())
        val current = runBlocking { preferences.data.first() }
        if (session.sessionGeneration != current.sessionGeneration || session.xAppDevice != current.xAppDevice) {
            response.close()
            throw IOException("会话已更新，请重试")
        }
        return response
    }
}
