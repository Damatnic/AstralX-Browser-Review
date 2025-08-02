package com.astralx.browser.domain.repository

import com.astralx.browser.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun getAllBookmarks(): Flow<List<Bookmark>>
    fun getBookmarksByFolder(folderId: Long?): Flow<List<Bookmark>>
    suspend fun getBookmark(id: Long): Bookmark?
    suspend fun addBookmark(url: String, title: String, folderId: Long? = null): Long
    suspend fun updateBookmark(bookmark: Bookmark)
    suspend fun deleteBookmark(id: Long)
    suspend fun isBookmarked(url: String): Boolean
    suspend fun removeBookmark(url: String)
}