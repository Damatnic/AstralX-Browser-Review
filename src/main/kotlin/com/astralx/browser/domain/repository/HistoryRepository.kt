package com.astralx.browser.domain.repository

import com.astralx.browser.domain.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryItem>>
    fun searchHistory(query: String): Flow<List<HistoryItem>>
    suspend fun addToHistory(url: String, title: String? = null)
    suspend fun deleteHistoryItem(id: Long)
    suspend fun deleteHistoryByUrl(url: String)
    suspend fun clearHistory()
    suspend fun clearHistoryRange(startDate: Date, endDate: Date)
}