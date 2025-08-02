package com.astralx.browser.di

import android.content.Context
import com.astralx.browser.core.audio.*
import com.astralx.browser.core.performance.PerformanceMonitor
import com.astralx.browser.core.privacy.PrivacyConfigManager
import com.astralx.browser.core.privacy.SecureMemoryUtils
import com.astralx.browser.core.security.NetworkSecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    
    @Provides
    @Singleton
    fun providePerformanceMonitor(): PerformanceMonitor {
        return PerformanceMonitor()
    }
    
    @Provides
    @Singleton
    fun providePrivacyConfigManager(
        @ApplicationContext context: Context
    ): PrivacyConfigManager {
        return PrivacyConfigManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSecureMemoryUtils(): SecureMemoryUtils {
        return SecureMemoryUtils()
    }
    
    @Provides
    @Singleton
    fun provideAudioExtractor(
        quantumAudioExtractor: QuantumAudioExtractor
    ): AudioExtractor {
        return quantumAudioExtractor
    }
}