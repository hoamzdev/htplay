package com.htdev.movie.htplay.home.download

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.htdev.movie.htplay.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(UnstableApi::class)
class OfflinePlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var position = 0L
    private var shouldPlay = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = savedInstanceState?.getLong("position") ?: 0L
        shouldPlay = savedInstanceState?.getBoolean("play", true) ?: true
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val download = withContext(Dispatchers.IO) {
                OfflineDownloads.manager(this@OfflinePlayerActivity).downloadIndex
                    .getDownload(intent.getStringExtra("download_id").orEmpty())
            }
            if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return@launch
            if (download?.state != Download.STATE_COMPLETED) {
                Toast.makeText(this@OfflinePlayerActivity, R.string.download_not_ready, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            val view = PlayerView(this@OfflinePlayerActivity)
            setContentView(view)
            player = ExoPlayer.Builder(this@OfflinePlayerActivity)
                .setMediaSourceFactory(DefaultMediaSourceFactory(OfflineDownloads.dataSource(this@OfflinePlayerActivity, true)))
                .build().also {
                    view.player = it
                    view.keepScreenOn = true
                    it.setMediaItem(download.request.toMediaItem(), position)
                    it.prepare()
                    it.playWhenReady = shouldPlay
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("position", player?.currentPosition ?: position)
        outState.putBoolean("play", player?.playWhenReady ?: shouldPlay)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        player?.let { position = it.currentPosition; shouldPlay = it.playWhenReady; it.release() }
        player = null
        super.onStop()
    }
}
