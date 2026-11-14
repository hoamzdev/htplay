package com.htdev.movie.htplay.home.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import java.io.File
import java.util.concurrent.Executors

@UnstableApi
object OfflineDownloads {
    private var instance: DownloadManager? = null
    private lateinit var downloadCache: SimpleCache
    @Synchronized
    fun manager(context: Context): DownloadManager = instance ?: run {
        val app = context.applicationContext
        val database = StandaloneDatabaseProvider(app)
        val cache = SimpleCache(File(app.filesDir, "offline_media"), NoOpCacheEvictor(), database)
        downloadCache = cache
        DownloadManager(app, database, cache, DefaultHttpDataSource.Factory(),
            Executors.newFixedThreadPool(3)).also {
            it.maxParallelDownloads = 2
            instance = it
        }
    }
    fun dataSource(context: Context, offlineOnly: Boolean = false): CacheDataSource.Factory {
        manager(context)
        return CacheDataSource.Factory().setCache(downloadCache)
            .setCacheWriteDataSinkFactory(null)
            .setUpstreamDataSourceFactory(if (offlineOnly) null else DefaultHttpDataSource.Factory())
    }
}
