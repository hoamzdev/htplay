package com.htdev.movie.htplay.util

import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.entities.HomeData
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.entities.ContinueWatching
import org.junit.Assert.*
import org.junit.Test

class HomeSectionsTest {
    private val movie = PosterMovie("movie-1", "poster", "Movie", "movie", "Episode 1")

    @Test
    fun popularKeepsItsSectionsWithoutTheOldGenreSection() {
        val sections = sections(HomeData(
            chineseMovies = Result.Error(IllegalStateException("offline")),
            koreanMovies = Result.Success(listOf(movie)),
        ))

        assertEquals(listOf(2, 3, 4, 5, 8, 9, 10, 7), sections.map { it.id })
        assertTrue((sections.first { it.id == HomeItem.CHINESE_MOVIES } as HomeItem.MovieSector).hasError)
        assertEquals("movie", (sections.first { it.id == HomeItem.KOREAN_MOVIES } as HomeItem.MovieSector).movies.single().slug)
        assertTrue((sections.last() as HomeItem.CountrySector).isLoading)
        assertFalse(sections.any { it is HomeItem.ContinueWatchingSector })
    }

    @Test
    fun continueWatchingReturnsAtTheTopWhenThereAreUnfinishedMovies() {
        val movies = listOf(ContinueWatching(slugMovie = "unfinished", totalProgress = 100, currentProgress = 40))
        val before = sections(HomeData(continueWatching = movies))
        assertEquals(movies, (before.first() as HomeItem.ContinueWatchingSector).movies)
        val after = sections(HomeData(continueWatching = emptyList()))
        assertEquals(before.drop(1), after)
    }

    @Test
    fun refreshedMoviesKeepTheirIdsAndDoNotInventRatings() {
        val home = HomeData(koreanMovies = Result.Success(listOf(movie)))
        val before = (sections(home).first { it.id == HomeItem.KOREAN_MOVIES } as HomeItem.MovieSector).movies.single()
        val after = (sections(home.copy(koreanMovies = Result.Success(listOf(movie.copy(name = "Updated")))))
            .first { it.id == HomeItem.KOREAN_MOVIES }
            as HomeItem.MovieSector).movies.single()

        assertEquals(before.id, after.id)
        assertEquals("Updated", after.nameFile)
        assertNull(after.rateCount)
    }

    @Test
    fun oldFavoriteNamesAndNewSlugsResolveToUniqueApiSlugs() {
        assertEquals(listOf("hanh-dong", "tinh-cam", "co-trang"),
            normalizeFavoriteGenres(listOf(" Action ", "hanh-dong", "Romance", "co-trang", "")))
    }

    private fun sections(home: HomeData) = buildHomeSections(
        Result.Success(listOf(movie)), home, "Latest", "For you", "China", "Korea", "Action", "Romance", "Family",
    )
}
