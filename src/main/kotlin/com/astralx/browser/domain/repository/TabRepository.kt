package com.astralx.browser.domain.repository

import com.astralx.browser.domain.model.Tab

interface TabRepository {
    suspend fun getAllTabs(): List<Tab>
    suspend fun getTabById(id: String): Tab?
    suspend fun insertTab(tab: Tab)
    suspend fun updateTab(tab: Tab)
    suspend fun deleteTab(id: String)
    suspend fun deleteAllTabs()
    suspend fun deleteUnpinnedTabs()
    suspend fun getActiveTabs(): List<Tab>
    suspend fun suspendBackgroundTabs()
    suspend fun clearIncognitoTabs()
} 