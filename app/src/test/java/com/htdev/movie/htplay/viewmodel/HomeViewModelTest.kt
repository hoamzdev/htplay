package com.htdev.movie.htplay.viewmodel

import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.entities.Genre
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.htdev.movie.htplay.domain.usecase.movie.*
import com.htdev.movie.htplay.domain.usecase.movie.pager.*
import com.htdev.movie.htplay.entities.ContinueWatching
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.favorite.FavoriteViewModel
import com.htdev.movie.htplay.home.home.viewmodel.HomeViewModel
import com.htdev.movie.htplay.home.home.HomeRecommendationSession
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.PosterMovieHome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun returningFromDetailKeepsTheSelectedGenrePager() = runTest {
        val repository = FakeMovieRepository()
        var loads = 0
        repository.genrePager = { slug ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                object : PagingSource<Int, PosterMovie>() {
                    override fun getRefreshKey(state: PagingState<Int, PosterMovie>): Int? = null
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PosterMovie> {
                        loads++
                        return LoadResult.Page(listOf(movie(slug)), null, null)
                    }
                }
            }.flow
        }
        val viewModel = createViewModel(repository)
        viewModel.selectHomeGenre("hanh-dong")
        fun presenter() = object : PagingDataPresenter<PosterMovie>() {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<PosterMovie>) = Unit
        }
        val first = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeGenreMovies.collectLatest { presenter().collectFrom(it.movies) }
        }
        runCurrent()
        assertEquals(1, loads)
        first.cancel()
        runCurrent()
        advanceTimeBy(60_000)
        val returned = presenter()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeGenreMovies.collectLatest { returned.collectFrom(it.movies) }
        }
        runCurrent()
        assertEquals(listOf("hanh-dong"), returned.snapshot().items.map { it.slug })
        assertEquals(1, loads)
    }

    @Test
    fun homeGenreTabsUseApiGenresAndCanRetryLoadingThem() = runTest {
        val repository = FakeMovieRepository()
        repository.genres = { Result.Error(IllegalStateException("offline")) }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertNull(viewModel.selectedHomeGenre.value)
        assertTrue(viewModel.genres.value is Result.Error)

        val genres = listOf(Genre("API genre", "api-genre"))
        repository.genres = { Result.Success(genres) }
        viewModel.fetchGenres()
        advanceUntilIdle()
        assertEquals(Result.Success(genres), viewModel.genres.value)
    }

    @Test
    fun switchingHomeGenreCancelsOldPagerAndPopularClearsTheGrid() = runTest {
        val repository = FakeMovieRepository()
        val requested = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        repository.genrePager = { slug -> flow {
            requested += slug
            try {
                emit(PagingData.from(listOf(movie(slug))))
                awaitCancellation()
            } finally {
                cancelled += slug
            }
        } }
        val viewModel = createViewModel(repository)
        val presenter = object : PagingDataPresenter<PosterMovie>() {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<PosterMovie>) = Unit
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeGenreMovies.collectLatest { presenter.collectFrom(it.movies) }
        }
        runCurrent()
        assertTrue(requested.isEmpty())

        viewModel.selectHomeGenre("hanh-dong")
        runCurrent()
        assertEquals("hanh-dong", presenter.snapshot().items.single().slug)

        viewModel.selectHomeGenre("tinh-cam")
        runCurrent()
        assertEquals(listOf("hanh-dong"), cancelled)
        assertEquals("tinh-cam", presenter.snapshot().items.single().slug)
        viewModel.selectHomeGenre("tinh-cam")
        runCurrent()
        assertEquals(listOf("hanh-dong", "tinh-cam"), requested)

        viewModel.selectHomeGenre(null)
        runCurrent()
        assertEquals(listOf("hanh-dong", "tinh-cam"), cancelled)
        assertTrue(presenter.snapshot().items.isEmpty())
    }

    @Test
    fun genreStaysLoadingUntilItsFirstPageIsReadyIncludingWhenSwitchingTabs() = runTest {
        val repository = FakeMovieRepository()
        val responses = mapOf(
            "hanh-dong" to CompletableDeferred<PagingSource.LoadResult<Int, PosterMovie>>(),
            "tinh-cam" to CompletableDeferred<PagingSource.LoadResult<Int, PosterMovie>>(),
        )
        repository.genrePager = { slug ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                object : PagingSource<Int, PosterMovie>() {
                    override fun getRefreshKey(state: PagingState<Int, PosterMovie>): Int? = null
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PosterMovie> =
                        responses.getValue(slug).await()
                }
            }.flow
        }
        val viewModel = createViewModel(repository)
        val presenter = object : PagingDataPresenter<PosterMovie>() {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<PosterMovie>) = Unit
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.homeGenreMovies.collectLatest { presenter.collectFrom(it.movies) }
        }
        runCurrent()

        viewModel.selectHomeGenre("hanh-dong")
        runCurrent()
        assertTrue(presenter.snapshot().items.isEmpty())
        assertEquals(LoadState.Loading, presenter.loadStateFlow.value?.refresh)

        responses.getValue("hanh-dong").complete(PagingSource.LoadResult.Page(
            data = listOf(movie("action")), prevKey = null, nextKey = null,
        ))
        runCurrent()
        assertEquals("action", presenter.snapshot().items.single().slug)
        assertTrue(presenter.loadStateFlow.value?.refresh is LoadState.NotLoading)

        viewModel.selectHomeGenre("tinh-cam")
        runCurrent()
        assertTrue(presenter.snapshot().items.isEmpty())
        assertEquals(LoadState.Loading, presenter.loadStateFlow.value?.refresh)

        // Only a completed empty response should show the no-movies state.
        responses.getValue("tinh-cam").complete(PagingSource.LoadResult.Page(
            data = emptyList(), prevKey = null, nextKey = null,
        ))
        runCurrent()
        assertTrue(presenter.snapshot().items.isEmpty())
        assertTrue(presenter.loadStateFlow.value?.refresh is LoadState.NotLoading)
        assertEquals(true, presenter.loadStateFlow.value?.append?.endOfPaginationReached)
    }

    @Test
    fun recommendationsUseAllSavedGenresAndRemoveDuplicateMovies() = runTest {
        val repository = FakeMovieRepository()
        repository.GenreMovies = { Genre -> Result.Success(listOf(movie("shared"), movie(Genre))) }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(setOf("hanh-dong", "tinh-cam", "gia-dinh"), repository.requestedGenres.toSet())
        assertEquals(listOf("shared", "hanh-dong", "tinh-cam"),
            (viewModel.homeData.value.recommendedMovies as Result.Success).data.map { it.id })
    }

    @Test
    fun popularGenresLoadIndependentlyAndRetryOnlyTheFailedSection() = runTest {
        val repository = FakeMovieRepository()
        repository.GenreMovies = { slug ->
            if (slug == "hanh-dong") Result.Error(IllegalStateException("offline"))
            else Result.Success(listOf(movie(slug)))
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.homeData.value.actionMovies is Result.Error)
        assertEquals("tinh-cam", (viewModel.homeData.value.romanceMovies as Result.Success).data.single().slug)
        assertEquals("gia-dinh", (viewModel.homeData.value.familyMovies as Result.Success).data.single().slug)
        val romanceBefore = viewModel.homeData.value.romanceMovies
        val familyBefore = viewModel.homeData.value.familyMovies
        repository.requestedGenres.clear()
        repository.GenreMovies = { Result.Success(listOf(movie(it))) }

        viewModel.retryHomeSection(HomeItem.ACTION_MOVIES)
        advanceUntilIdle()
        assertEquals(listOf("hanh-dong"), repository.requestedGenres)
        assertEquals("hanh-dong", (viewModel.homeData.value.actionMovies as Result.Success).data.single().slug)
        assertSame(romanceBefore, viewModel.homeData.value.romanceMovies)
        assertSame(familyBefore, viewModel.homeData.value.familyMovies)
    }

    @Test
    fun failedChineseRequestDoesNotOverwriteKoreanOrRecommendedMovies() = runTest {
        val repository = FakeMovieRepository()
        repository.countryMovies = {
            if (it == "trung-quoc") Result.Error(IllegalStateException("offline"))
            else Result.Success(listOf(movie("korea")))
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.homeData.value.chineseMovies is Result.Error)
        assertEquals("korea", (viewModel.homeData.value.koreanMovies as Result.Success).data.single().id)
        assertTrue(viewModel.homeData.value.recommendedMovies is Result.Success)
        assertTrue(viewModel.homeData.value.countries is Result.Success)
    }

    @Test
    fun changingCountryPreventsLateOldResponseFromReplacingNewMovies() = runTest {
        val repository = FakeMovieRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        repository.countryMovies = { country ->
            if (country == "old") withContext(NonCancellable) { delay(100) }
            Result.Success(listOf(movie(country)))
        }
        viewModel.fetchMoviesByCountry("old")
        runCurrent()
        viewModel.fetchMoviesByCountry("new")
        advanceUntilIdle()

        assertEquals("new", (viewModel.moviesByCountry.value as Result.Success).data.single().id)
    }

    @Test
    fun continueWatchingExcludesFinishedUnstartedAndUnknownDuration() = runTest {
        val viewModel = createViewModel(FakeMovieRepository())
        val unfinished = ContinueWatching(slugMovie = "unfinished", totalProgress = 100, currentProgress = 40)
        viewModel.updateContinueWatching(listOf(unfinished, unfinished,
            ContinueWatching(slugMovie = "finished", totalProgress = 100, currentProgress = 100),
            ContinueWatching(slugMovie = "unstarted", totalProgress = 100, currentProgress = 0),
            ContinueWatching(slugMovie = "unknown-duration", currentProgress = 40)))

        assertEquals(listOf(unfinished), viewModel.homeData.value.continueWatching)
        advanceUntilIdle()
    }

    @Test
    fun favoriteSaveStaysLoadingUntilPersistenceCompletesAndIgnoresDoubleTap() = runTest {
        val saved = CompletableDeferred<Result<Boolean>>()
        val repository = FakeMovieRepository()
        var calls = 0
        repository.saveGenres = { calls++; saved.await() }
        val viewModel = FavoriteViewModel(SaveGenresFavorite(repository))
        viewModel.saveGenresFavorite(listOf("hanh-dong"))
        viewModel.saveGenresFavorite(listOf("tinh-cam"))
        runCurrent()
        assertSame(Result.Loading, viewModel.saveResult.value)
        assertEquals(1, calls)

        saved.complete(Result.Success(true))
        advanceUntilIdle()
        assertEquals(Result.Success(true), viewModel.saveResult.value)
    }

    private fun movie(id: String) = PosterMovie(id, "poster", id, id, "Episode 1")

    @Test fun trailerCanComeFromEveryHomeMovieSectionWhenLatestHasNoTrailer() = runTest {
        for (source in listOf("recommended", "trung-quoc", "han-quoc", "hanh-dong", "tinh-cam", "gia-dinh", "banner", "continue")) {
            val repository = FakeMovieRepository()
            repository.savedGenres = listOf("phieu-luu")
            repository.latestMovies = { Result.Success(listOf(movie("latest-without-trailer"))) }
            repository.GenreMovies = { genre ->
                Result.Success(if (genre == source || source == "recommended" && genre == "phieu-luu") listOf(movie(source)) else emptyList())
            }
            repository.countryMovies = { country ->
                Result.Success(if (country == source) listOf(movie(source)) else emptyList())
            }
            repository.bannerMovies = {
                Result.Success(if (source == "banner") listOf(PosterMovieHome(source, "poster", source, "", source, "", 2026, "")) else emptyList())
            }
            repository.movieDetail = { slug ->
                Result.Success(DataMovieDetail(slug = slug, trailerUrl = if (slug == source) "https://youtu.be/abcdefghijk" else " \t"))
            }
            val viewModel = createViewModel(repository)
            if (source == "continue") {
                viewModel.updateContinueWatching(listOf(ContinueWatching(slugMovie = source, totalProgress = 100, currentProgress = 40)))
            }
            advanceUntilIdle()
            assertEquals(source, source, viewModel.homeData.value.htRecommendation.movie?.slug)
        }
    }

    @Test fun trailerWaitsForSlowerSectionsAndDeduplicatesMoviesAcrossSources() = runTest {
        val repository = FakeMovieRepository()
        val pending = CompletableDeferred<Result<List<PosterMovie>>>()
        val requests = mutableListOf<String>()
        repository.latestMovies = { Result.Success(listOf(movie("shared"))) }
        repository.GenreMovies = { Result.Success(listOf(movie("shared"))) }
        repository.countryMovies = { country ->
            if (country == "han-quoc") pending.await() else Result.Error(IllegalStateException("offline"))
        }
        repository.movieDetail = { slug ->
            requests += slug
            Result.Success(DataMovieDetail(slug = slug, trailerUrl = if (slug == "slow") "https://youtu.be/abcdefghijk" else ""))
        }
        val viewModel = createViewModel(repository)
        runCurrent()
        assertTrue(requests.isEmpty())
        assertTrue(viewModel.homeData.value.htRecommendation.isLoading)
        pending.complete(Result.Success(listOf(movie("shared"), movie("slow"))))
        advanceUntilIdle()
        assertEquals("slow", viewModel.homeData.value.htRecommendation.movie?.slug)
        assertEquals(requests.distinct(), requests)
    }

    @Test fun recommendationUsesOneLatestMovieAndItsFavoriteButtonUpdatesThatSameMovie() = runTest {
        val repository = FakeMovieRepository()
        val detailRequests = mutableListOf<String>()
        repository.latestMovies = { Result.Success(listOf(movie("picked"))) }
        repository.movieDetail = { slug ->
            detailRequests += slug
            Result.Success(DataMovieDetail(slug = slug, name = "Picked movie", trailerUrl = "https://youtu.be/abcdefghijk"))
        }
        val storage = FakeLocalStorageRepository()
        val viewModel = createViewModel(repository, storage)
        advanceUntilIdle()
        assertEquals("picked", viewModel.homeData.value.htRecommendation.movie?.slug)
        assertEquals(false, viewModel.homeData.value.htRecommendation.isFavorite)
        viewModel.toggleRecommendationFavorite()
        advanceUntilIdle()
        assertEquals("picked", storage.favoriteToggles.single().slugMovie)
        assertEquals(true, viewModel.homeData.value.htRecommendation.isFavorite)
        repository.latestMovies = { Result.Success(listOf(movie("replacement"))) }
        viewModel.fetchMovieLastest()
        advanceUntilIdle()
        assertEquals(listOf("picked"), detailRequests)
        assertEquals("picked", viewModel.homeData.value.htRecommendation.movie?.slug)
    }

    private fun createViewModel(
        repository: FakeMovieRepository,
        storage: FakeLocalStorageRepository = FakeLocalStorageRepository(),
    ) = HomeViewModel(
        GetMoviesLatest(repository),
        GetMoviesByPage(repository),
        GetMoviesByGenres(repository),
        GetMoviesSeries(repository),
        GetMoviesByCountry(repository),
        GetMovieByGenreAndCountry(repository),
        GetMoviesPager(repository),
        GetMoviesByGenrePager(repository),
        GetMoviesSeriesPager(repository),
        GetMoviesBySearch(repository),
        GetSavedGenres(repository),
        GetCountries(repository),
        GetGenres(repository),
        HomeRecommendationSession(GetMovieDetail(repository), CoroutineScope(Dispatchers.Main)),
        CheckMovieExistsInFavorite(storage),
        ToggleFavoriteMovie(storage),
    )
}
