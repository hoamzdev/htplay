package com.htdev.movie.htplay.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetCountries
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesByCountryPager
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesByGenrePager
import com.htdev.movie.htplay.domain.usecase.movie.pager.GetMoviesPager
import com.htdev.movie.htplay.entities.HomeViewAllTarget
import com.htdev.movie.htplay.home.home.viewmodel.ShowAllViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShowAllViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun returningFromDetailKeepsLoadedPagesAfterSubscriptionTimeout() = runTest {
        val repository = FakeMovieRepository()
        var loads = 0
        repository.latestPager = {
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                object : PagingSource<Int, PosterMovie>() {
                    override fun getRefreshKey(state: PagingState<Int, PosterMovie>): Int? = null
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PosterMovie> {
                        loads++
                        return LoadResult.Page(listOf(movie("cached")), null, null)
                    }
                }
            }.flow
        }
        val viewModel = createViewModel(repository)
        viewModel.initialize(HomeViewAllTarget.Latest)
        val first = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.movies.collectLatest { presenter().collectFrom(it.movies) }
        }
        runCurrent()
        assertEquals(1, loads)
        first.cancel()
        runCurrent()
        advanceTimeBy(60_000)
        val returned = presenter()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.movies.collectLatest { returned.collectFrom(it.movies) }
        }
        runCurrent()
        assertEquals(listOf("cached"), returned.snapshot().items.map { it.slug })
        assertEquals(1, loads)
    }

    @Test fun recommendationsUseOnlyFavoriteTabsAndCancelPreviousPager() = runTest {
        val repository = FakeMovieRepository()
        val requested = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        repository.genrePager = { slug -> flow {
            requested += slug
            try {
                emit(PagingData.from(listOf(movie(slug))))
                awaitCancellation()
            } finally { cancelled += slug }
        } }
        val viewModel = createViewModel(repository)
        viewModel.initialize(HomeViewAllTarget.Recommended(listOf("Action", "tinh-cam", "hanh-dong", " ")))
        val presenter = presenter()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.movies.collectLatest { presenter.collectFrom(it.movies) }
        }
        runCurrent()
        assertEquals(listOf("hanh-dong", "tinh-cam"), viewModel.genreSlugs)
        assertEquals("hanh-dong", presenter.snapshot().items.single().slug)

        viewModel.selectGenre("tinh-cam")
        runCurrent()
        assertEquals(listOf("hanh-dong"), cancelled)
        assertEquals("tinh-cam", presenter.snapshot().items.single().slug)
        viewModel.selectGenre("tinh-cam")
        viewModel.selectGenre("not-a-favorite")
        runCurrent()
        assertEquals(listOf("hanh-dong", "tinh-cam"), requested)
        assertFalse(viewModel.isCurrentPage(HomeViewAllTarget.ByGenre("hanh-dong")))
    }

    @Test fun switchingTabsClearsOldMoviesAndStaysLoadingUntilNewPageArrives() = runTest {
        val repository = FakeMovieRepository()
        val nextPage = CompletableDeferred<PagingSource.LoadResult<Int, PosterMovie>>()
        repository.genrePager = { slug ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                object : PagingSource<Int, PosterMovie>() {
                    override fun getRefreshKey(state: PagingState<Int, PosterMovie>): Int? = null
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PosterMovie> =
                        if (slug == "hanh-dong") LoadResult.Page(listOf(movie(slug)), null, null)
                        else nextPage.await()
                }
            }.flow
        }
        val viewModel = createViewModel(repository)
        viewModel.initialize(HomeViewAllTarget.Recommended(listOf("hanh-dong", "tinh-cam")))
        val presenter = presenter()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.movies.collectLatest { presenter.collectFrom(it.movies) }
        }
        runCurrent()
        assertEquals("hanh-dong", presenter.snapshot().items.single().slug)
        viewModel.selectGenre("tinh-cam")
        runCurrent()
        assertTrue(presenter.snapshot().items.isEmpty())
        assertEquals(LoadState.Loading, presenter.loadStateFlow.value?.refresh)
        nextPage.complete(PagingSource.LoadResult.Page(emptyList(), null, null))
        runCurrent()
        assertTrue(presenter.loadStateFlow.value?.refresh is LoadState.NotLoading)
        assertEquals(true, presenter.loadStateFlow.value?.append?.endOfPaginationReached)
    }

    @Test fun latestGenreAndCountryScreensUseTheirOwnPagerOnly() = runTest {
        val repository = FakeMovieRepository()
        val requested = mutableListOf<String>()
        repository.latestPager = { flow { requested += "latest"; emit(PagingData.empty()) } }
        repository.genrePager = { slug -> flow { requested += "genre:$slug"; emit(PagingData.empty()) } }
        repository.countryPager = { slug -> flow { requested += "country:$slug"; emit(PagingData.empty()) } }
        repository.countries = { error("Country catalog should not load on a movie screen") }
        val targets = listOf(HomeViewAllTarget.Latest, HomeViewAllTarget.ByGenre("gia-dinh"),
            HomeViewAllTarget.ByCountry("han-quoc"))
        targets.forEach { target ->
            val viewModel = createViewModel(repository)
            viewModel.initialize(target)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.movies.collectLatest { presenter().collectFrom(it.movies) }
            }
        }
        runCurrent()
        assertEquals(listOf("latest", "genre:gia-dinh", "country:han-quoc"), requested)
    }

    @Test fun countryMoviesLoadMoreAndRetryAFailedNextPage() = runTest {
        val repository = FakeMovieRepository()
        val pages = mutableListOf<Int>()
        var failNextPage = true
        repository.countryPager = {
            Pager(PagingConfig(pageSize = 1, initialLoadSize = 1, prefetchDistance = 1, enablePlaceholders = false)) {
                object : PagingSource<Int, PosterMovie>() {
                    override fun getRefreshKey(state: PagingState<Int, PosterMovie>): Int? = null
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PosterMovie> {
                        val page = params.key ?: 1
                        pages += page
                        return if (page == 2 && failNextPage) LoadResult.Error(IllegalStateException("offline"))
                        else LoadResult.Page(listOf(movie("page-$page")), null, if (page == 1) 2 else null)
                    }
                }
            }.flow
        }
        val viewModel = createViewModel(repository)
        viewModel.initialize(HomeViewAllTarget.ByCountry("trung-quoc"))
        val presenter = presenter()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.movies.collectLatest { presenter.collectFrom(it.movies) }
        }
        runCurrent()
        presenter[0]
        runCurrent()
        assertTrue(presenter.loadStateFlow.value?.append is LoadState.Error)
        assertEquals(listOf("page-1"), presenter.snapshot().items.map { it.slug })
        failNextPage = false
        presenter.retry()
        runCurrent()
        assertEquals(listOf(1, 2, 2), pages)
        assertEquals(listOf("page-1", "page-2"), presenter.snapshot().items.map { it.slug })
    }

    @Test fun countriesCanRetryAndDoNotStartAMoviePager() = runTest {
        val repository = FakeMovieRepository()
        repository.latestPager = { error("Unexpected latest pager") }
        repository.genrePager = { error("Unexpected genre pager") }
        repository.countryPager = { error("Unexpected country pager") }
        repository.countries = { Result.Error(IllegalStateException("offline")) }
        val viewModel = createViewModel(repository)
        viewModel.initialize(HomeViewAllTarget.Countries)
        advanceUntilIdle()
        assertTrue(viewModel.countries.value is Result.Error)
        val countries = listOf(Country("Japan", "nhat-ban"))
        repository.countries = { Result.Success(countries) }
        viewModel.fetchCountries()
        advanceUntilIdle()
        assertEquals(Result.Success(countries), viewModel.countries.value)
    }

    @Test fun continueAndEmptyRecommendationsDoNotLoadData() = runTest {
        val repository = FakeMovieRepository()
        repository.latestPager = { error("Unexpected latest pager") }
        repository.genrePager = { error("Unexpected genre pager") }
        repository.countryPager = { error("Unexpected country pager") }
        repository.countries = { error("Unexpected country catalog") }
        listOf(HomeViewAllTarget.ContinueWatching, HomeViewAllTarget.Recommended(emptyList())).forEach { target ->
            val viewModel = createViewModel(repository)
            viewModel.initialize(target)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.movies.collectLatest { error("Unexpected movie page") }
            }
            assertTrue(viewModel.genreSlugs.isEmpty())
            assertNull(viewModel.selectedGenre.value)
        }
        runCurrent()
    }

    @Test fun selectedFavoriteSurvivesRecreationAndInvalidSavedTabFallsBackToFirst() = runTest {
        val repository = FakeMovieRepository()
        val state = SavedStateHandle()
        val target = HomeViewAllTarget.Recommended(listOf("hanh-dong", "tinh-cam"))
        val viewModel = createViewModel(repository, state)
        viewModel.initialize(target)
        viewModel.selectGenre("tinh-cam")
        val recreated = createViewModel(repository, state)
        recreated.initialize(target)
        assertEquals("tinh-cam", recreated.selectedGenre.value)
        recreated.initialize(target)
        assertEquals("tinh-cam", recreated.selectedGenre.value)
        val changedFavorites = createViewModel(repository, state)
        changedFavorites.initialize(HomeViewAllTarget.Recommended(listOf("gia-dinh")))
        assertEquals("gia-dinh", changedFavorites.selectedGenre.value)
        runCurrent()
    }

    private fun presenter() = object : PagingDataPresenter<PosterMovie>() {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<PosterMovie>) = Unit
    }
    private fun movie(slug: String) = PosterMovie(slug, "poster", slug, slug, "Episode 1")
    private fun createViewModel(repository: FakeMovieRepository, state: SavedStateHandle = SavedStateHandle()) =
        ShowAllViewModel(GetMoviesPager(repository), GetMoviesByGenrePager(repository),
            GetMoviesByCountryPager(repository), GetCountries(repository), state)
}
