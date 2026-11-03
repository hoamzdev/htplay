package com.htdev.movie.htplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.cachedIn
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.entities.PosterMovieHome
import com.htdev.movie.htplay.domain.entities.Genre
import com.htdev.movie.htplay.domain.usecase.movie.GetGenres
import com.htdev.movie.htplay.domain.usecase.movie.GetMovieByGenreAndCountry
import com.htdev.movie.htplay.domain.usecase.movie.GetCountries
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByCountry
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByGenres
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByPage
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesLatest
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesSeries
import com.htdev.movie.htplay.domain.usecase.movie.GetSavedGenres
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesByGenrePager
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesBySearch
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesPager
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesSeriesPager
import dagger.hilt.android.lifecycle.HiltViewModel
import com.htdev.movie.htplay.entities.ContinueWatching
import com.htdev.movie.htplay.entities.HomeData
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.entities.HomeGenrePage
import com.htdev.movie.htplay.util.normalizeFavoriteGenres
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val getMovieLastest: GetMoviesLatest,
    private val getMoviesByPage: GetMoviesByPage,
    private val getMoviesByGenres: GetMoviesByGenres,
    private val getMoviesSeries: GetMoviesSeries,
    private val getMoviesByCountry: GetMoviesByCountry,
    private val getMovieByGenreAndCountry: GetMovieByGenreAndCountry,
    private val getMoviesPager: GetMoviesPager,
    private val getMoviesByGenrePager: GetMoviesByGenrePager,
    private val getMoviesSeriesPager: GetMoviesSeriesPager,
    private val getMoviesBySearch: GetMoviesBySearch,
    private val getSavedGenres: GetSavedGenres,
    private val getCountries: GetCountries,
    private val getGenres: GetGenres,
) : ViewModel() {

    private val _genres = MutableStateFlow<Result<List<Genre>>>(Result.Loading)
    val genres = _genres.asStateFlow()

    private val _selectedHomeGenre = MutableStateFlow<String?>(null)
    val selectedHomeGenre = _selectedHomeGenre.asStateFlow()

    // One cached stream per active tab; changing tabs cancels the previous pager.
    @OptIn(ExperimentalCoroutinesApi::class)
    val homeGenreMovies = selectedHomeGenre.flatMapLatest { slug ->
        channelFlow {
            // Clear the previous tab while keeping refresh loading until this pager responds.
            send(HomeGenrePage(slug, PagingData.empty<PosterMovie>(sourceLoadStates = LoadStates(
                refresh = if (slug == null) LoadState.NotLoading(true) else LoadState.Loading,
                prepend = LoadState.NotLoading(true),
                append = LoadState.NotLoading(slug == null),
            ))))
            if (slug != null) {
                // This cache belongs to the selected tab and is cancelled when it changes.
                getMoviesByGenrePager(slug).cachedIn(this).collect { pages ->
                    send(HomeGenrePage(slug, pages))
                }
            }
        }
    }.shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    fun selectHomeGenre(slug: String?) {
        _selectedHomeGenre.value = slug
    }

    fun fetchGenres() {
        genresJob = loadMovies(_genres, genresJob) { getGenres() }
    }

    private val _movies = MutableStateFlow<Result<List<PosterMovie>>>(Result.Loading)
    val movies: StateFlow<Result<List<PosterMovie>>> = _movies.asStateFlow()

    private val _homeData = MutableStateFlow(HomeData())
    val homeData: StateFlow<HomeData> = _homeData.asStateFlow()

    private val _moviesByPage = MutableStateFlow<Result<List<PosterMovieHome>>>(Result.Idle)
    val moviesByPage: StateFlow<Result<List<PosterMovieHome>>> = _moviesByPage.asStateFlow()

    private val _moviesByGenre = MutableStateFlow<Result<List<PosterMovie>>>(Result.Idle)
    val moviesByGenre: StateFlow<Result<List<PosterMovie>>> = _moviesByGenre.asStateFlow()

    private val _seriesMovies = MutableStateFlow<Result<List<PosterMovie>>>(Result.Idle)
    val seriesMovies: StateFlow<Result<List<PosterMovie>>> = _seriesMovies.asStateFlow()

    private val _moviesByCountry = MutableStateFlow<Result<List<PosterMovie>>>(Result.Idle)
    val moviesByCountry: StateFlow<Result<List<PosterMovie>>> = _moviesByCountry.asStateFlow()

    private val _moviesByGenreAndCountry = MutableStateFlow<Result<List<PosterMovie>>>(Result.Idle)
    val moviesByGenreAndCountry: StateFlow<Result<List<PosterMovie>>> =
        _moviesByGenreAndCountry.asStateFlow()

    val moviesPager: Flow<PagingData<PosterMovie>> by lazy {
        getMoviesPager().cachedIn(viewModelScope)
    }

    private var latestMoviesJob: Job? = null
    private var moviesByPageJob: Job? = null
    private var moviesByGenreJob: Job? = null
    private var seriesMoviesJob: Job? = null
    private var moviesByCountryJob: Job? = null
    private var moviesByGenreAndCountryJob: Job? = null
    private var recommendedMoviesJob: Job? = null
    private var chineseMoviesJob: Job? = null
    private var koreanMoviesJob: Job? = null
    private var actionMoviesJob: Job? = null
    private var romanceMoviesJob: Job? = null
    private var familyMoviesJob: Job? = null
    private var countriesJob: Job? = null
    private var genresJob: Job? = null

    init {
        fetchMoviesByPage()
        fetchMovieLastest()
        fetchRecommendedMovies()
        fetchChineseMovies()
        fetchKoreanMovies()
        fetchActionMovies()
        fetchRomanceMovies()
        fetchFamilyMovies()
        fetchCountries()
        fetchGenres()
    }

    fun fetchRecommendedMovies() {
        recommendedMoviesJob = loadList(
            recommendedMoviesJob,
            { result -> _homeData.update { it.copy(recommendedMovies = result) } },
        ) {
            when (val saved = getSavedGenres()) {
                is Result.Success -> {
                    val Genres = normalizeFavoriteGenres(saved.data)
                    _homeData.update { it.copy(selectedGenreSlugs = Genres) }
                    val results = coroutineScope {
                        Genres.map { Genre -> async { getMoviesByGenres(Genre) } }.awaitAll()
                    }
                    val successful = results.filterIsInstance<Result.Success<List<PosterMovie>>>()
                    if (successful.isEmpty() && results.any { it is Result.Error }) {
                        results.filterIsInstance<Result.Error>().first()
                    } else {
                        Result.Success(successful.flatMap { it.data }.distinctBy { it.id })
                    }
                }
                is Result.Error -> saved
                else -> Result.Success(emptyList())
            }
        }
    }

    // Moi quoc gia co state va Job rieng de hai section khong ghi de len nhau.
    fun fetchChineseMovies() {
        chineseMoviesJob = loadList(
            chineseMoviesJob,
            { result -> _homeData.update { it.copy(chineseMovies = result) } },
        ) { getMoviesByCountry("trung-quoc") }
    }

    fun fetchKoreanMovies() {
        koreanMoviesJob = loadList(
            koreanMoviesJob,
            { result -> _homeData.update { it.copy(koreanMovies = result) } },
        ) { getMoviesByCountry("han-quoc") }
    }

    fun fetchCountries() {
        countriesJob = loadList(
            countriesJob,
            { result -> _homeData.update { it.copy(countries = result) } },
        ) { getCountries() }
    }

    fun fetchActionMovies() {
        actionMoviesJob = loadList(
            actionMoviesJob,
            { result -> _homeData.update { it.copy(actionMovies = result) } },
        ) { getMoviesByGenres("hanh-dong") }
    }

    fun fetchRomanceMovies() {
        romanceMoviesJob = loadList(
            romanceMoviesJob,
            { result -> _homeData.update { it.copy(romanceMovies = result) } },
        ) { getMoviesByGenres("tinh-cam") }
    }

    fun fetchFamilyMovies() {
        familyMoviesJob = loadList(
            familyMoviesJob,
            { result -> _homeData.update { it.copy(familyMovies = result) } },
        ) { getMoviesByGenres("gia-dinh") }
    }

    // Goi tu nguon lich su/player khi co du lieu tien do xem.
    fun updateContinueWatching(movies: List<ContinueWatching>) {
        _homeData.update { home ->
            home.copy(continueWatching = movies.filter {
                val progress = it.currentProgress ?: 0
                val duration = it.totalProgress ?: 0
                progress in 1..<duration
            }.distinctBy { it.id })
        }
    }

    fun retryHomeSection(sectionId: Int) {
        when (sectionId) {
            HomeItem.LATEST_MOVIES -> fetchMovieLastest()
            HomeItem.SELECTED_FOR_YOU -> fetchRecommendedMovies()
            HomeItem.CHINESE_MOVIES -> fetchChineseMovies()
            HomeItem.KOREAN_MOVIES -> fetchKoreanMovies()
            HomeItem.ACTION_MOVIES -> fetchActionMovies()
            HomeItem.ROMANCE_MOVIES -> fetchRomanceMovies()
            HomeItem.FAMILY_MOVIES -> fetchFamilyMovies()
            HomeItem.COUNTRIES -> fetchCountries()
        }
    }

    fun fetchMovieLastest() {
        latestMoviesJob = loadMovies(_movies, latestMoviesJob) {
            getMovieLastest()
        }
    }

    // Danh sach poster cho banner Home, khong phan trang.
    fun fetchMoviesByPage() {
        moviesByPageJob = loadMovies(_moviesByPage, moviesByPageJob) {
            getMoviesByPage()
        }
    }

    fun fetchMoviesByGenre(Genre: String, limit: Int = 20) {
        moviesByGenreJob = loadMovies(_moviesByGenre, moviesByGenreJob) {
            getMoviesByGenres(Genre, limit)
        }
    }

    // Genre la loai danh sach: phim-bo, phim-le, tv-shows, ...
    fun fetchMoviesSeries(Genre: String, limit: Int = 20) {
        seriesMoviesJob = loadMovies(_seriesMovies, seriesMoviesJob) {
            getMoviesSeries(Genre, limit)
        }
    }

    fun fetchMoviesByCountry(countrySlug: String, limit: Int = 20) {
        moviesByCountryJob = loadMovies(_moviesByCountry, moviesByCountryJob) {
            getMoviesByCountry(countrySlug, limit)
        }
    }

    fun fetchMoviesByGenreAndCountry(
        GenreSlug: String,
        countrySlug: String,
        limit: Int = 30,
    ) {
        moviesByGenreAndCountryJob = loadMovies(
            _moviesByGenreAndCountry,
            moviesByGenreAndCountryJob,
        ) {
            getMovieByGenreAndCountry(GenreSlug, countrySlug, limit)
        }
    }

    // UI collect cac Flow nay de nhan PagingData va submit vao PagingDataAdapter.
    fun fetchMoviesPager(): Flow<PagingData<PosterMovie>> = moviesPager

    fun fetchMoviesByGenrePager(Genre: String, limit: Int = 20): Flow<PagingData<PosterMovie>> {
        return getMoviesByGenrePager(Genre, limit).cachedIn(viewModelScope)
    }

    fun fetchMoviesSeriesPager(Genre: String, limit: Int = 20): Flow<PagingData<PosterMovie>> {
        return getMoviesSeriesPager(Genre, limit).cachedIn(viewModelScope)
    }

    fun fetchMoviesBySearch(
        keyword: String,
        Genre: String = "All",
        limit: Int = 20,
    ): Flow<PagingData<PosterMovie>> {
        return getMoviesBySearch(keyword, Genre, limit).cachedIn(viewModelScope)
    }

    private fun <T> loadMovies(
        state: MutableStateFlow<Result<List<T>>>,
        previousJob: Job?,
        request: suspend () -> Result<List<T>>,
    ): Job = loadList(previousJob, { state.value = it }, request)

    private fun <T> loadList(
        previousJob: Job?,
        updateState: (Result<List<T>>) -> Unit,
        request: suspend () -> Result<List<T>>,
    ): Job {
        previousJob?.cancel()
        updateState(Result.Loading)
        return viewModelScope.launch {
            val result = try {
                request()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.Error(e)
            }
            ensureActive()
            updateState(result)
        }
    }

}
