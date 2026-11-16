package com.htdev.movie.htplay.home.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class DownloadMetadataTest {
    @Test fun searchesNamesSlugsAndEpisodeLabelsUsingTheSameRulesAsFavorites() {
        val metadata = DownloadMetadata("Đường Đến Hạnh Phúc", 10, "duong-den-hanh-phuc")
        for (query in listOf("  DUONG   DEN  ", "đường đến", "hanh-phuc", "tap 10", "10", "   ", "")) {
            assertTrue("Expected a match for: $query", metadata.matchesSearch(query, "Tập 10"))
        }
        assertTrue(DownloadMetadata("An English Title", 2, "ten-phim").matchesSearch("ten phim", "Episode 2"))
        assertFalse(metadata.matchesSearch("missing", "Tập 10"))
        assertFalse(metadata.matchesSearch("11", "Tập 10"))
    }

    @Test fun preservesTitleAndEpisodeWithoutSavingALocalizedLabel() {
        val metadata = DownloadMetadata("Phim \"Mùa hè\" - 2026\n电影", 10, "mua-he-2026", "https://example.com/poster.jpg")
        assertEquals(metadata, DownloadMetadata.fromByteArray(metadata.toByteArray()))
    }

    @Test fun readsPreviouslySavedMetadataWithoutAMovieSlug() {
        assertEquals(DownloadMetadata("Movie", 10), decode("{\"title\":\"Movie\",\"episodeNumber\":10}"))
    }

    @Test fun readsPreviouslySavedMetadataWithoutAPoster() {
        assertEquals(
            DownloadMetadata("Movie", 10, "movie"),
            decode("{\"title\":\"Movie\",\"episodeNumber\":10,\"movieSlug\":\"movie\"}"),
        )
    }

    @Test fun readsLegacyEpisodeSlugsWithoutConfusingNumbersInMovieTitles() {
        for (slug in listOf("10", "010", "tap-10", "episode-10", "EP10")) {
            assertEquals(DownloadMetadata("Movie - 2026", 10), decode("Movie - 2026 - $slug"))
        }
    }

    @Test fun readsLegacySingleEpisodeMovies() {
        assertEquals(DownloadMetadata("Movie", 1), decode("Movie - full"))
    }

    @Test fun preservesLegacyTitlesWhenTheEpisodeCannotBeRecovered() {
        for (title in listOf("Movie 2026", "Movie - special", "Movie - 0", "Movie - 99999999999", "")) {
            assertEquals(DownloadMetadata(title, null), decode(title))
        }
    }

    @Test fun invalidJsonFallsBackToTheOriginalTitle() {
        for (title in listOf("{movie}", "{\"title\":\"Movie\"}", "{\"title\":\"Movie\",\"episodeNumber\":0}")) {
            assertEquals(DownloadMetadata(title, null), decode(title))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotSaveAnInvalidEpisodeNumber() {
        DownloadMetadata("Movie", 0).toByteArray()
    }

    private fun decode(text: String) = DownloadMetadata.fromByteArray(text.toByteArray(Charsets.UTF_8))
}
