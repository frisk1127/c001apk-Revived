package com.example.c001apk.compose.logic.backup

import com.example.c001apk.compose.logic.model.FeedEntity
import com.example.c001apk.compose.logic.model.FeedMediaJson
import java.io.Writer

object FavoriteDocumentExport {
    fun writeMarkdown(favorites: List<FeedEntity>, writer: Writer) {
        favorites.forEachIndexed { index, item ->
            writer.append("# ").append((index + 1).toString()).append(". ")
                .append(item.uname).append("\n\n")
            writer.append("- 时间：").append(item.pubDate).append("\n")
                .append("- 设备：").append(item.device).append("\n")
                .append("- 链接：https://www.coolapk.com/feed/").append(item.id).append("\n\n")
                .append(item.message).append("\n\n")
            FeedMediaJson.decode(item.mediaJson)?.images?.forEach { image ->
                writer.append("![图片](").append(image).append(")\n\n")
            }
            writer.append("---\n\n")
        }
    }

    fun writeHtml(favorites: List<FeedEntity>, writer: Writer) {
        fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
        writer.append("<!doctype html><meta charset=\"utf-8\"><title>酷安动态导出</title>")
            .append("<style>body{max-width:900px;margin:auto;padding:24px;font:16px/1.7 system-ui;background:#f5f6f8}article{background:white;border-radius:14px;padding:20px;margin:16px 0;white-space:pre-wrap}small{color:#68707b}</style>")
        favorites.forEachIndexed { index, item ->
            val images = FeedMediaJson.decode(item.mediaJson)?.images.orEmpty()
                .joinToString("") { image ->
                    "<img loading=\"lazy\" style=\"max-width:100%;border-radius:10px\" src=\"${escape(image)}\" alt=\"图片\">"
                }
            writer.append("<article><h2>").append((index + 1).toString()).append(". ")
                .append(escape(item.uname)).append("</h2><small>")
                .append(escape(item.pubDate)).append(" · ").append(escape(item.device))
                .append(" · <a href=\"https://www.coolapk.com/feed/").append(escape(item.id))
                .append("\">打开原帖</a></small><p>").append(escape(item.message))
                .append("</p>").append(images).append("</article>")
        }
    }
}
