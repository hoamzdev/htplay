package com.htdev.movie.htplay.home.download

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.htdev.movie.htplay.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    fun downloadEpisode(url: String, title: String) {
        val request = DownloadRequest.Builder(
            UUID.nameUUIDFromBytes(url.toByteArray(Charsets.UTF_8)).toString(), Uri.parse(url)
        ).setMimeType(MimeTypes.APPLICATION_M3U8)
            .setData(title.toByteArray(Charsets.UTF_8)).build()
        DownloadService.sendAddDownload(context, NotificationService::class.java, request, false)
        DownloadService.sendResumeDownloads(context, NotificationService::class.java, false)
    }
}
