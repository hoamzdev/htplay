package com.htdev.movie.htplay.home.home

import com.htdev.movie.htplay.di.ApplicationScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.common.map
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetMovieDetail
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Keep one pick for each foreground session, across Home recreation and detail navigation. */
@Singleton
class HomeRecommendationSession @Inject constructor(
    private val getMovieDetail: GetMovieDetail,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val _movie = MutableStateFlow<Result<DataMovieDetail?>>(Result.Loading)
    val movie = _movie.asStateFlow()
    private val _sessionId = MutableStateFlow(0L)
    val sessionId = _sessionId.asStateFlow()
    private var selectedMovie: PosterMovie? = null
    private val remainingMovies = ArrayDeque<PosterMovie>()
    private var loadJob: Job? = null

    @Synchronized
    fun updateMovies(result: Result<List<PosterMovie>>) {
        if (selectedMovie != null) return
        when (result) {
            is Result.Success -> {
                remainingMovies.clear()
                // The first valid trailer in a random permutation is a uniform pick from
                // all valid trailers, without requesting every movie detail up front.
                remainingMovies.addAll(result.data.filter { it.slug.isNotBlank() }.distinctBy { it.slug }.shuffled())
                selectedMovie = remainingMovies.removeFirstOrNull()
                if (selectedMovie == null) _movie.value = Result.Success(null) else loadSelectedMovie()
            }
            is Result.Error -> _movie.value = result
            Result.Idle, Result.Loading -> _movie.value = Result.Loading
        }
    }

    @Synchronized
    fun startNewSession() {
        loadJob?.cancel()
        selectedMovie = null
        remainingMovies.clear()
        _movie.value = Result.Loading
        _sessionId.value += 1L
    }

    @Synchronized
    fun retry(): Boolean {
        if (selectedMovie == null) return false
        if (loadJob?.isActive != true) loadSelectedMovie()
        return true
    }

    private fun loadSelectedMovie() {
        if (selectedMovie == null) return
        _movie.value = Result.Loading
        loadJob = applicationScope.launch {
            while (true) {
                val selected = selectedMovie ?: break
                val result = try {
                    getMovieDetail(selected.slug).map { detail ->
                        detail.copy(
                            slug = selected.slug,
                            name = detail.name.ifBlank { selected.name },
                            posterUrl = detail.posterUrl.ifBlank { selected.posterUrl },
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Result.Error(error)
                }
                ensureActive()
                if (result is Result.Success && result.data.trailerUrl.isNullOrBlank()) {
                    // Try each candidate once so a list without trailers cannot loop forever.
                    selectedMovie = remainingMovies.removeFirstOrNull()
                    continue
                }
                if (result is Result.Error && remainingMovies.isNotEmpty()) {
                    selectedMovie = remainingMovies.removeFirst()
                    continue
                }
                _movie.value = result
                return@launch
            }
            _movie.value = Result.Success(null)
        }
    }
}
