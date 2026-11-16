package com.htdev.movie.htplay.detail

import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.WritableDownloadIndex
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.home.download.DownloadMetadata
import com.htdev.movie.htplay.home.download.OfflineDownloads
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(UnstableApi::class)
class DownloadDetailActivityTest {
    @Test fun downloadedMediaPlaysInDetailWithOnlyTheMovieAndTitleVisibleAfterRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = "download-detail-test-${UUID.randomUUID()}"
        val uri = Uri.parse("https://offline.invalid/$id.wav")
        val title = "Offline movie $id"
        val cache = OfflineDownloads.dataSource(context).createDataSource().cache
        val index = OfflineDownloads.manager(context).downloadIndex as WritableDownloadIndex
        val audio = silentWav()
        try {
            // Seed the real shared cache with local bytes; this test makes no HTTP requests.
            CacheWriter(
                CacheDataSource.Factory().setCache(cache)
                    .setUpstreamDataSourceFactory { ByteArrayDataSource(audio) }.createDataSource(),
                DataSpec(uri), null, null,
            ).cache()
            val request = DownloadRequest.Builder(id, uri).setMimeType(MimeTypes.AUDIO_WAV)
                .setData(DownloadMetadata(title, 10).toByteArray()).build()
            index.putDownload(Download(request, Download.STATE_COMPLETED, 0L, 0L, audio.size.toLong(), 0, 0))

            // A legacy download without a slug follows the same cache-only path even on Wi-Fi.
            ActivityScenario.launch<DetailActivity>(DetailActivity.createDownloadIntent(context, id)).use { scenario ->
                awaitPlayback(scenario)
                assertOfflineScreen(scenario, title)
                scenario.recreate()
                awaitPlayback(scenario)
                assertOfflineScreen(scenario, title)
            }
        } finally {
            index.removeDownload(id)
            cache.removeResource(uri.toString())
        }
    }

    private fun assertOfflineScreen(scenario: ActivityScenario<DetailActivity>, title: String) {
        scenario.onActivity { activity ->
            assertEquals(title, activity.findViewById<TextView>(R.id.tv_name_movie).text.toString())
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tv_name_movie).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.media_play).visibility)
            for (id in listOf(R.id.scroll_detail, R.id.tv_current_episode, R.id.iv_drop, R.id.progress_bar)) {
                assertEquals("Unexpected visible detail element: $id", View.INVISIBLE, activity.findViewById<View>(id).visibility)
            }
        }
    }

    private fun awaitPlayback(scenario: ActivityScenario<DetailActivity>) {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (SystemClock.elapsedRealtime() < deadline) {
            var ready = false
            scenario.onActivity { activity ->
                val player = activity.findViewById<PlayerView>(R.id.media_play).player
                assertNull(player?.playerError)
                ready = player != null && player.duration > 0 &&
                    player.playbackState in listOf(Player.STATE_READY, Player.STATE_ENDED)
            }
            if (ready) return
            SystemClock.sleep(100)
        }
        fail("Downloaded media never became playable from the local cache")
    }

    private fun silentWav(): ByteArray {
        val sampleRate = 8_000
        val pcmSize = sampleRate * 2 * 2
        return ByteBuffer.allocate(44 + pcmSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + pcmSize); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(sampleRate); putInt(sampleRate * 2)
            putShort(2); putShort(16); put("data".toByteArray()); putInt(pcmSize)
        }.array()
    }
}
