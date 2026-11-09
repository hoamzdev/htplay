package com.htdev.movie.htplay.viewmodel

import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.usecase.movie.GetListContinueWatching
import com.htdev.movie.htplay.home.home.viewmodel.ContinueWatchingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ContinueWatchingViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun observesSavedMoviesWithStableIdsAndConvertsProgressToSeconds() = runTest {
        val storage = FakeLocalStorageRepository()
        val viewModel = ContinueWatchingViewModel(GetListContinueWatching(storage))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.movies.collect {} }
        storage.movies.value = listOf(movie("film", position = 45_000))
        runCurrent()
        val first = viewModel.movies.value.single()
        assertEquals("film", first.slugMovie)
        assertEquals("thumbnail", first.urlThumbnail)
        assertEquals(45L, first.currentProgress)
        assertEquals(120L, first.totalProgress)
        storage.movies.value = listOf(movie("film", position = 60_000))
        runCurrent()
        assertEquals(first.id, viewModel.movies.value.single().id)
        assertEquals(60L, viewModel.movies.value.single().currentProgress)
        storage.movies.value = emptyList()
        runCurrent()
        assertTrue(viewModel.movies.value.isEmpty())
    }

    @Test fun showsOnlyFiveDistinctUnfinishedMoviesInStorageOrder() = runTest {
        val storage = FakeLocalStorageRepository().apply {
            movies.value = listOf(movie("done", 120_000), movie("unstarted", 0), movie("subsecond", 999), movie("unknown", 1, 0),
                movie(""), movie("film-1", 1_000), movie("film-1")) + (2..8).map { movie("film-$it") }
        }
        val viewModel = ContinueWatchingViewModel(GetListContinueWatching(storage))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.movies.collect {} }
        runCurrent()
        assertEquals((1..5).map { "film-$it" }, viewModel.movies.value.map { it.slugMovie })
    }

    private fun movie(slug: String, position: Long = 45_000, duration: Long = 120_000) = ContinueWatching(
        name = slug, slugMovie = slug, thumbnail = "thumbnail", genre = "hanh-dong", currentEpisode = 0,
        lengthCurrentEpisode = duration, positionContinue = position,
    )
}
