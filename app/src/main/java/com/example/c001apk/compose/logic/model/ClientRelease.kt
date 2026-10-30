package com.example.c001apk.compose.logic.model

import com.example.c001apk.compose.constant.Constants.URL_RELEASES
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class ClientRelease(
    val tag: String,
    val versionCode: Int,
    val name: String,
    val notes: String,
    val apkUrl: String?,
) {
    val pageUrl: String get() = "$URL_RELEASES/tag/$tag"

    companion object {
        private val stableTag = Regex("^([1-9][0-9]*)-[0-9a-fA-F]{7,40}$")

        fun parse(json: String): ClientRelease {
            val root = JsonParser.parseString(json).asJsonObject
            require(!root.get("draft").asBoolean && !root.get("prerelease").asBoolean) {
                "未找到正式发布版本"
            }
            val tag = root.string("tag_name")
            val version = stableTag.matchEntire(tag)?.groupValues?.get(1)?.toIntOrNull()
                ?: error("无法识别发布版本，请在 GitHub 查看")
            val apk = root.getAsJsonArray("assets")?.mapNotNull { element ->
                val asset = element.asJsonObject
                val name = asset.string("name")
                val url = asset.string("browser_download_url").toHttpUrlOrNull()
                if (asset.string("state") != "uploaded" || !name.endsWith(".apk", ignoreCase = true) ||
                    name.contains("debug", ignoreCase = true) ||
                    !(name.startsWith("c001apk-Revived_") || name.startsWith("c001apk-compose_")) ||
                    url == null || url.scheme != "https" || url.host != "github.com" ||
                    !url.encodedPath.startsWith("/frisk1127/c001apk-Revived/releases/download/$tag/")) null
                else name to url.toString()
            }?.sortedByDescending { (name, _) -> name.contains("_signed", ignoreCase = true) }
                ?.firstOrNull()?.second
            val body = root.string("body").ifBlank { "此版本未提供更新说明，可前往 GitHub 查看。" }
            val notes = if (body.length > 24_000) body.take(24_000) + "\n\n完整更新说明请前往 GitHub 查看。" else body
            return ClientRelease(tag, version, root.string("name").ifBlank { tag }, notes, apk)
        }

        private fun JsonObject.string(key: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()
    }
}
