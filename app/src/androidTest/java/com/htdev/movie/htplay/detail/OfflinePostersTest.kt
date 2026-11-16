package com.htdev.movie.htplay.detail

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.WritableDownloadIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.htdev.movie.htplay.home.download.DownloadMetadata
import com.htdev.movie.htplay.home.download.OfflineDownloads
import com.htdev.movie.htplay.home.download.OfflinePosters
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(UnstableApi::class)
class OfflinePostersTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun posterRemainsReadableWithoutItsOriginalSourceOrGlideCache() = runBlocking {
        val original = poster()
        val url = Uri.fromFile(original).toString()
        val index = OfflineDownloads.manager(context).downloadIndex
        try {
            OfflinePosters.cache(context, url)
            val saved = requireNotNull(OfflinePosters.cachedFile(context, url))
            assertTrue(saved.canonicalPath.startsWith(context.filesDir.canonicalPath + File.separator))
            assertTrue(original.delete())
            assertEquals(saved.absolutePath, OfflinePosters.source(context, url))
            val request = Glide.with(context).asBitmap().load(OfflinePosters.source(context, url))
                .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).submit()
            try {
                assertEquals(Color.BLUE, request.get().getPixel(0, 0))
            } finally {
                Glide.with(context).clear(request)
            }
        } finally {
            original.delete()
            OfflinePosters.removeIfUnused(context, url, index)
        }
    }

    @Test fun sharedPosterIsKeptUntilTheLastDownloadIsRemoved() = runBlocking {
        val original = poster()
        val url = Uri.fromFile(original).toString()
        val index = OfflineDownloads.manager(context).downloadIndex as WritableDownloadIndex
        val ids = List(2) { "poster-test-${UUID.randomUUID()}" }
        try {
            ids.forEachIndexed { number, id ->
                val request = DownloadRequest.Builder(id, Uri.parse("https://offline.invalid/$id.m3u8"))
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .setData(DownloadMetadata("Poster test", number + 1, "poster-test", url).toByteArray()).build()
                index.putDownload(Download(request, Download.STATE_COMPLETED, 0L, 0L, 0L, 0, 0))
            }
            OfflinePosters.cache(context, url, index)
            assertNotNull(OfflinePosters.cachedFile(context, url))
            index.removeDownload(ids[0])
            OfflinePosters.removeIfUnused(context, url, index)
            assertNotNull(OfflinePosters.cachedFile(context, url))
            index.removeDownload(ids[1])
            OfflinePosters.removeIfUnused(context, url, index)
            assertNull(OfflinePosters.cachedFile(context, url))
        } finally {
            ids.forEach { index.removeDownload(it) }
            OfflinePosters.removeIfUnused(context, url, index)
            original.delete()
        }
    }

    private fun poster(): File = File.createTempFile("poster-test-", ".png", context.cacheDir).also { file ->
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
