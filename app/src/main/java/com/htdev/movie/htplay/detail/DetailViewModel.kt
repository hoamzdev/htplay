package com.htdev.movie.htplay.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.common.map
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetDataMoviePlayer
import com.htdev.movie.htplay.domain.usecase.movie.GetMovieDetail
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByCountry
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByGenres
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getMovieDetail: GetMovieDetail,
    private val getDataMoviePlayer: GetDataMoviePlayer,
    private val getMoviesByGenres: GetMoviesByGenres,
    private val getMoviesByCountry: GetMoviesByCountry,
) : ViewModel() {
    private val _detail = MutableStateFlow<Result<DataMovieDetail>>(Result.Idle)
    val detail = _detail.asStateFlow()

    private val _episodes = MutableStateFlow<Result<List<MoviePlayer>>>(Result.Idle)
    val episodes = _episodes.asStateFlow()

    private val _moreLikeThis = MutableStateFlow<Result<List<PosterMovie>>>(Result.Idle)
    val moreLikeThis = _moreLikeThis.asStateFlow()

    private val _countryMovies = MutableStateFlow<Result<List<PosterMovie>>>(Result.Idle)
    val countryMovies = _countryMovies.asStateFlow()

    private var initialized = false

    fun initialize(movieSlug: String) {
        if (initialized) return
        initialized = true
        val slug = movieSlug.trim()
        if (slug.isEmpty()) {
            val error = Result.Error(IllegalArgumentException("Missing movie slug"))
            _detail.value = error
            _episodes.value = error
            _moreLikeThis.value = Result.Success(emptyList())
            _countryMovies.value = Result.Success(emptyList())
            return
        }

        _detail.value = Result.Loading
        _episodes.value = Result.Loading
        _moreLikeThis.value = Result.Loading
        _countryMovies.value = Result.Loading
        viewModelScope.launch {
            launch {
                _episodes.value = request { getDataMoviePlayer(slug) }.map { player ->
                    (player.vietsub + player.longtieng).groupBy { it.id }.values.map { versions ->
                        versions.firstOrNull { it.linkM3u8.isNotBlank() } ?: versions.first()
                    }
                }
            }
            val result = request { getMovieDetail(slug) }
            _detail.value = result
            val movie = (result as? Result.Success)?.data
            launch {
                val genre = movie?.genreSlug?.ifBlank { movie.genre }.orEmpty()
                _moreLikeThis.value = if (movie == null || genre.isBlank() || genre.equals("unknown", true)) {
                    Result.Success(emptyList())
                } else {
                    // Request one extra so the current movie can be excluded from the 20 results.
                    request { getMoviesByGenres(genre, RELATED_MOVIES_LIMIT + 1) }.map {
                        it.otherMovies(movie, slug)
                    }
                }
            }
            launch {
                val country = movie?.countrySlug.orEmpty().trim()
                _countryMovies.value = if (movie == null || country.isBlank() || country.equals("unknown", true)) {
                    Result.Success(emptyList())
                } else {
                    request { getMoviesByCountry(country, RELATED_MOVIES_LIMIT + 1) }.map {
                        it.otherMovies(movie, slug)
                    }
                }
            }
        }
    }

    private fun List<PosterMovie>.otherMovies(movie: DataMovieDetail, requestedSlug: String): List<PosterMovie> =
        filterNot {
            it.slug == requestedSlug || (movie.slug.isNotBlank() && it.slug == movie.slug) ||
                (movie.id.isNotBlank() && it.id == movie.id)
        }.distinctBy { it.slug.ifBlank { it.id } }.take(RELATED_MOVIES_LIMIT)

    private suspend fun <T> request(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(e)
    }

    private companion object {
        const val RELATED_MOVIES_LIMIT = 20
    }
}
