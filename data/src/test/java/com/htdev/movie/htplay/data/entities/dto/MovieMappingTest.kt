package com.htdev.movie.htplay.data.entities.dto

import com.htdev.movie.htplay.data.common.toImageUrl
import com.htdev.movie.htplay.data.BuildConfig
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MovieMappingTest {
    @Test fun detailReadsTheApiTrailerUrlFieldAndKeepsMissingTrailersEmpty() {
        val json = """{"_id":"movie-id","slug":"movie","origin_name":"Movie","content":"","tmdb":{"vote_average":5.0},"episode_current":"Full","quality":"HD","year":2026,"category":[],"country":[],"trailer_url":"https://youtu.be/abcdefghijk"}"""
        val movie = Gson().fromJson(json, MovieDetail::class.java)
        assertEquals("https://youtu.be/abcdefghijk", movie.mapToDataMovieDetail().trailerUrl)
        assertEquals("", movie.copy(trailerUrl = null).mapToDataMovieDetail().trailerUrl)
    }

    @Test
    fun imageUrlsHandleAbsoluteRelativeAndMissingPaths() {
        assertEquals("https://cdn.example.com/poster.jpg", "https://cdn.example.com/poster.jpg".toImageUrl())
        assertEquals("${BuildConfig.BASE_URL_IMAGE.trimEnd('/')}/poster.jpg", "/poster.jpg".toImageUrl())
        assertEquals("https://cdn.example.com/a.jpg", "//cdn.example.com/a.jpg".toImageUrl())
        assertEquals("", (null as String?).toImageUrl())
        assertEquals("", "  ".toImageUrl())
    }

    @Test
    fun GenrePosterDoesNotPrefixAnAbsoluteUrlOrProduceNullPath() {
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
        assertEquals("${BuildConfig.BASE_URL_IMAGE.trimEnd('/')}/poster.jpg", detail.posterUrl)
        assertEquals("", detail.trailerUrl)
        assertEquals("https://youtu.be/abcdefghijk", movie.copy(trailerUrl = " https://youtu.be/abcdefghijk ").mapToDataMovieDetail().trailerUrl)
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
