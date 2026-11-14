package com.htdev.movie.htplay.service

import android.app.Notification
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.home.download.OfflineDownloads

@UnstableApi
class NotificationService : DownloadService(1001, 1000L, "offline_downloads", R.string.app_name, 0) {
    override fun getDownloadManager() = OfflineDownloads.manager(this)
    override fun getScheduler(): Scheduler? = null
    override fun getForegroundNotification(downloads: List<Download>, notMetRequirements: Int): Notification =
        DownloadNotificationHelper(this, "offline_downloads").buildProgressNotification(
            this, R.drawable.ic_download, null, null, downloads, notMetRequirements)
}
