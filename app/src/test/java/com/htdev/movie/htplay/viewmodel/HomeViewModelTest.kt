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
        val unfinished = ContinueWatching(totalProgress = 100, currentProgress = 40)
        viewModel.updateContinueWatching(listOf(unfinished, unfinished,
            ContinueWatching(totalProgress = 100, currentProgress = 100),
            ContinueWatching(totalProgress = 100, currentProgress = 0),
            ContinueWatching(currentProgress = 40)))

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

    private fun createViewModel(repository: FakeMovieRepository) = HomeViewModel(
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
    )
}
