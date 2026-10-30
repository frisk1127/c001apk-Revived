package com.example.c001apk.compose.ui.search

enum class SearchOrderType(val title: String, val apiValue: String) {
    COMPREHENSIVE("综合", "none"),
    REALTIME("实时", "dateline"),
    DATELINE("时间", "default"),
    HOT("热度", "hot"),
    REPLY("回复", "reply"),
}
