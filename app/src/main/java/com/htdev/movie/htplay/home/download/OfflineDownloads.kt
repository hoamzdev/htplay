package com.htdev.movie.htplay.home.download

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.Download
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@UnstableApi
object OfflineDownloads {
    private var instance: DownloadManager? = null
    private lateinit var downloadCache: SimpleCache
    private val posterScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val posterJobs = mutableMapOf<String, Job>()
    @Synchronized
    fun manager(context: Context): DownloadManager = instance ?: run {
        val app = context.applicationContext
        val database = StandaloneDatabaseProvider(app)
        val cache = SimpleCache(File(app.filesDir, "offline_media"), NoOpCacheEvictor(), database)
        downloadCache = cache
        DownloadManager(app, database, cache, DefaultHttpDataSource.Factory(),
            Executors.newFixedThreadPool(3)).also {
            it.maxParallelDownloads = 5
            it.addListener(object : DownloadManager.Listener {
                override fun onInitialized(downloadManager: DownloadManager) {
                    launchPosterTask {
                        val urls = downloadManager.downloadIndex.getDownloads().use { cursor ->
                            buildSet {
                                while (cursor.moveToNext()) {
                                    DownloadMetadata.fromByteArray(cursor.download.request.data).posterUrl?.let(::add)
                                }
                            }
                        }
                        urls.forEach { url -> cachePoster(app, url) }
                    }
                }

                override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                    val url = DownloadMetadata.fromByteArray(download.request.data).posterUrl
                    cachePoster(app, url)
                }

                override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                    val url = DownloadMetadata.fromByteArray(download.request.data).posterUrl
                    launchPosterTask { OfflinePosters.removeIfUnused(app, url, downloadManager.downloadIndex) }
                }
            })
            instance = it
        }
    }

    @Synchronized
    internal fun cachePoster(context: Context, posterUrl: String?) {
        val url = posterUrl?.takeIf { it.isNotBlank() } ?: return
        if (OfflinePosters.cachedFile(context, url) != null || posterJobs[url]?.isActive == true) return
        val index = manager(context).downloadIndex
        val app = context.applicationContext
        posterJobs[url] = launchPosterTask { OfflinePosters.cache(app, url, index) }
    }

    private fun launchPosterTask(block: suspend () -> Unit): Job = posterScope.launch {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w("OfflineDownloads", "Could not update offline poster", error)
        }
    }
    fun dataSource(context: Context, offlineOnly: Boolean = false): CacheDataSource.Factory {
        manager(context)
        return CacheDataSource.Factory().setCache(downloadCache)
            .setCacheWriteDataSinkFactory(null)
            .setUpstreamDataSourceFactory(if (offlineOnly) null else DefaultHttpDataSource.Factory())
    }
}
