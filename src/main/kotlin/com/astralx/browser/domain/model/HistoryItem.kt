package com.astralx.browser.domain.model

data class HistoryItem(
    val id: Long = 0,
    val url: String,
    val title: String,
    val visitTime: Long
)