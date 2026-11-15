package com.htdev.movie.htplay.home.download

import android.content.Context
import android.util.Log
import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadIndex
import com.bumptech.glide.Glide
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Posters belong to the offline library, so keep them outside Glide's evictable disk cache. */
@androidx.annotation.OptIn(UnstableApi::class)
internal object OfflinePosters {
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val retryAfter = ConcurrentHashMap<String, Long>()

    fun source(context: Context, posterUrl: String?): String? =
        cachedFile(context, posterUrl)?.absolutePath ?: posterUrl?.takeIf { it.isNotBlank() }

    fun cachedFile(context: Context, posterUrl: String?): File? =
        posterUrl?.takeIf { it.isNotBlank() }?.let { file(context, it) }
            ?.takeIf { it.isFile && it.length() > 0 }

    suspend fun cache(context: Context, posterUrl: String?, index: DownloadIndex? = null) = withContext(Dispatchers.IO) {
        val url = posterUrl?.takeIf { it.isNotBlank() } ?: return@withContext
        locks.getOrPut(url) { Mutex() }.withLock {
            if (cachedFile(context, url) != null || (index != null && !isUsed(index, url))) return@withLock
            val now = SystemClock.elapsedRealtime()
            if (now < (retryAfter[url] ?: 0L)) return@withLock
            retryAfter[url] = now + 30_000
            val app = context.applicationContext
            val target = file(app, url)
            var temporary: File? = null
            val request = Glide.with(app).downloadOnly().load(url).timeout(15_000).submit()
            try {
                val downloaded = request.get()
                if (!target.parentFile!!.isDirectory && !target.parentFile!!.mkdirs()) {
                    throw IOException("Cannot create offline poster directory")
                }
                temporary = File.createTempFile(target.name, ".tmp", target.parentFile)
                downloaded.copyTo(temporary, overwrite = true)
                if (!temporary.renameTo(target)) throw IOException("Cannot save offline poster")
                retryAfter.remove(url)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("OfflinePosters", "Could not save download poster", error)
            } finally {
                Glide.with(app).clear(request)
                temporary?.delete()
            }
        }
    }

    suspend fun removeIfUnused(context: Context, posterUrl: String?, index: DownloadIndex) = withContext(Dispatchers.IO) {
        val url = posterUrl?.takeIf { it.isNotBlank() } ?: return@withContext
        locks.getOrPut(url) { Mutex() }.withLock {
            if (!isUsed(index, url)) file(context, url).delete()
        }
    }

    private fun isUsed(index: DownloadIndex, url: String): Boolean = index.getDownloads().use { cursor ->
        while (cursor.moveToNext()) {
            if (DownloadMetadata.fromByteArray(cursor.download.request.data).posterUrl == url) return true
        }
        false
    }

    private fun file(context: Context, url: String) = File(
        File(context.filesDir, "offline_posters"), UUID.nameUUIDFromBytes(url.toByteArray(Charsets.UTF_8)).toString(),
    )
}
