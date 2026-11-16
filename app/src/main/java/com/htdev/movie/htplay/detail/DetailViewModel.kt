package com.htdev.movie.htplay.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.data.entities.dto.mapToFavorite
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.common.map
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.entities.DataMoviePlayer
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetDataMoviePlayer
import com.htdev.movie.htplay.domain.usecase.movie.GetMovieDetail
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByCountry
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByGenres
import com.htdev.movie.htplay.domain.usecase.movie.GetListContinueWatching
import com.htdev.movie.htplay.domain.usecase.movie.SaveContinueWatching
import com.htdev.movie.htplay.di.ApplicationScope
import com.htdev.movie.htplay.domain.usecase.movie.CheckMovieExistsInFavorite
import com.htdev.movie.htplay.domain.usecase.movie.ToggleFavoriteMovie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EpisodeServer { ORIGINAL, DUBBED }

data class DetailUiState(
    val movieSlug: String = "",
    val detail: Result<DataMovieDetail> = Result.Idle,
    val episodes: Result<List<MoviePlayer>> = Result.Idle,
    val episodeServers: DataMoviePlayer = DataMoviePlayer(),
    val selectedServer: EpisodeServer = EpisodeServer.ORIGINAL,
    val showServerTabs: Boolean = false,
    val moreLikeThis: Result<List<PosterMovie>> = Result.Idle,
    val countryMovies: Result<List<PosterMovie>> = Result.Idle,
    val continueWatching: ContinueWatching? = null,
    val isMovieFavorite: Boolean? = null,
    val isUpdatingFavorite: Boolean = false,
    val favoriteChange: Result<Boolean> = Result.Idle,
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
    private val toggleFavoriteMovie: ToggleFavoriteMovie,
    private val checkMovieExistsInFavorite: CheckMovieExistsInFavorite,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState = _uiState.asStateFlow()

    private var initialized = false
    private var serverSelectionAllowed = false
    private var loadJob: Job? = null
    private var pendingSave: Job? = null
    private var favoriteJob: Job? = null
    private var lastSavedProgress: ContinueWatching? = null
    private val currentSlug: String?
        get() = savedStateHandle[STATE_CURRENT_SLUG]

    fun initialize(movieSlug: String, language: String = "en", server: EpisodeServer? = null) {
        if (initialized) return
        initialized = true
        serverSelectionAllowed = language.substringBefore('-').substringBefore('_').equals("vi", true)
        if (server != null) savedStateHandle[STATE_CURRENT_SERVER] = server.name
        loadMovie(currentSlug ?: movieSlug.trim(), restoreServer = true)
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

    fun selectEpisodeServer(server: EpisodeServer) {
        val state = _uiState.value
        if (!state.showServerTabs || state.selectedServer == server) return
        val episodes = state.episodeServers.episodesFor(server)
        if (episodes.none { it.linkM3u8.isNotBlank() }) return
        savedStateHandle[STATE_CURRENT_SERVER] = server.name
        _uiState.update { it.copy(selectedServer = server, episodes = Result.Success(episodes)) }
    }

    private fun loadMovie(slug: String, restoreServer: Boolean = false) {
        loadJob?.cancel()
        val preferredServer = if (restoreServer) {
            EpisodeServer.entries.firstOrNull { it.name == savedStateHandle.get<String>(STATE_CURRENT_SERVER) }
                ?: EpisodeServer.ORIGINAL
        } else EpisodeServer.ORIGINAL
        savedStateHandle[STATE_CURRENT_SERVER] = preferredServer.name
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
            isUpdatingFavorite = favoriteJob?.isActive == true,
            detail = Result.Loading,
            episodes = Result.Loading,
            moreLikeThis = Result.Loading,
            countryMovies = Result.Loading,
        )
        loadJob = viewModelScope.launch {
            launch {
                checkMovieExistsInFavorite(slug)
                    .catch { error ->
                        _uiState.update { it.copy(favoriteChange = Result.Error(error)) }
                    }
                    .collect { isFavorite ->
                        _uiState.update { it.copy(isMovieFavorite = isFavorite) }
                    }
            }
            launch {
                pendingSave?.join()
                val saved = request {
                    Result.Success(getListContinueWatching().first().firstOrNull { it.slugMovie == slug })
                }
                val playerResult = request { getDataMoviePlayer(slug) }.map { player ->
                    player.copy(vietsub = player.vietsub.uniqueEpisodes(), longtieng = player.longtieng.uniqueEpisodes())
                }
                val servers = (playerResult as? Result.Success)?.data ?: DataMoviePlayer()
                val hasDubbed = servers.longtieng.any { it.linkM3u8.isNotBlank() }
                val selectedServer = if (serverSelectionAllowed && hasDubbed &&
                    (preferredServer == EpisodeServer.DUBBED || servers.vietsub.none { it.linkM3u8.isNotBlank() })) {
                    EpisodeServer.DUBBED
                } else EpisodeServer.ORIGINAL
                val episodes = playerResult.map { player ->
                    if (serverSelectionAllowed) player.episodesFor(selectedServer)
                    else (player.vietsub + player.longtieng).uniqueEpisodes()
                }
                savedStateHandle[STATE_CURRENT_SERVER] = selectedServer.name
                _uiState.update {
                    it.copy(
                        episodes = episodes,
                        episodeServers = servers,
                        selectedServer = selectedServer,
                        showServerTabs = serverSelectionAllowed && hasDubbed,
                        continueWatching = (saved as? Result.Success)?.data,
                    )
                }
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

    private fun DataMoviePlayer.episodesFor(server: EpisodeServer): List<MoviePlayer> =
        if (server == EpisodeServer.DUBBED) longtieng else vietsub

    private fun List<MoviePlayer>.uniqueEpisodes(): List<MoviePlayer> =
        groupBy { it.id }.values.map { versions ->
            versions.firstOrNull { it.linkM3u8.isNotBlank() } ?: versions.first()
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


    fun toggleFavorite() {
        val state = _uiState.value
        val movie = (state.detail as? Result.Success)?.data ?: return
        if (state.isMovieFavorite == null || favoriteJob?.isActive == true) return
        val favorite = movie.mapToFavorite().copy(slugMovie = state.movieSlug)
        _uiState.update { it.copy(isUpdatingFavorite = true, favoriteChange = Result.Idle) }
        favoriteJob = viewModelScope.launch {
            try {
                val result = request { Result.Success(toggleFavoriteMovie(favorite)) }
                _uiState.update {
                    if (it.movieSlug == state.movieSlug) it.copy(favoriteChange = result) else it
                }
            } finally {
                _uiState.update { it.copy(isUpdatingFavorite = false) }
            }
        }
    }

    fun clearFavoriteChange() {
        _uiState.update { it.copy(favoriteChange = Result.Idle) }
    }

    private companion object {
        const val RELATED_MOVIES_LIMIT = 20
        const val HISTORY_LIMIT = 5 // Previous movies; the current movie is stored separately.
        const val STATE_CURRENT_SLUG = "detail_current_slug"
        const val STATE_CURRENT_SERVER = "detail_current_server"
        const val STATE_HISTORY = "detail_history"
    }
}
