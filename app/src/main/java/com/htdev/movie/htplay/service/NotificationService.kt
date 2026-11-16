package com.htdev.movie.htplay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.navigation.NavDeepLinkBuilder
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.home.HomeRouterActivity
import com.htdev.movie.htplay.home.download.DownloadMetadata
import com.htdev.movie.htplay.home.download.OfflineDownloads
import com.htdev.movie.htplay.home.download.OfflinePosters
import com.htdev.movie.htplay.util.SharePref

@UnstableApi
class NotificationService : DownloadService(1001, 1000L, DownloadNotifications.CHANNEL_ID,
    R.string.download_notification_channel, 0) {
    private var posterUrl: String? = null
    private var poster: Bitmap? = null
    private var posterTarget: CustomTarget<Bitmap>? = null
    private val openDownloads by lazy {
        NavDeepLinkBuilder(this).setComponentName(HomeRouterActivity::class.java)
            .setGraph(R.navigation.home_nav_router).setDestination(R.id.downloadFragment)
            .createPendingIntent()
    }

    override fun onCreate() {
        DownloadNotifications.createChannel(this)
        super.onCreate()
    }

    override fun getDownloadManager() = OfflineDownloads.manager(this)
    override fun getScheduler(): Scheduler? = null

    @SuppressLint("WrongConstant")
    override fun getForegroundNotification(downloads: List<Download>, notMetRequirements: Int): Notification {
        // Android requires a foreground notification even when detailed notifications are disabled.
        if (!SharePref.init(this).isEnableNotification() || !DownloadNotifications.areAllowed(this)) {
            loadPoster(null)
            return DownloadNotificationHelper(this, DownloadNotifications.CHANNEL_ID)
                .buildProgressNotification(this, R.drawable.ic_download, openDownloads, null,
                    downloads, notMetRequirements)
        }
        val current = downloads.firstOrNull { it.state == Download.STATE_DOWNLOADING }
            ?: downloads.firstOrNull()
        val metadata = current?.let { DownloadMetadata.fromByteArray(it.request.data) }
        loadPoster(metadata?.posterUrl)
        val percent = current?.percentDownloaded ?: -1f
        val downloading = current?.state == Download.STATE_DOWNLOADING
        val status = when {
            current?.state == Download.STATE_REMOVING -> getString(R.string.download_removing)
            current?.state == Download.STATE_STOPPED -> getString(R.string.download_stopped)
            notMetRequirements != 0 -> getString(R.string.download_notification_waiting_network)
            downloading && percent >= 0 -> getString(R.string.download_progress, percent.toInt())
            downloading -> getString(R.string.download_notification_downloading)
            else -> getString(R.string.download_queued)
        }
        val text = metadata?.episodeNumber?.let {
            "${getString(R.string.detail_episode_number, it)} · $status"
        } ?: status
        return NotificationCompat.Builder(this, DownloadNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(metadata?.title?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name))
            .setContentText(text)
            .setLargeIcon(poster)
            .setContentIntent(openDownloads)
            .setProgress(100, percent.coerceIn(0f, 100f).toInt(), downloading && percent < 0)
            .setSubText(if (downloads.size > 1) getString(R.string.download_notification_count, downloads.size) else null)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun loadPoster(url: String?) {
        if (posterUrl == url) return
        posterUrl = url
        poster = null
        posterTarget?.let { Glide.with(applicationContext).clear(it) }
        posterTarget = null
        if (url.isNullOrBlank()) return
        val target = object : CustomTarget<Bitmap>(128, 192) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (posterUrl == url) {
                    poster = resource
                    invalidateForegroundNotification()
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                if (posterUrl == url) poster = null
            }
        }
        posterTarget = target
        Glide.with(applicationContext).asBitmap().load(OfflinePosters.source(this, url))
            .timeout(15_000).into(target)
    }

    override fun onDestroy() {
        posterTarget?.let { Glide.with(applicationContext).clear(it) }
        posterTarget = null
        poster = null
        super.onDestroy()
    }
}
