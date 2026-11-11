package com.htdev.movie.htplay.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.htdev.movie.htplay.detail.DetailUiState
import com.htdev.movie.htplay.detail.DetailViewModel
import com.htdev.movie.htplay.detail.EpisodeServer
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.domain.entities.DataMoviePlayer
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.repository.MovieRepository
import com.htdev.movie.htplay.domain.usecase.movie.GetDataMoviePlayer
import com.htdev.movie.htplay.domain.usecase.movie.GetMovieDetail
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByCountry
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByGenres
import com.htdev.movie.htplay.domain.usecase.movie.GetListContinueWatching
import com.htdev.movie.htplay.domain.usecase.movie.SaveContinueWatching
import com.htdev.movie.htplay.domain.usecase.movie.CheckMovieExistsInFavorite
import com.htdev.movie.htplay.domain.usecase.movie.ToggleFavoriteMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun loadsActualEpisodesAndTwentyOtherMoviesFromTheCurrentGenreOnlyOnce() = runTest {
        val repository = DetailRepository()
        val original = movie("current")
        repository.movies = {
            Result.Success(listOf(original, movie("other-1"), movie("other-1")) +
                (2..22).map { movie("other-$it") })
        }
        repository.player = {
            Result.Success(DataMoviePlayer(
                vietsub = listOf(MoviePlayer("tap-1", "sub-1")),
                longtieng = listOf(MoviePlayer("tap-1", "dub-1"), MoviePlayer("tap-2", "dub-2")),
            ))
        }
        val viewModel = viewModel(repository)
        viewModel.initialize(" current ")
        advanceUntilIdle()
        viewModel.initialize("current")
        advanceUntilIdle()

        assertEquals(listOf("current"), repository.detailRequests)
        assertEquals(listOf("current"), repository.playerRequests)
        assertEquals(listOf("hanh-dong" to 21), repository.genreRequests)
        assertEquals("Current movie", (viewModel.uiState.value.detail as Result.Success).data.name)
        assertEquals(listOf("tap-1", "tap-2"),
            (viewModel.uiState.value.episodes as Result.Success).data.map { it.id })
        val recommendations = (viewModel.uiState.value.moreLikeThis as Result.Success).data
        assertEquals(20, recommendations.size)
        assertEquals(20, recommendations.distinctBy { it.id }.size)
        assertFalse(recommendations.any { it.id == original.id || it.slug == original.slug })
    }

    @Test fun secondaryFailuresKeepTheLoadedMovieDetail() = runTest {
        val repository = DetailRepository().apply {
            player = { throw IllegalStateException("Episodes unavailable") }
            movies = { Result.Error(IllegalStateException("Recommendations unavailable")) }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.detail is Result.Success)
        assertTrue(viewModel.uiState.value.episodes is Result.Error)
        assertTrue(viewModel.uiState.value.moreLikeThis is Result.Error)
    }

    @Test fun tenEpisodesRemainTenAndUseDubbedLinkWhenSubtitledLinkIsMissing() = runTest {
        val repository = DetailRepository().apply {
            player = {
                Result.Success(DataMoviePlayer(
                    vietsub = (1..10).map { number ->
                        MoviePlayer("tap-$number", if (number == 1) "" else "https://example.com/$number.m3u8")
                    },
                    longtieng = listOf(MoviePlayer("tap-1", "https://example.com/dubbed-1.m3u8")),
                ))
            }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current")
        advanceUntilIdle()

        val episodes = (viewModel.uiState.value.episodes as Result.Success).data
        assertEquals((1..10).map { "tap-$it" }, episodes.map { it.id })
        assertEquals("https://example.com/dubbed-1.m3u8", episodes.first().linkM3u8)
        assertTrue(episodes.all { it.linkM3u8.isNotBlank() })
    }

    @Test fun failedDetailDoesNotRequestRecommendations() = runTest {
        val repository = DetailRepository().apply {
            detail = { Result.Error(IllegalStateException("Detail unavailable")) }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.detail is Result.Error)
        assertTrue(repository.genreRequests.isEmpty())
        assertTrue((viewModel.uiState.value.moreLikeThis as Result.Success).data.isEmpty())
    }

    @Test fun missingSlugDoesNotCallAnyEndpoint() = runTest {
        val repository = DetailRepository()
        val viewModel = viewModel(repository)
        viewModel.initialize(" ")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.detail is Result.Error)
        assertTrue(viewModel.uiState.value.episodes is Result.Error)
        assertTrue(repository.detailRequests.isEmpty())
        assertTrue(repository.playerRequests.isEmpty())
        assertTrue(repository.genreRequests.isEmpty())
    }

    @Test fun unknownGenreSkipsRecommendationsAndDubbedOnlyEpisodesStillLoad() = runTest {
        val repository = DetailRepository().apply {
            detail = { Result.Success(DataMovieDetail(slug = "current", genreSlug = "Unknown")) }
            player = { Result.Success(DataMoviePlayer(longtieng = listOf(MoviePlayer("full", "dub")))) }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current")
        advanceUntilIdle()

        assertTrue(repository.genreRequests.isEmpty())
        assertTrue((viewModel.uiState.value.moreLikeThis as Result.Success).data.isEmpty())
        assertEquals("full", (viewModel.uiState.value.episodes as Result.Success).data.single().id)
    }

    @Test fun countryRecommendationsUseCurrentCountryAndExcludeCurrentMovieAndDuplicates() = runTest {
        val repository = DetailRepository().apply {
            countryMovies = {
                Result.Success(listOf(movie("current"), movie("other-1"), movie("other-1")) +
                    (2..22).map { movie("other-$it") })
            }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current")
        advanceUntilIdle()

        assertEquals(listOf("han-quoc" to 41), repository.countryRequests)
        val movies = (viewModel.uiState.value.countryMovies as Result.Success).data
        assertEquals(20, movies.size)
        assertEquals(20, movies.distinctBy { it.id }.size)
        assertFalse(movies.any { it.slug == "current" })
    }

    @Test fun countryRecommendationsLoadWhileGenreRequestIsStillPending() = runTest {
        val genreResult = CompletableDeferred<Result<List<PosterMovie>>>()
        val repository = DetailRepository().apply {
            movies = { genreResult.await() }
            countryMovies = { Result.Success(listOf(movie("country-movie"))) }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current")
        runCurrent()

        assertTrue(viewModel.uiState.value.moreLikeThis is Result.Loading)
        assertEquals("country-movie", (viewModel.uiState.value.countryMovies as Result.Success).data.single().slug)
        genreResult.complete(Result.Error(IllegalStateException("Genre unavailable")))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.detail is Result.Success)
        assertTrue(viewModel.uiState.value.moreLikeThis is Result.Error)
        assertTrue(viewModel.uiState.value.countryMovies is Result.Success)
    }

    @Test fun recommendationSectionsNeverOverlapRegardlessOfRequestCompletionOrder() = runTest {
        for (countryFirst in listOf(false, true)) {
            val genreResult = CompletableDeferred<Result<List<PosterMovie>>>()
            val countryResult = CompletableDeferred<Result<List<PosterMovie>>>()
            val shared = (1..20).map { movie("shared-$it") }
            val countryCandidates = listOf(movie("current")) + shared.mapIndexed { index, movie ->
                when (index) {
                    0 -> movie.copy(slug = "different-slug") // Same ID still means the same film.
                    1 -> movie.copy(id = "different-id") // A shared slug also means the same film.
                    else -> movie
                }
            } + (1..20).map { movie("country-$it") }
            val repository = DetailRepository().apply {
                movies = { genreResult.await() }
                countryMovies = { countryResult.await() }
            }
            val viewModel = viewModel(repository)
            val states = mutableListOf<DetailUiState>()
            val observation = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { states.add(it) }
            }
            viewModel.initialize("current")
            runCurrent()
            if (countryFirst) countryResult.complete(Result.Success(countryCandidates))
            else genreResult.complete(Result.Success(shared))
            runCurrent()
            if (countryFirst) genreResult.complete(Result.Success(shared))
            else countryResult.complete(Result.Success(countryCandidates))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(shared, (state.moreLikeThis as Result.Success).data)
            assertEquals((1..20).map { "country-$it" },
                (state.countryMovies as Result.Success).data.map { it.slug })
            states.forEach { snapshot ->
                val genre = (snapshot.moreLikeThis as? Result.Success)?.data.orEmpty()
                val country = (snapshot.countryMovies as? Result.Success)?.data.orEmpty()
                assertTrue(genre.map { it.id }.intersect(country.map { it.id }.toSet()).isEmpty())
                assertTrue(genre.map { it.slug }.intersect(country.map { it.slug }.toSet()).isEmpty())
            }
            observation.cancel()
        }
    }

    @Test fun missingCountryDoesNotRequestCountryMovies() = runTest {
        val repository = DetailRepository().apply {
            detail = { Result.Success(DataMovieDetail(slug = "current", genreSlug = "hanh-dong")) }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current")
        advanceUntilIdle()

        assertTrue(repository.countryRequests.isEmpty())
        assertTrue((viewModel.uiState.value.countryMovies as Result.Success).data.isEmpty())
        assertEquals(listOf("hanh-dong" to 21), repository.genreRequests)
    }

    @Test fun opensMoviesInPlaceAndBackConsumesHistoryWithoutPushingAgain() = runTest {
        val repository = DetailRepository()
        val viewModel = viewModel(repository)
        viewModel.initialize("first")
        advanceUntilIdle()
        assertFalse(viewModel.goBack())
        assertTrue(viewModel.openMovie(" second "))
        advanceUntilIdle()
        assertTrue(viewModel.openMovie("third"))
        advanceUntilIdle()
        assertTrue(viewModel.goBack())
        advanceUntilIdle()
        assertTrue(viewModel.goBack())
        advanceUntilIdle()
        assertFalse(viewModel.goBack())

        assertEquals(listOf("first", "second", "third", "second", "first"), repository.detailRequests)
        assertEquals(repository.detailRequests, repository.playerRequests)
    }

    @Test fun backKeepsOnlyTheFiveMostRecentPreviousMovies() = runTest {
        val repository = DetailRepository()
        val viewModel = viewModel(repository)
        viewModel.initialize("movie-0")
        advanceUntilIdle()
        (1..7).forEach {
            assertTrue(viewModel.openMovie("movie-$it"))
            advanceUntilIdle()
        }
        repeat(5) {
            assertTrue(viewModel.goBack())
            advanceUntilIdle()
        }

        assertFalse(viewModel.goBack())
        assertEquals((6 downTo 2).map { "movie-$it" }, repository.detailRequests.takeLast(5))
    }

    @Test fun blankAndCurrentMovieDoNotReloadOrAddHistory() = runTest {
        val repository = DetailRepository()
        val viewModel = viewModel(repository)
        viewModel.initialize("first")
        advanceUntilIdle()

        assertFalse(viewModel.openMovie(" "))
        assertFalse(viewModel.openMovie(" first "))
        assertFalse(viewModel.goBack())
        advanceUntilIdle()
        assertEquals(listOf("first"), repository.detailRequests)
    }

    @Test fun openingAfterBackStartsANewHistoryBranch() = runTest {
        val repository = DetailRepository()
        val viewModel = viewModel(repository)
        viewModel.initialize("first")
        viewModel.openMovie("second")
        viewModel.openMovie("third")
        assertTrue(viewModel.goBack())
        viewModel.openMovie("fourth")
        advanceUntilIdle()
        assertTrue(viewModel.goBack())
        advanceUntilIdle()
        assertEquals("second", repository.detailRequests.last())
        assertTrue(viewModel.goBack())
        advanceUntilIdle()
        assertEquals("first", repository.detailRequests.last())
        assertFalse(viewModel.goBack())
    }

    @Test fun restoresCurrentMovieAndHistoryInsteadOfTheOriginalIntentMovie() = runTest {
        val savedState = SavedStateHandle()
        val repository = DetailRepository()
        val original = viewModel(repository, savedState)
        original.initialize("first")
        original.openMovie("second")
        advanceUntilIdle()
        val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) })
        val restored = viewModel(repository, restoredState)
        restored.initialize("first")
        advanceUntilIdle()

        assertEquals("second", repository.detailRequests.last())
        assertTrue(restored.goBack())
        advanceUntilIdle()
        assertEquals("first", repository.detailRequests.last())
        assertFalse(restored.goBack())
    }

    @Test fun switchingMoviesClearsOldResultsAndIgnoresLateResponsesFromCancelledLoads() = runTest {
        val oldDetail = CompletableDeferred<Result<DataMovieDetail>>()
        val oldEpisodes = CompletableDeferred<Result<DataMoviePlayer>>()
        val repository = DetailRepository().apply {
            detail = { withContext(NonCancellable) { oldDetail.await() } }
            player = { withContext(NonCancellable) { oldEpisodes.await() } }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("first")
        runCurrent()
        repository.detail = { Result.Success(DataMovieDetail(slug = "second", name = "Second")) }
        repository.player = { Result.Success(DataMoviePlayer(vietsub = listOf(MoviePlayer("second", "new-link")))) }
        viewModel.openMovie("second")
        assertTrue(viewModel.uiState.value.detail is Result.Loading)
        assertTrue(viewModel.uiState.value.episodes is Result.Loading)
        assertTrue(viewModel.uiState.value.moreLikeThis is Result.Loading)
        assertTrue(viewModel.uiState.value.countryMovies is Result.Loading)
        runCurrent()
        oldDetail.complete(Result.Success(DataMovieDetail(slug = "first", name = "First")))
        oldEpisodes.complete(Result.Success(DataMoviePlayer(vietsub = listOf(MoviePlayer("first", "old-link")))))
        advanceUntilIdle()

        assertEquals("second", (viewModel.uiState.value.detail as Result.Success).data.slug)
        assertEquals("second", (viewModel.uiState.value.episodes as Result.Success).data.single().id)
        assertTrue(repository.genreRequests.isEmpty())
        assertTrue(repository.countryRequests.isEmpty())
    }

    @Test fun switchingClearsLoadedDataAndCancelsPendingRecommendations() = runTest {
        val oldGenre = CompletableDeferred<Result<List<PosterMovie>>>()
        val oldCountry = CompletableDeferred<Result<List<PosterMovie>>>()
        val repository = DetailRepository().apply {
            movies = { withContext(NonCancellable) { oldGenre.await() } }
            countryMovies = { withContext(NonCancellable) { oldCountry.await() } }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("first")
        runCurrent()
        assertTrue(viewModel.uiState.value.detail is Result.Success)
        assertTrue(viewModel.uiState.value.episodes is Result.Success)
        repository.movies = { Result.Success(listOf(movie("new-genre"))) }
        repository.countryMovies = { Result.Success(listOf(movie("new-country"))) }
        viewModel.openMovie("second")
        assertTrue(viewModel.uiState.value.detail is Result.Loading)
        assertTrue(viewModel.uiState.value.episodes is Result.Loading)
        runCurrent()
        oldGenre.complete(Result.Success(listOf(movie("old-genre"))))
        oldCountry.complete(Result.Success(listOf(movie("old-country"))))
        advanceUntilIdle()

        assertEquals("new-genre", (viewModel.uiState.value.moreLikeThis as Result.Success).data.single().slug)
        assertEquals("new-country", (viewModel.uiState.value.countryMovies as Result.Success).data.single().slug)
    }

    @Test fun selectingAnotherMovieChangesTheDisplayedDetailAndVideoAndBackRestoresBoth() = runTest {
        val repository = DetailRepository().apply {
            player = { slug ->
                // Episode IDs are shared between movies in the real API.
                Result.Success(DataMoviePlayer(vietsub = listOf(MoviePlayer("tap-1", "$slug.m3u8"))))
            }
        }
        val viewModel = viewModel(repository)
        val states = mutableListOf<DetailUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }
        viewModel.initialize("first")
        advanceUntilIdle()
        viewModel.openMovie("second")
        advanceUntilIdle()

        assertEquals("second", viewModel.uiState.value.movieSlug)
        assertEquals("second", (viewModel.uiState.value.detail as Result.Success).data.name)
        assertEquals("second.m3u8", (viewModel.uiState.value.episodes as Result.Success).data.single().linkM3u8)
        assertTrue(viewModel.goBack())
        advanceUntilIdle()
        assertEquals("first", (viewModel.uiState.value.detail as Result.Success).data.name)
        assertEquals("first.m3u8", (viewModel.uiState.value.episodes as Result.Success).data.single().linkM3u8)
        states.forEach { state ->
            (state.detail as? Result.Success)?.let { assertEquals(state.movieSlug, it.data.slug) }
            (state.episodes as? Result.Success)?.let {
                assertEquals("${state.movieSlug}.m3u8", it.data.single().linkM3u8)
            }
        }
    }

    @Test fun fastLoadsStillPublishTheSelectedMovieWhenTheUiSkipsLoadingStates() = runTest {
        val repository = DetailRepository()
        val viewModel = viewModel(repository)
        viewModel.initialize("first")
        advanceUntilIdle()
        val previous = viewModel.uiState.value
        viewModel.openMovie("second")
        advanceUntilIdle()
        // A resumed UI can read the completed snapshot without ever observing Loading.
        val current = viewModel.uiState.value

        assertNotEquals(previous.movieSlug, current.movieSlug)
        assertEquals("second", (current.detail as Result.Success).data.slug)
        assertTrue(current.episodes is Result.Success)
        assertTrue(current.moreLikeThis is Result.Success)
        assertTrue(current.countryMovies is Result.Success)
    }

    @Test fun savesThePlayingEpisodeWithMetadataAndKeepsWritingAfterDetailIsClosed() = runTest {
        val storage = FakeLocalStorageRepository()
        val episode = MoviePlayer("tap-2", "second-link")
        val repository = DetailRepository().apply {
            player = { Result.Success(DataMoviePlayer(vietsub = listOf(MoviePlayer("tap-1", "first-link"), episode))) }
        }
        val viewModel = viewModel(repository, storage = storage, saveScope = backgroundScope)
        val store = ViewModelStore().apply { put("detail", viewModel) }
        viewModel.initialize("current")
        advanceUntilIdle()
        viewModel.savePlaybackProgress(episode, 45_000, 120_000)
        store.clear()
        runCurrent()

        val saved = storage.saved.single()
        assertEquals("current", saved.slugMovie)
        assertEquals("Current movie", saved.name)
        assertEquals(1, saved.currentEpisode)
        assertEquals(45_000L, saved.positionContinue)
        assertEquals(120_000L, saved.lengthCurrentEpisode)
    }

    @Test fun resumeProgressBelongsToTheSelectedMovieAndLoadsBeforeEpisodesAreDisplayed() = runTest {
        val storage = FakeLocalStorageRepository().apply {
            movies.value = listOf(ContinueWatching(name = "Saved", slugMovie = "current", genre = "hanh-dong",
                currentEpisode = 1, lengthCurrentEpisode = 120_000, positionContinue = 45_000))
        }
        val repository = DetailRepository().apply {
            player = { Result.Success(DataMoviePlayer(vietsub = listOf(MoviePlayer("tap-1", "one"), MoviePlayer("tap-2", "two")))) }
        }
        val viewModel = viewModel(repository, storage = storage)
        viewModel.initialize("current")
        advanceUntilIdle()

        assertEquals(45_000L, viewModel.uiState.value.continueWatching?.positionContinue)
        assertEquals(1, viewModel.uiState.value.continueWatching?.currentEpisode)
        assertTrue(viewModel.uiState.value.episodes is Result.Success)
        viewModel.openMovie("other")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.continueWatching)
    }

    @Test fun progressSavesAreOrderedAndRejectInvalidOrStaleEpisodes() = runTest {
        val firstWrite = CompletableDeferred<Unit>()
        val storage = FakeLocalStorageRepository().apply {
            save = { movie ->
                if (movie.positionContinue == 10_000L) firstWrite.await()
                saved.add(movie)
            }
        }
        val episode = MoviePlayer("tap-1", "link")
        val repository = DetailRepository().apply {
            player = { Result.Success(DataMoviePlayer(vietsub = listOf(episode))) }
        }
        val viewModel = viewModel(repository, storage = storage, saveScope = backgroundScope)
        viewModel.initialize("current")
        advanceUntilIdle()
        assertNull(viewModel.savePlaybackProgress(episode, 0, 100_000))
        assertNull(viewModel.savePlaybackProgress(episode, 10_000, -1))
        assertNull(viewModel.savePlaybackProgress(MoviePlayer("tap-1", "old-link"), 10_000, 100_000))
        viewModel.savePlaybackProgress(episode, 10_000, 100_000)
        viewModel.savePlaybackProgress(episode, 20_000, 100_000)
        viewModel.savePlaybackProgress(episode, 20_000, 100_000)
        runCurrent()
        assertTrue(storage.saved.isEmpty())
        firstWrite.complete(Unit)
        runCurrent()
        assertEquals(listOf(10_000L, 20_000L), storage.saved.map { it.positionContinue })
    }

    @Test fun progressIsSavedOnlyAfterAtLeastOneWholeSecond() = runTest {
        val storage = FakeLocalStorageRepository()
        val episode = MoviePlayer("tap-1", "link")
        val repository = DetailRepository().apply {
            player = { Result.Success(DataMoviePlayer(vietsub = listOf(episode))) }
        }
        val viewModel = viewModel(repository, storage = storage, saveScope = backgroundScope)
        viewModel.initialize("current")
        advanceUntilIdle()

        listOf(-1L, 0L, 1L, 999L).forEach { position ->
            assertNull(viewModel.savePlaybackProgress(episode, position, 100_000))
        }
        runCurrent()
        assertTrue(storage.saved.isEmpty())

        viewModel.savePlaybackProgress(episode, 1_000, 100_000)
        runCurrent()
        assertEquals(1_000L, storage.saved.single().positionContinue)
    }

    @Test fun favoriteTracksDatabaseChangesAndTheMovieSelectedThroughBack() = runTest {
        val storage = FakeLocalStorageRepository().apply {
            favorites.value = listOf(Favorite(name = "First", slugMovie = "first", genre = ""))
        }
        val viewModel = viewModel(DetailRepository(), storage = storage)
        viewModel.initialize("first")
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isMovieFavorite)

        viewModel.openMovie("second")
        assertNull(viewModel.uiState.value.isMovieFavorite)
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isMovieFavorite)
        storage.favorites.value = emptyList()
        advanceUntilIdle()
        assertTrue(viewModel.goBack())
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isMovieFavorite)

        storage.favorites.value = listOf(Favorite(name = "First", slugMovie = "first", genre = ""))
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isMovieFavorite)
    }

    @Test fun favoriteTogglesBySlugAndNeverWritesWhileNextMovieIsLoading() = runTest {
        val storage = FakeLocalStorageRepository()
        val viewModel = viewModel(DetailRepository(), storage = storage)
        viewModel.initialize("current")
        advanceUntilIdle()
        viewModel.toggleFavorite()
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertEquals(1, storage.favoriteToggles.size)
        assertEquals("current", storage.favorites.value.single().slugMovie)
        assertEquals(true, viewModel.uiState.value.isMovieFavorite)
        assertEquals(Result.Success(true), viewModel.uiState.value.favoriteChange)
        viewModel.clearFavoriteChange()
        assertEquals(Result.Idle, viewModel.uiState.value.favoriteChange)

        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue(storage.favorites.value.isEmpty())
        assertEquals(2, storage.favoriteToggles.size)
        assertEquals(false, viewModel.uiState.value.isMovieFavorite)
        assertEquals(Result.Success(false), viewModel.uiState.value.favoriteChange)

        viewModel.openMovie("next")
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertEquals(2, storage.favoriteToggles.size)
        assertEquals(false, viewModel.uiState.value.isMovieFavorite)
    }

    @Test fun favoriteDoesNotChangeOrReportSuccessWhenTheWriteFails() = runTest {
        val storage = FakeLocalStorageRepository().apply {
            toggle = { throw IllegalStateException("Database unavailable") }
        }
        val viewModel = viewModel(DetailRepository(), storage = storage)
        viewModel.initialize("current")
        advanceUntilIdle()
        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isMovieFavorite)
        assertTrue(viewModel.uiState.value.favoriteChange is Result.Error)
        assertFalse(viewModel.uiState.value.isUpdatingFavorite)
        assertTrue(storage.favorites.value.isEmpty())
    }

    @Test fun pendingFavoriteWriteCannotChangeTheNextMoviesIconOrShowAnOldSuccess() = runTest {
        val write = CompletableDeferred<Unit>()
        val storage = FakeLocalStorageRepository()
        val originalToggle = storage.toggle
        storage.toggle = { favorite ->
            write.await()
            originalToggle(favorite)
        }
        val viewModel = viewModel(DetailRepository(), storage = storage)
        viewModel.initialize("first")
        advanceUntilIdle()
        viewModel.toggleFavorite()
        runCurrent()
        assertTrue(viewModel.uiState.value.isUpdatingFavorite)
        assertEquals(false, viewModel.uiState.value.isMovieFavorite)
        viewModel.openMovie("second")
        runCurrent()
        assertTrue(viewModel.uiState.value.isUpdatingFavorite)
        viewModel.toggleFavorite()
        write.complete(Unit)
        advanceUntilIdle()

        assertEquals("first", storage.favoriteToggles.single().slugMovie)
        assertEquals("first", storage.favorites.value.single().slugMovie)
        assertEquals(false, viewModel.uiState.value.isMovieFavorite)
        assertFalse(viewModel.uiState.value.isUpdatingFavorite)
        assertEquals(Result.Idle, viewModel.uiState.value.favoriteChange)
        assertTrue(viewModel.goBack())
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isMovieFavorite)
    }

    @Test fun vietnameseCanSelectEachServerWithoutMixingTheirEpisodeLinks() = runTest {
        val original = listOf(MoviePlayer("tap-1", "original-1"), MoviePlayer("tap-2", ""))
        val dubbed = listOf(MoviePlayer("tap-1", "dubbed-1"), MoviePlayer("tap-2", "dubbed-2"))
        val repository = DetailRepository().apply {
            player = { Result.Success(DataMoviePlayer(vietsub = original, longtieng = dubbed)) }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current", "vi-VN")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showServerTabs)
        assertEquals(EpisodeServer.ORIGINAL, viewModel.uiState.value.selectedServer)
        assertEquals(original, (viewModel.uiState.value.episodes as Result.Success).data)
        viewModel.selectEpisodeServer(EpisodeServer.DUBBED)
        assertEquals(dubbed, (viewModel.uiState.value.episodes as Result.Success).data)
        assertEquals(EpisodeServer.DUBBED, viewModel.uiState.value.selectedServer)
        viewModel.selectEpisodeServer(EpisodeServer.ORIGINAL)
        assertEquals(original, (viewModel.uiState.value.episodes as Result.Success).data)
        assertEquals(listOf("current"), repository.playerRequests)
    }

    @Test fun vietnameseHidesBothServerTabsWhenDubbedEpisodesAreMissingOrUnplayable() = runTest {
        for (dubbed in listOf(emptyList(), listOf(MoviePlayer("tap-1", "  ")))) {
            val original = listOf(MoviePlayer("tap-1", "original"))
            val repository = DetailRepository().apply {
                player = { Result.Success(DataMoviePlayer(vietsub = original, longtieng = dubbed)) }
            }
            val viewModel = viewModel(repository)
            viewModel.initialize("current", "vi")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showServerTabs)
            viewModel.selectEpisodeServer(EpisodeServer.DUBBED)
            assertEquals(EpisodeServer.ORIGINAL, viewModel.uiState.value.selectedServer)
            assertEquals(original, (viewModel.uiState.value.episodes as Result.Success).data)
        }
    }

    @Test fun otherLanguagesKeepTheExistingEpisodeFallbackAndNeverShowServerTabs() = runTest {
        for (language in listOf("en", "ko", "fr", "zh")) {
            val original = MoviePlayer("tap-1", "original")
            val dubbed = MoviePlayer("tap-2", "dubbed")
            val repository = DetailRepository().apply {
                player = {
                    Result.Success(DataMoviePlayer(
                        vietsub = listOf(original, MoviePlayer("tap-2", "")),
                        longtieng = listOf(MoviePlayer("tap-1", "dubbed-1"), dubbed),
                    ))
                }
            }
            val viewModel = viewModel(repository)
            viewModel.initialize("current", language)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showServerTabs)
            viewModel.selectEpisodeServer(EpisodeServer.DUBBED)
            assertEquals(listOf(original, dubbed), (viewModel.uiState.value.episodes as Result.Success).data)
        }
    }

    @Test fun selectedServerIsRestoredButResetsWhenAnotherMovieIsOpened() = runTest {
        val savedState = SavedStateHandle()
        val repository = DetailRepository().apply {
            player = { slug ->
                Result.Success(DataMoviePlayer(
                    vietsub = listOf(MoviePlayer("tap-1", "$slug-original")),
                    longtieng = listOf(MoviePlayer("tap-1", "$slug-dubbed")),
                ))
            }
        }
        val original = viewModel(repository, savedState)
        original.initialize("first", "vi")
        advanceUntilIdle()
        original.selectEpisodeServer(EpisodeServer.DUBBED)
        val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) })
        val restored = viewModel(repository, restoredState)
        restored.initialize("first", "vi")
        advanceUntilIdle()

        assertEquals(EpisodeServer.DUBBED, restored.uiState.value.selectedServer)
        assertEquals("first-dubbed", (restored.uiState.value.episodes as Result.Success).data.single().linkM3u8)
        restored.openMovie("second")
        assertFalse(restored.uiState.value.showServerTabs)
        advanceUntilIdle()
        assertEquals(EpisodeServer.ORIGINAL, restored.uiState.value.selectedServer)
        assertEquals("second-original", (restored.uiState.value.episodes as Result.Success).data.single().linkM3u8)
    }

    @Test fun vietnameseUsesDubbedWhenItIsTheOnlyPlayableServer() = runTest {
        val dubbed = listOf(MoviePlayer("full", "dubbed"))
        val repository = DetailRepository().apply {
            player = { Result.Success(DataMoviePlayer(longtieng = dubbed)) }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current", "vi")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showServerTabs)
        assertEquals(EpisodeServer.DUBBED, viewModel.uiState.value.selectedServer)
        viewModel.selectEpisodeServer(EpisodeServer.ORIGINAL)
        assertEquals(dubbed, (viewModel.uiState.value.episodes as Result.Success).data)
        assertEquals(EpisodeServer.DUBBED, viewModel.uiState.value.selectedServer)
    }

    private fun viewModel(
        repository: MovieRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        storage: FakeLocalStorageRepository = FakeLocalStorageRepository(),
        saveScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    ) = DetailViewModel(
        GetMovieDetail(repository), GetDataMoviePlayer(repository), GetMoviesByGenres(repository),
        GetMoviesByCountry(repository), savedStateHandle, GetListContinueWatching(storage),
        SaveContinueWatching(storage), ToggleFavoriteMovie(storage), CheckMovieExistsInFavorite(storage), saveScope,
    )

    private fun movie(slug: String) = PosterMovie(slug, "poster", slug, slug, "", "thumb")

    private class DetailRepository : MovieRepository by FakeMovieRepository() {
        val detailRequests = mutableListOf<String>()
        val playerRequests = mutableListOf<String>()
        val genreRequests = mutableListOf<Pair<String, Int>>()
        val countryRequests = mutableListOf<Pair<String, Int>>()
        var detail: suspend (String) -> Result<DataMovieDetail> = { slug ->
            Result.Success(DataMovieDetail(id = slug, slug = slug, name = if (slug == "current") "Current movie" else slug,
                genreSlug = "hanh-dong", genre = "Action", countrySlug = "han-quoc"))
        }
        var player: suspend (String) -> Result<DataMoviePlayer> = { Result.Success(DataMoviePlayer()) }
        var movies: suspend () -> Result<List<PosterMovie>> = { Result.Success(emptyList()) }
        var countryMovies: suspend () -> Result<List<PosterMovie>> = { Result.Success(emptyList()) }

        override suspend fun getMovieDetail(slug: String): Result<DataMovieDetail> {
            detailRequests += slug
            return detail(slug)
        }

        override suspend fun getDataMoviePlayer(slug: String): Result<DataMoviePlayer> {
            playerRequests += slug
            return player(slug)
        }

        override suspend fun getMoviesByGenre(Genre: String, limit: Int): Result<List<PosterMovie>> {
            genreRequests += Genre to limit
            return movies()
        }

        override suspend fun getMoviesByCountry(countrySlug: String, limit: Int): Result<List<PosterMovie>> {
            countryRequests += countrySlug to limit
            return countryMovies()
        }
    }
}
