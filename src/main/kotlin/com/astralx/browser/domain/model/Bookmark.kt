package com.astralx.browser.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Bookmark(
    val id: String,
    val url: String,
    val title: String,
    val favicon: String? = null,
    val folder: String = "Default",
    val tags: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastVisited: Long = System.currentTimeMillis(),
    val visitCount: Int = 0,
    val description: String? = null,
    val isAdultContent: Boolean = false,
    val rating: Float = 0.0f, // 0-5 stars
    val quickAccess: Boolean = false
) : Parcelable {
    
    val domain: String
        get() = try {
            java.net.URL(url).host
        } catch (e: Exception) {
            ""
        }
    
    val isRecent: Boolean
        get() = System.currentTimeMillis() - lastVisited < 24 * 60 * 60 * 1000 // 24 hours
    
    val isPopular: Boolean
        get() = visitCount >= 10
    
    val formattedVisitCount: String
        get() = when {
            visitCount < 1000 -> visitCount.toString()
            visitCount < 1000000 -> "${visitCount / 1000}K"
            else -> "${visitCount / 1000000}M"
        }
}

@Parcelize
data class BookmarkFolder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val isPrivate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val color: String = "#6200EA", // Purple default
    val icon: String? = null,
    val sortOrder: Int = 0
) : Parcelable

enum class BookmarkSortType {
    TITLE_ASC,
    TITLE_DESC,
    DATE_ADDED_ASC,
    DATE_ADDED_DESC,
    VISIT_COUNT_ASC,
    VISIT_COUNT_DESC,
    LAST_VISITED_ASC,
    LAST_VISITED_DESC,
    RATING_ASC,
    RATING_DESC,
    DOMAIN
}

enum class BookmarkFilterType {
    ALL,
    RECENT,
    POPULAR,
    PRIVATE,
    ADULT_CONTENT,
    QUICK_ACCESS,
    UNVISITED,
    BY_FOLDER,
    BY_TAG
} 