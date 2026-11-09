package com.htdev.movie.htplay.detail

import com.htdev.movie.htplay.domain.entities.MoviePlayer
import org.junit.Assert.*
import org.junit.Test

class EpisodePagerTest {
    @Test fun loadsFiftyAtATimeAndKeepsTheOriginalEpisodeOrder() {
        val episodes = episodes("series", 123)
        val pager = EpisodePager()
        pager.submit(episodes)

        assertEquals(episodes.take(50), pager.visibleEpisodes)
        assertTrue(pager.hasMore)
        assertTrue(pager.loadMore())
        assertEquals(episodes.take(100), pager.visibleEpisodes)
        assertTrue(pager.loadMore())
        assertEquals(episodes, pager.visibleEpisodes)
        assertEquals(123, pager.loadedCount)
        assertFalse(pager.hasMore)
        assertFalse(pager.loadMore())
        assertEquals(episodes, pager.visibleEpisodes)
    }

    @Test fun emptyAndShortSeriesDoNotLoadAnotherBatch() {
        for (count in listOf(0, 1, 49, 50)) {
            val pager = EpisodePager()
            val episodes = episodes("series", count)
            pager.submit(episodes)
            assertEquals(episodes, pager.visibleEpisodes)
            assertFalse(pager.hasMore)
            assertFalse(pager.loadMore())
        }
    }

    @Test fun rebindingTheSameEpisodesKeepsTheLoadedBatches() {
        val pager = EpisodePager()
        val episodes = episodes("series", 150)
        pager.submit(episodes)
        pager.loadMore()
        pager.submit(episodes.toList())
        assertEquals(episodes.take(100), pager.visibleEpisodes)
    }

    @Test fun switchingMoviesAndClearingContentResetTheLoadedBatches() {
        val pager = EpisodePager()
        pager.submit(episodes("first", 200))
        pager.loadMore()
        val otherEpisodes = episodes("second", 80)
        pager.submit(otherEpisodes)
        assertEquals(otherEpisodes.take(50), pager.visibleEpisodes)

        pager.submit(emptyList())
        assertEquals(0, pager.loadedCount)
        assertTrue(pager.visibleEpisodes.isEmpty())
        assertFalse(pager.hasMore)

        pager.submit(otherEpisodes)
        assertEquals(otherEpisodes.take(50), pager.visibleEpisodes)
    }

    private fun episodes(movie: String, count: Int) = (1..count).map {
        MoviePlayer("$movie-$it", "$movie/$it.m3u8")
    }
}
