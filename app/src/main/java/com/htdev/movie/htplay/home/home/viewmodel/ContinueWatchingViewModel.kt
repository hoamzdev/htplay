package com.htdev.movie.htplay.home.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.usecase.movie.GetListContinueWatching
import com.htdev.movie.htplay.entities.ContinueWatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ContinueWatchingViewModel @Inject constructor(
    getListContinueWatching: GetListContinueWatching,
) : ViewModel() {
    val movies = flow { emitAll(getListContinueWatching()) }
        .map { movies ->
            movies.filter {
                it.slugMovie.isNotBlank() && it.positionContinue >= 1_000 &&
                    it.positionContinue < it.lengthCurrentEpisode
            }.distinctBy { it.slugMovie }.take(5).map { movie ->
                ContinueWatching(
                    id = UUID.nameUUIDFromBytes(movie.slugMovie.toByteArray(Charsets.UTF_8)),
                    slugMovie = movie.slugMovie,
                    urlThumbnail = movie.thumbnail,
                    currentProgress = movie.positionContinue / 1_000,
                    totalProgress = movie.lengthCurrentEpisode / 1_000,
                    name = movie.name,
                )
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
