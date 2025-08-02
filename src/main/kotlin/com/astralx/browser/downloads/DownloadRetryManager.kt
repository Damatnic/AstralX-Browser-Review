package com.astralx.browser.downloads

import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRetryManager @Inject constructor() {
    
    private val retryJobs = ConcurrentHashMap<Long, Job>()
    private val retryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 1000,
        val maxDelayMs: Long = 30000,
        val backoffMultiplier: Double = 2.0
    )
    
    fun scheduleRetry(
        downloadId: Long,
        attempt: Int,
        config: RetryConfig = RetryConfig(),
        onRetry: suspend () -> Result<Any>
    ) {
        if (attempt >= config.maxAttempts) {
            Timber.w("Max retry attempts reached for download $downloadId")
            return
        }
        
        cancelRetry(downloadId)
        
        val delay = calculateDelay(attempt, config)
        Timber.d("Scheduling retry for download $downloadId in ${delay}ms (attempt ${attempt + 1}/${config.maxAttempts})")
        
        val job = retryScope.launch {
            delay(delay)
            
            try {
                val result = onRetry()
                if (result.isFailure) {
                    scheduleRetry(downloadId, attempt + 1, config, onRetry)
                }
            } catch (e: Exception) {
                Timber.e(e, "Retry failed for download $downloadId")
                scheduleRetry(downloadId, attempt + 1, config, onRetry)
            }
        }
        
        retryJobs[downloadId] = job
    }
    
    fun cancelRetry(downloadId: Long) {
        retryJobs.remove(downloadId)?.cancel()
    }
    
    fun cancelAllRetries() {
        retryJobs.values.forEach { it.cancel() }
        retryJobs.clear()
    }
    
    private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
        val exponentialDelay = (config.initialDelayMs * Math.pow(config.backoffMultiplier, attempt.toDouble())).toLong()
        return minOf(exponentialDelay, config.maxDelayMs)
    }
    
    fun cleanup() {
        cancelAllRetries()
        retryScope.cancel()
    }
}