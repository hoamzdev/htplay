package com.htdev.movie.htplay.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.common.map
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetDataMoviePlayer
import com.htdev.movie.htplay.domain.usecase.movie.GetMovieDetail
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByCountry
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByGenres
import com.htdev.movie.htplay.domain.usecase.movie.GetListContinueWatching
import com.htdev.movie.htplay.domain.usecase.movie.SaveContinueWatching
import com.htdev.movie.htplay.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val movieSlug: String = "",
    val detail: Result<DataMovieDetail> = Result.Idle,
    val episodes: Result<List<MoviePlayer>> = Result.Idle,
    val moreLikeThis: Result<List<PosterMovie>> = Result.Idle,
    val countryMovies: Result<List<PosterMovie>> = Result.Idle,
    val continueWatching: ContinueWatching? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getMovieDetail: GetMovieDetail,
    private val getDataMoviePlayer: GetDataMoviePlayer,
    private val getMoviesByGenres: GetMoviesByGenres,
    private val getMoviesByCountry: GetMoviesByCountry,
    private val savedStateHandle: SavedStateHandle,
    private val getListContinueWatching: GetListContinueWatching,
    private val saveContinueWatching: SaveContinueWatching,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState = _uiState.asStateFlow()

    private var initialized = false
    private var loadJob: Job? = null
    private var pendingSave: Job? = null
    private var lastSavedProgress: ContinueWatching? = null
    private val currentSlug: String?
        get() = savedStateHandle[STATE_CURRENT_SLUG]

    fun initialize(movieSlug: String) {
        if (initialized) return
        initialized = true
        loadMovie(currentSlug ?: movieSlug.trim())
    }

    fun openMovie(movieSlug: String): Boolean {
        val slug = movieSlug.trim()
        if (slug.isEmpty() || slug == currentSlug) return false
        val history = savedStateHandle.get<ArrayList<String>>(STATE_HISTORY).orEmpty().toMutableList()
        currentSlug?.takeIf { it.isNotBlank() }?.let { history.add(it) }
        savedStateHandle[STATE_HISTORY] = ArrayList(history.takeLast(HISTORY_LIMIT))
        loadMovie(slug)
        return true
    }

    fun goBack(): Boolean {
        val history = savedStateHandle.get<ArrayList<String>>(STATE_HISTORY).orEmpty().toMutableList()
        if (history.isEmpty()) return false
        val slug = history.removeAt(history.lastIndex)
        savedStateHandle[STATE_HISTORY] = ArrayList(history)
        loadMovie(slug)
        return true
    }

    fun savePlaybackProgress(episode: MoviePlayer, position: Long, duration: Long): Job? {
        val state = _uiState.value
        val movie = (state.detail as? Result.Success)?.data ?: return null
        val episodes = (state.episodes as? Result.Success)?.data ?: return null
        val episodeIndex = episodes.indexOf(episode)
        // Player positions are milliseconds; require a nonzero whole second.
        if (episodeIndex < 0 || position < 1_000 || duration <= 0) return null
        val progress = ContinueWatching(
            name = movie.name,
            slugMovie = state.movieSlug,
            thumbnail = movie.thumbUrl.ifBlank { movie.posterUrl },
            genre = movie.genreSlug.ifBlank { movie.genre },
            currentEpisode = episodeIndex,
            lengthCurrentEpisode = duration,
            positionContinue = position.coerceAtMost(duration),
        )
        if (progress == lastSavedProgress) return pendingSave
        lastSavedProgress = progress
        val previous = pendingSave
        // Finish local writes even if Back destroys this ViewModel, keeping saves in order.
        return applicationScope.launch {
            previous?.join()
            try {
                saveContinueWatching(progress)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastSavedProgress = null
                Log.e("ContinueWatching", "Could not save playback progress", e)
            }
        }.also { pendingSave = it }
    }

    private fun loadMovie(slug: String) {
        loadJob?.cancel()
        savedStateHandle[STATE_CURRENT_SLUG] = slug
        if (slug.isEmpty()) {
            val error = Result.Error(IllegalArgumentException("Missing movie slug"))
            _uiState.value = DetailUiState(
                movieSlug = slug,
                detail = error,
                episodes = error,
                moreLikeThis = Result.Success(emptyList()),
                countryMovies = Result.Success(emptyList()),
            )
            return
        }

        _uiState.value = DetailUiState(
            movieSlug = slug,
            detail = Result.Loading,
            episodes = Result.Loading,
            moreLikeThis = Result.Loading,
            countryMovies = Result.Loading,
        )
        loadJob = viewModelScope.launch {
            launch {
                pendingSave?.join()
                val saved = request {
                    Result.Success(getListContinueWatching().first().firstOrNull { it.slugMovie == slug })
                }
                val episodes = request { getDataMoviePlayer(slug) }.map { player ->
                    (player.vietsub + player.longtieng).groupBy { it.id }.values.map { versions ->
                        versions.firstOrNull { it.linkM3u8.isNotBlank() } ?: versions.first()
                    }
                }
                _uiState.update { it.copy(episodes = episodes, continueWatching = (saved as? Result.Success)?.data) }
            }
            val result = request { getMovieDetail(slug) }
            _uiState.update { it.copy(detail = result) }
            val movie = (result as? Result.Success)?.data
            var genreMovies: Result<List<PosterMovie>> = Result.Loading
            var countryMovies: Result<List<PosterMovie>> = Result.Loading
            fun publishRecommendations() {
                val related = (genreMovies as? Result.Success)?.data.orEmpty()
                val relatedIds = related.mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()
                val relatedSlugs = related.mapNotNull { it.slug.takeIf(String::isNotBlank) }.toSet()
                _uiState.update { state ->
                    state.copy(
                        moreLikeThis = genreMovies,
                        countryMovies = countryMovies.map { candidates ->
                            candidates.filterNot { it.id in relatedIds || it.slug in relatedSlugs }
                                .take(RELATED_MOVIES_LIMIT)
                        },
                    )
                }
            }
            launch {
                val genre = movie?.genreSlug?.ifBlank { movie.genre }.orEmpty()
                genreMovies = if (movie == null || genre.isBlank() || genre.equals("unknown", true)) {
                    Result.Success(emptyList())
                } else {
                    // Request one extra so the current movie can be excluded from the 20 results.
                    request { getMoviesByGenres(genre, RELATED_MOVIES_LIMIT + 1) }.map {
                        it.otherMovies(movie, slug).take(RELATED_MOVIES_LIMIT)
                    }
                }
                publishRecommendations()
            }
            launch {
                val country = movie?.countrySlug.orEmpty().trim()
                countryMovies = if (movie == null || country.isBlank() || country.equals("unknown", true)) {
                    Result.Success(emptyList())
                } else {
                    // Leave room to exclude the current movie and all genre recommendations.
                    request { getMoviesByCountry(country, RELATED_MOVIES_LIMIT * 2 + 1) }.map {
                        it.otherMovies(movie, slug)
                    }
                }
                publishRecommendations()
            }
        }
    }

    private fun List<PosterMovie>.otherMovies(movie: DataMovieDetail, requestedSlug: String): List<PosterMovie> =
        filterNot {
            it.slug == requestedSlug || (movie.slug.isNotBlank() && it.slug == movie.slug) ||
                (movie.id.isNotBlank() && it.id == movie.id)
        }.distinctBy { it.slug.ifBlank { it.id } }

    private suspend fun <T> request(block: suspend () -> Result<T>): Result<T> = try {
        block().also { currentCoroutineContext().ensureActive() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        Result.Error(e)
    }

    private companion object {
        const val RELATED_MOVIES_LIMIT = 20
        const val HISTORY_LIMIT = 5 // Previous movies; the current movie is stored separately.
        const val STATE_CURRENT_SLUG = "detail_current_slug"
        const val STATE_HISTORY = "detail_history"
    }
}
