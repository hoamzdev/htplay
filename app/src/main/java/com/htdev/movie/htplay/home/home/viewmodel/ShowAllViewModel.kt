package com.htdev.movie.htplay.home.home.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetCountries
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesByCountryPager
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesByGenrePager
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesPager
import com.htdev.movie.htplay.entities.HomeViewAllTarget
import com.htdev.movie.htplay.util.normalizeFavoriteGenres
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShowAllMoviePage(
    val target: HomeViewAllTarget,
    val movies: PagingData<PosterMovie>,
)

@HiltViewModel
class ShowAllViewModel @Inject constructor(
    private val getMoviesPager: GetMoviesPager,
    private val getMoviesByGenrePager: GetMoviesByGenrePager,
    private val getMoviesByCountryPager: GetMoviesByCountryPager,
    private val getCountries: GetCountries,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var screen: HomeViewAllTarget? = null
    private val movieTarget = MutableStateFlow<HomeViewAllTarget?>(null)
    val selectedGenre = savedStateHandle.getStateFlow<String?>(SELECTED_GENRE, null)
    val genreSlugs: List<String>
        get() = (screen as? HomeViewAllTarget.Recommended)?.genreSlugs.orEmpty()

    private val _countries = MutableStateFlow<Result<List<Country>>>(Result.Idle)
    val countries = _countries.asStateFlow()
    private var countriesJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val movies = movieTarget.filterNotNull().flatMapLatest { target ->
        channelFlow {
            // Clear the old tab immediately and stay loading until the new pager responds.
            send(ShowAllMoviePage(target, PagingData.empty(
                sourceLoadStates = LoadStates(
                    refresh = LoadState.Loading,
                    prepend = LoadState.NotLoading(true),
                    append = LoadState.NotLoading(false),
                ),
            )))
            val pager = when (target) {
                HomeViewAllTarget.Latest -> getMoviesPager()
                is HomeViewAllTarget.ByGenre -> getMoviesByGenrePager(target.slug)
                is HomeViewAllTarget.ByCountry -> getMoviesByCountryPager(target.slug)
                else -> error("Unsupported movie target: $target")
            }
            pager.cachedIn(this).collect { send(ShowAllMoviePage(target, it)) }
        }
    // Keep the active pager cached while a detail screen covers this screen.
    }.shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    fun initialize(target: HomeViewAllTarget) {
        if (screen != null) return
        screen = if (target is HomeViewAllTarget.Recommended) {
            target.copy(genreSlugs = normalizeFavoriteGenres(target.genreSlugs))
        } else target
        when (val source = screen) {
            is HomeViewAllTarget.Recommended -> {
                val slug = selectedGenre.value?.takeIf { it in source.genreSlugs }
                    ?: source.genreSlugs.firstOrNull()
                savedStateHandle[SELECTED_GENRE] = slug
                movieTarget.value = slug?.let { HomeViewAllTarget.ByGenre(it) }
            }
            HomeViewAllTarget.Countries -> fetchCountries()
            HomeViewAllTarget.Latest,
            is HomeViewAllTarget.ByGenre,
            is HomeViewAllTarget.ByCountry -> movieTarget.value = source
            else -> Unit
        }
    }

    fun selectGenre(slug: String) {
        if (slug !in genreSlugs || slug == selectedGenre.value) return
        savedStateHandle[SELECTED_GENRE] = slug
        movieTarget.value = HomeViewAllTarget.ByGenre(slug)
    }

    fun isCurrentPage(target: HomeViewAllTarget) = movieTarget.value == target

    fun fetchCountries() {
        if (screen != HomeViewAllTarget.Countries) return
        countriesJob?.cancel()
        _countries.value = Result.Loading
        countriesJob = viewModelScope.launch {
            val result = try {
                getCountries()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.Error(e)
            }
            ensureActive()
            _countries.value = result
        }
    }

    private companion object {
        const val SELECTED_GENRE = "show_all_selected_genre"
    }
}
