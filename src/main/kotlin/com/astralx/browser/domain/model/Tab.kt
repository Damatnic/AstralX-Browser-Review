package com.astralx.browser.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tab(
    val id: String,
    val url: String,
    val title: String,
    val favicon: String?,
    val isIncognito: Boolean,
    val createdAt: Long,
    val lastAccessed: Long = System.currentTimeMillis(),
    val scrollPosition: Int = 0,
    val isSuspended: Boolean = false,
    val groupId: String? = null,
    val parentTabId: String? = null,
    val isPinned: Boolean = false
) : Parcelable 