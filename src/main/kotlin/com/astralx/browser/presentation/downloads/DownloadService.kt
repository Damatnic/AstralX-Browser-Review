package com.astralx.browser.downloads

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.astralx.browser.R
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class DownloadService : LifecycleService() {
    
    companion object {
        private const val TAG = "DownloadService"
        private const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_DOWNLOAD = "com.astralx.browser.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.astralx.browser.PAUSE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.astralx.browser.CANCEL_DOWNLOAD"
        
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_DOWNLOAD_TITLE = "download_title"
        const val EXTRA_IS_ADULT_CONTENT = "is_adult_content"
        const val EXTRA_DOWNLOAD_ID = "download_id"
        
        fun startDownload(
            context: Context,
            url: String,
            title: String,
            isAdultContent: Boolean
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_URL, url)
                putExtra(EXTRA_DOWNLOAD_TITLE, title)
                putExtra(EXTRA_IS_ADULT_CONTENT, isAdultContent)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    private lateinit var downloadEngine: AdvancedDownloadEngine
    private lateinit var notificationManager: NotificationManagerCompat
    private val activeNotifications = ConcurrentHashMap<Long, Notification>()
    
    override fun onCreate() {
        super.onCreate()
        
        downloadEngine = AdvancedDownloadEngine(this)
        notificationManager = NotificationManagerCompat.from(this)
        
        createNotificationChannel()
        observeDownloadProgress()
        
        Log.d(TAG, "DownloadService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: ""
                val title = intent.getStringExtra(EXTRA_DOWNLOAD_TITLE) ?: "Video"
                val isAdultContent = intent.getBooleanExtra(EXTRA_IS_ADULT_CONTENT, false)
                
                if (url.isNotEmpty()) {
                    startForeground(NOTIFICATION_ID, createServiceNotification())
                    startVideoDownload(url, title, isAdultContent)
                }
            }
            
            ACTION_PAUSE_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    pauseDownload(downloadId)
                }
            }
            
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    cancelDownload(downloadId)
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Video download notifications"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createServiceNotification(): Notification {
        val intent = Intent(this, DownloadService::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AstralX Downloads")
            .setContentText("Managing downloads in background")
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun observeDownloadProgress() {
        downloadEngine.downloadProgress.observe(this) { progressMap ->
            lifecycleScope.launch {
                updateDownloadNotifications(progressMap)
            }
        }
        
        downloadEngine.downloadQueue.observe(this) { downloads ->
            if (downloads.isEmpty()) {
                // No active downloads, stop foreground service
                stopForeground(true)
                stopSelf()
            }
        }
    }
    
    private fun startVideoDownload(url: String, title: String, isAdultContent: Boolean) {
        lifecycleScope.launch {
            try {
                val downloadId = downloadEngine.downloadVideo(
                    url = url,
                    title = title,
                    isAdultContent = isAdultContent,
                    enableNotifications = false // We handle notifications ourselves
                )
                
                Log.d(TAG, "Started download: $downloadId for $title")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download", e)
                showErrorNotification("Failed to start download: ${e.message}")
            }
        }
    }
    
    private fun updateDownloadNotifications(progressMap: Map<Long, DownloadProgress>) {
        progressMap.forEach { (downloadId, progress) ->
            when (progress.status) {
                DownloadStatus.DOWNLOADING -> {
                    showDownloadProgressNotification(downloadId, progress)
                }
                
                DownloadStatus.COMPLETED -> {
                    showDownloadCompletedNotification(downloadId, progress)
                    activeNotifications.remove(downloadId)
                }
                
                DownloadStatus.FAILED -> {
                    showDownloadFailedNotification(downloadId, progress)
                    activeNotifications.remove(downloadId)
                }
                
                DownloadStatus.CANCELLED -> {
                    cancelNotification(downloadId)
                    activeNotifications.remove(downloadId)
                }
                
                else -> {
                    // Handle other states if needed
                }
            }
        }
    }
    
    private fun showDownloadProgressNotification(downloadId: Long, progress: DownloadProgress) {
        val downloadInfo = downloadEngine.downloadQueue.value?.find { it.id == downloadId }
        val title = downloadInfo?.title ?: "Video Download"
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading... ${progress.progress}%")
            .setSmallIcon(R.drawable.ic_download_progress)
            .setProgress(100, progress.progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(createCancelAction(downloadId))
            .build()
        
        activeNotifications[downloadId] = notification
        notificationManager.notify(downloadId.toInt(), notification)
    }
    
    private fun showDownloadCompletedNotification(downloadId: Long, progress: DownloadProgress) {
        val downloadInfo = downloadEngine.downloadHistory.find { it.id == downloadId }
        val title = downloadInfo?.title ?: "Video Download"
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(downloadId.toInt(), notification)
    }
    
    private fun showDownloadFailedNotification(downloadId: Long, progress: DownloadProgress) {
        val downloadInfo = downloadEngine.downloadHistory.find { it.id == downloadId }
        val title = downloadInfo?.title ?: "Video Download"
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("$title - Failed to download")
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(downloadId.toInt(), notification)
    }
    
    private fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Error")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun createCancelAction(downloadId: Long): NotificationCompat.Action {
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        
        val cancelPendingIntent = PendingIntent.getService(
            this, downloadId.toInt(), cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_download,
            "Cancel",
            cancelPendingIntent
        ).build()
    }
    
    private fun pauseDownload(downloadId: Long) {
        lifecycleScope.launch {
            val success = downloadEngine.pauseDownload(downloadId)
            Log.d(TAG, "Pause download $downloadId: $success")
        }
    }
    
    private fun cancelDownload(downloadId: Long) {
        lifecycleScope.launch {
            val success = downloadEngine.cancelDownload(downloadId)
            if (success) {
                cancelNotification(downloadId)
                activeNotifications.remove(downloadId)
                Log.d(TAG, "Cancelled download: $downloadId")
            }
        }
    }
    
    private fun cancelNotification(downloadId: Long) {
        notificationManager.cancel(downloadId.toInt())
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cancel all active notifications
        activeNotifications.keys.forEach { downloadId ->
            cancelNotification(downloadId)
        }
        
        Log.d(TAG, "DownloadService destroyed")
    }
}