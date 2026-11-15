package com.htdev.movie.htplay.home.download

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.htdev.movie.htplay.service.NotificationService
import com.htdev.movie.htplay.domain.usecase.movie.GetListContinueWatching
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class DownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getListContinueWatching: GetListContinueWatching,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.getStateFlow(QUERY, "")
    val isSearchOpen = savedStateHandle.getStateFlow(SEARCH_OPEN, false)

    fun setQuery(value: String) {
        savedStateHandle[QUERY] = value
    }

    fun openSearch() {
        savedStateHandle[SEARCH_OPEN] = true
    }

    fun closeSearch() {
        savedStateHandle[QUERY] = ""
        savedStateHandle[SEARCH_OPEN] = false
    }

    fun downloadEpisode(url: String, title: String, episodeNumber: Int, movieSlug: String, posterUrl: String?) {
        require(movieSlug.isNotBlank())
        val request = DownloadRequest.Builder(
            UUID.nameUUIDFromBytes(url.toByteArray(Charsets.UTF_8)).toString(), Uri.parse(url)
        ).setMimeType(MimeTypes.APPLICATION_M3U8)
            .setData(DownloadMetadata(title, episodeNumber, movieSlug, posterUrl?.trim()).toByteArray()).build()
        DownloadService.sendAddDownload(context, NotificationService::class.java, request, false)
        DownloadService.sendResumeDownloads(context, NotificationService::class.java, false)
    }

    internal suspend fun resolveMovieSlug(metadata: DownloadMetadata): String? =
        metadata.movieSlug ?: getListContinueWatching().first()
            .filter { it.name.equals(metadata.title, ignoreCase = true) }
            .map { it.slugMovie }.distinct().singleOrNull()

    private companion object {
        const val QUERY = "download_search_query"
        const val SEARCH_OPEN = "download_search_open"
    }
}
