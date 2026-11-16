package com.htdev.movie.htplay.viewmodel

import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.repository.MovieRepository
import com.htdev.movie.htplay.domain.usecase.movie.GetMovieDetail
import com.htdev.movie.htplay.home.home.HomeRecommendationSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRecommendationSessionTest {
    @Test fun oneSessionChoosesOnlyOnceEvenWhenHomeIsRecreatedOrLatestMoviesRefresh() = runTest {
        val requests = mutableListOf<String>()
        val session = session(backgroundScope) { slug ->
            requests += slug
            Result.Success(DataMovieDetail(slug = slug, trailerUrl = "https://youtu.be/abcdefghijk"))
        }
        val latest = listOf(movie("first"), movie("second"), movie("third"))
        session.updateMovies(Result.Success(latest))
        runCurrent()
        val picked = movie(session).slug
        assertTrue(picked in latest.map { it.slug })
        session.updateMovies(Result.Loading)
        session.updateMovies(Result.Success(listOf(movie("replacement"))))
        session.updateMovies(Result.Error(IllegalStateException("offline")))
        runCurrent()
        assertEquals(listOf(picked), requests)
        assertEquals(picked, movie(session).slug)
    }

    @Test fun retryKeepsTheSelectedMovieAndDoesNotStartDuplicateRequestsWhileLoading() = runTest {
        val requests = mutableListOf<String>()
        val pending = CompletableDeferred<Result<DataMovieDetail>>()
        val session = session(backgroundScope) { slug ->
            requests += slug
            if (requests.size == 1) pending.await() else Result.Success(DataMovieDetail(slug = slug, trailerUrl = "https://youtu.be/abcdefghijk"))
        }
        session.updateMovies(Result.Success(listOf(movie("picked"))))
        runCurrent()
        assertTrue(session.retry())
        session.updateMovies(Result.Success(listOf(movie("other"))))
        runCurrent()
        assertEquals(listOf("picked"), requests)
        pending.complete(Result.Error(IllegalStateException("offline")))
        runCurrent()
        assertTrue(session.movie.value is Result.Error)
        assertTrue(session.retry())
        runCurrent()
        assertEquals(listOf("picked", "picked"), requests)
        assertEquals("picked", movie(session).slug)
    }

    @Test fun newAppSessionCanChooseAgainWhileEmptyLatestListsMakeNoDetailRequests() = runTest {
        val requests = mutableListOf<String>()
        val request: suspend (String) -> Result<DataMovieDetail> = { slug ->
            requests += slug
            Result.Success(DataMovieDetail(slug = slug, trailerUrl = "https://youtu.be/abcdefghijk"))
        }
        val firstSession = session(backgroundScope, request)
        firstSession.updateMovies(Result.Success(emptyList()))
        runCurrent()
        assertNull((firstSession.movie.value as Result.Success).data)
        assertTrue(requests.isEmpty())
        firstSession.updateMovies(Result.Success(listOf(movie("first"))))
        runCurrent()
        val secondSession = session(backgroundScope, request)
        secondSession.updateMovies(Result.Success(listOf(movie("second"))))
        runCurrent()
        assertEquals(listOf("first", "second"), requests)
        assertEquals("first", movie(firstSession).slug)
        assertEquals("second", movie(secondSession).slug)
    }

    @Test fun reenteringTheAppChoosesFromTheLatestListWithoutRequiringAProcessRestart() = runTest {
        val requests = mutableListOf<String>()
        val session = session(backgroundScope) { slug ->
            requests += slug
            Result.Success(DataMovieDetail(slug = slug, trailerUrl = "https://youtu.be/abcdefghijk"))
        }
        session.updateMovies(Result.Success(listOf(movie("first"))))
        runCurrent()
        session.updateMovies(Result.Success(listOf(movie("new-latest"))))
        runCurrent()
        assertEquals(listOf("first"), requests)
        session.startNewSession()
        assertTrue(session.movie.value is Result.Loading)
        session.updateMovies(Result.Success(listOf(movie("new-latest"))))
        runCurrent()
        assertEquals(listOf("first", "new-latest"), requests)
        assertEquals("new-latest", movie(session).slug)
    }

    @Test fun skipsEmptyAndWhitespaceTrailersUntilAValidTrailerIsFound() = runTest {
        val requests = mutableListOf<String>()
        val session = session(backgroundScope) { slug ->
            requests += slug
            val trailer = when (requests.size) {
                1 -> ""
                2 -> " \t\n"
                else -> "https://youtu.be/abcdefghijk"
            }
            Result.Success(DataMovieDetail(slug = slug, trailerUrl = trailer))
        }
        session.updateMovies(Result.Success(listOf(movie("first"), movie("second"), movie("third"))))
        runCurrent()
        assertEquals(3, requests.size)
        assertEquals(3, requests.distinct().size)
        assertEquals(requests.last(), movie(session).slug)
        assertEquals("https://youtu.be/abcdefghijk", movie(session).trailerUrl)
        session.updateMovies(Result.Success(listOf(movie("replacement"))))
        runCurrent()
        assertEquals(3, requests.size)
    }

    @Test fun finishesWithoutAMovieWhenEveryUniqueCandidateHasABlankTrailer() = runTest {
        val requests = mutableListOf<String>()
        val session = session(backgroundScope) { slug ->
            requests += slug
            Result.Success(DataMovieDetail(slug = slug, trailerUrl = " \t"))
        }
        session.updateMovies(Result.Success(listOf(movie("first"), movie("first"), movie("second"), movie(""))))
        runCurrent()
        assertEquals(setOf("first", "second"), requests.toSet())
        assertEquals(2, requests.size)
        assertNull((session.movie.value as Result.Success).data)
        assertFalse(session.retry())
    }

    @Test fun detailFailuresDoNotPreventAnotherCandidateFromProvidingATrailer() = runTest {
        val requests = mutableListOf<String>()
        val session = session(backgroundScope) { slug ->
            requests += slug
            when (requests.size) {
                1 -> Result.Error(IllegalStateException("unavailable"))
                2 -> throw IllegalStateException("network error")
                else -> Result.Success(DataMovieDetail(slug = slug, trailerUrl = "https://youtu.be/abcdefghijk"))
            }
        }
        session.updateMovies(Result.Success(listOf(movie("first"), movie("second"), movie("third"))))
        runCurrent()
        assertEquals(3, requests.distinct().size)
        assertEquals(requests.last(), movie(session).slug)
    }

    private fun session(scope: CoroutineScope, request: suspend (String) -> Result<DataMovieDetail>) =
        HomeRecommendationSession(GetMovieDetail(object : MovieRepository by FakeMovieRepository() {
            override suspend fun getMovieDetail(slug: String) = request(slug)
        }), scope)

    private fun movie(slug: String) = PosterMovie(slug, "poster", slug, slug, "Full")
    private fun movie(session: HomeRecommendationSession) = requireNotNull((session.movie.value as Result.Success).data)
}
