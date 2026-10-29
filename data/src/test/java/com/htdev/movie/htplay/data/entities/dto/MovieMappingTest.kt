package com.htdev.movie.htplay.data.entities.dto

import com.htdev.movie.htplay.data.common.toImageUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MovieMappingTest {
    @Test
    fun imageUrlsHandleAbsoluteRelativeAndMissingPaths() {
        assertEquals("https://cdn.example.com/poster.jpg", "https://cdn.example.com/poster.jpg".toImageUrl())
        assertEquals("https://phimimg.com/poster.jpg", "/poster.jpg".toImageUrl())
        assertEquals("https://cdn.example.com/a.jpg", "//cdn.example.com/a.jpg".toImageUrl())
        assertEquals("", (null as String?).toImageUrl())
        assertEquals("", "  ".toImageUrl())
    }

    @Test
    fun genrePosterDoesNotPrefixAnAbsoluteUrlOrProduceNullPath() {
        val item = ItemByGenre("id", posterURL = "https://cdn.example.com/a.jpg", episodeCurrent = "Full")
        assertEquals("https://cdn.example.com/a.jpg", item.mapToPosterMovie().posterUrl)
        assertEquals("", item.copy(posterURL = null).mapToPosterMovie().posterUrl)
    }

    @Test
    fun detailAllowsEmptyCountryAndCategoryLists() {
        val movie = MovieDetail(
            tmdb = Tmdb(null), id = "id", slug = "movie", originName = "Movie", content = "",
            posterURL = "/poster.jpg", thumbUrl = null, episodeCurrent = "Full", quality = "HD",
            year = 2026, category = emptyList(), country = emptyList()
        )
        val detail = movie.mapToDataMovieDetail()
        assertEquals("", detail.countrySlug)
        assertEquals("Unknown", detail.genreSlug)
        assertEquals("0.0", detail.vote)
        assertEquals("https://phimimg.com/poster.jpg", detail.posterUrl)
    }

    @Test
    fun voteUsesADecimalPointRegardlessOfDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val item = Item(tmdb = Tmdb(9.5), id = "id", quality = null)
            assertEquals("9.5", item.mapToPosterMovieHome().vote)
        } finally {
            Locale.setDefault(previous)
        }
    }
}
