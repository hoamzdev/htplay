package com.htdev.movie.htplay.data.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.htdev.movie.htplay.data.repository.LocalStorageRepositoryImpl
import com.htdev.movie.htplay.domain.entities.ContinueWatching
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ContinueWatchingDatabaseTest {
    private lateinit var database: AppRoomDataBase
    private lateinit var repository: LocalStorageRepositoryImpl

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppRoomDataBase::class.java).build()
        repository = LocalStorageRepositoryImpl(context.getSharedPreferences("continue_watching_test", 0), database.continueWatchingDao())
    }

    @After fun tearDown() { database.close() }

    @Test fun savingSixMoviesStoresOnlyTheFiveMostRecent() = runBlocking {
        (1..6).forEach { repository.saveContinueWatching(movie("film-$it")) }
        assertEquals((6 downTo 2).map { "film-$it" }, repository.getListContinueWatching().first().map { it.slugMovie })
        database.openHelper.readableDatabase.query("select count(*) from continue_watching").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5, cursor.getInt(0))
        }
    }

    @Test fun updatingAnExistingMovieMovesItToTheTopWithoutDuplicatesOrEvictingAnotherMovie() = runBlocking {
        (1..5).forEach { repository.saveContinueWatching(movie("film-$it")) }
        repository.saveContinueWatching(movie("film-1").copy(positionContinue = 75_000, currentEpisode = 2))
        val movies = repository.getListContinueWatching().first()
        assertEquals(listOf("film-1", "film-5", "film-4", "film-3", "film-2"), movies.map { it.slugMovie })
        assertEquals(75_000L, movies.first().positionContinue)
        assertEquals(2, movies.first().currentEpisode)
    }

    @Test fun completedMoviesAreRemovedAndInvalidPlaybackDoesNotCreateHistory() = runBlocking {
        repository.saveContinueWatching(movie("film"))
        repository.saveContinueWatching(movie("film").copy(positionContinue = 120_000))
        repository.saveContinueWatching(movie("unstarted").copy(positionContinue = 0))
        repository.saveContinueWatching(movie("unknown").copy(lengthCurrentEpisode = -1))
        assertTrue(repository.getListContinueWatching().first().isEmpty())
    }

    @Test fun subsecondProgressDoesNotCreateOrOverwriteHistory() = runBlocking {
        listOf(-1L, 0L, 1L, 999L).forEach { position ->
            repository.saveContinueWatching(movie("film").copy(positionContinue = position))
        }
        assertTrue(repository.getListContinueWatching().first().isEmpty())

        repository.saveContinueWatching(movie("film").copy(positionContinue = 1_000))
        assertEquals(1_000L, repository.getListContinueWatching().first().single().positionContinue)

        repository.saveContinueWatching(movie("film").copy(positionContinue = 999))
        assertEquals(1_000L, repository.getListContinueWatching().first().single().positionContinue)
    }

    private fun movie(slug: String) = ContinueWatching(
        name = slug, slugMovie = slug, genre = "hanh-dong", currentEpisode = 0,
        lengthCurrentEpisode = 120_000, positionContinue = 45_000,
    )
}
