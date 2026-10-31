package com.example.c001apk.compose.ui.search

enum class SearchOrderType(val title: String, val apiValue: String) {
    COMPREHENSIVE("综合", "default"),
    REALTIME("实时", "dateline"),
    DATELINE("时间", "default"), // 兼容 AtTopicActivity 等内部调用；主搜索菜单使用“综合”。
    HOT("热度", "hot"),
    REPLY("回复", "reply"),
}
