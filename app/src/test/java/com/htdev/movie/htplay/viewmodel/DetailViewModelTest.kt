package com.htdev.movie.htplay.viewmodel

import com.htdev.movie.htplay.detail.DetailViewModel
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.DataMoviePlayer
import com.htdev.movie.htplay.domain.entities.MoviePlayer
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.repository.MovieRepository
import com.htdev.movie.htplay.domain.usecase.movie.GetDataMoviePlayer
import com.htdev.movie.htplay.domain.usecase.movie.GetMovieDetail
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByCountry
import com.htdev.movie.htplay.domain.usecase.movie.GetMoviesByGenres
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
        assertEquals("Current movie", (viewModel.detail.value as Result.Success).data.name)
        assertEquals(listOf("tap-1", "tap-2"),
            (viewModel.episodes.value as Result.Success).data.map { it.id })
        val recommendations = (viewModel.moreLikeThis.value as Result.Success).data
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

        assertTrue(viewModel.detail.value is Result.Success)
        assertTrue(viewModel.episodes.value is Result.Error)
        assertTrue(viewModel.moreLikeThis.value is Result.Error)
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

        val episodes = (viewModel.episodes.value as Result.Success).data
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

        assertTrue(viewModel.detail.value is Result.Error)
        assertTrue(repository.genreRequests.isEmpty())
        assertTrue((viewModel.moreLikeThis.value as Result.Success).data.isEmpty())
    }

    @Test fun missingSlugDoesNotCallAnyEndpoint() = runTest {
        val repository = DetailRepository()
        val viewModel = viewModel(repository)
        viewModel.initialize(" ")
        advanceUntilIdle()

        assertTrue(viewModel.detail.value is Result.Error)
        assertTrue(viewModel.episodes.value is Result.Error)
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
        assertTrue((viewModel.moreLikeThis.value as Result.Success).data.isEmpty())
        assertEquals("full", (viewModel.episodes.value as Result.Success).data.single().id)
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

        assertEquals(listOf("han-quoc" to 21), repository.countryRequests)
        val movies = (viewModel.countryMovies.value as Result.Success).data
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

        assertTrue(viewModel.moreLikeThis.value is Result.Loading)
        assertEquals("country-movie", (viewModel.countryMovies.value as Result.Success).data.single().slug)
        genreResult.complete(Result.Error(IllegalStateException("Genre unavailable")))
        advanceUntilIdle()
        assertTrue(viewModel.detail.value is Result.Success)
        assertTrue(viewModel.moreLikeThis.value is Result.Error)
        assertTrue(viewModel.countryMovies.value is Result.Success)
    }

    @Test fun missingCountryDoesNotRequestCountryMovies() = runTest {
        val repository = DetailRepository().apply {
            detail = { Result.Success(DataMovieDetail(slug = "current", genreSlug = "hanh-dong")) }
        }
        val viewModel = viewModel(repository)
        viewModel.initialize("current")
        advanceUntilIdle()

        assertTrue(repository.countryRequests.isEmpty())
        assertTrue((viewModel.countryMovies.value as Result.Success).data.isEmpty())
        assertEquals(listOf("hanh-dong" to 21), repository.genreRequests)
    }

    private fun viewModel(repository: MovieRepository) = DetailViewModel(
        GetMovieDetail(repository), GetDataMoviePlayer(repository), GetMoviesByGenres(repository),
        GetMoviesByCountry(repository),
    )

    private fun movie(slug: String) = PosterMovie(slug, "poster", slug, slug, "", "thumb")

    private class DetailRepository : MovieRepository by FakeMovieRepository() {
        val detailRequests = mutableListOf<String>()
        val playerRequests = mutableListOf<String>()
        val genreRequests = mutableListOf<Pair<String, Int>>()
        val countryRequests = mutableListOf<Pair<String, Int>>()
        var detail: suspend () -> Result<DataMovieDetail> = {
            Result.Success(DataMovieDetail(id = "current", slug = "current", name = "Current movie",
                genreSlug = "hanh-dong", genre = "Action", countrySlug = "han-quoc"))
        }
        var player: suspend () -> Result<DataMoviePlayer> = { Result.Success(DataMoviePlayer()) }
        var movies: suspend () -> Result<List<PosterMovie>> = { Result.Success(emptyList()) }
        var countryMovies: suspend () -> Result<List<PosterMovie>> = { Result.Success(emptyList()) }

        override suspend fun getMovieDetail(slug: String): Result<DataMovieDetail> {
            detailRequests += slug
            return detail()
        }

        override suspend fun getDataMoviePlayer(slug: String): Result<DataMoviePlayer> {
            playerRequests += slug
            return player()
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
