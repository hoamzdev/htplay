package com.htdev.movie.htplay.detail

import com.htdev.movie.htplay.domain.entities.MoviePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeRowTest {
    @Test fun longSeriesKeepsEveryEpisodeInOrderIncludingThePartialLastRow() {
        val episodes = episodes(125)
        val rows = episodes.toEpisodeRows()

        assertEquals(21, rows.size)
        assertEquals(episodes, rows.flatMap { it.episodes })
        assertEquals((0..120 step 6).toList(), rows.map { it.firstEpisodeIndex })
        assertEquals(5, rows.last().episodes.size)
        assertTrue(rows.dropLast(1).all { it.episodes.size == EPISODES_PER_ROW })
    }

    @Test fun numberingStaysCorrectAtRowBoundaries() {
        for (count in listOf(0, 1, 5, 6, 7, 100, 101, 120, 121, 1000)) {
            val episodes = episodes(count)
            val rows = episodes.toEpisodeRows()
            assertEquals((count + 5) / 6, rows.size)
            rows.forEach { row ->
                row.episodes.forEachIndexed { column, episode ->
                    assertEquals(episodes[row.firstEpisodeIndex + column], episode)
                }
            }
        }
    }

    private fun episodes(count: Int) = (1..count).map {
        MoviePlayer("episode-$it", "https://example.com/$it.m3u8")
    }
}
