package com.example.c001apk.compose.logic.model

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement
import java.io.IOException

data class CloudCollection(
    @SerializedName(value = "id", alternate = ["collectionId"])
    val id: String?,
    val entityId: String?,
    val entityType: String?,
    @SerializedName(value = "title", alternate = ["name"])
    val title: String?,
    @SerializedName(value = "isBeCollected", alternate = ["is_be_collected"])
    val isCollected: Int?,
)

data class CollectionResponse<T>(
    val data: T?,
    val status: Int?,
    val code: Int?,
    val error: String?,
    val message: String?,
    val messageStatus: String?,
) {
    fun checkedData(): T {
        if ((!error.isNullOrBlank() && error != "0") ||
            (status != null && status !in listOf(0, 1, 200)) ||
            (code != null && code !in listOf(0, 1, 200)) ||
            messageStatus?.startsWith("err_") == true
        ) {
            throw IOException(message?.takeIf { it.isNotBlank() } ?: error ?: "收藏操作失败")
        }
        if (data == null || (data is JsonElement && data.isJsonNull)) {
            throw IOException(message?.takeIf { it.isNotBlank() } ?: "服务器未返回收藏结果")
        }
        return data
    }
}
