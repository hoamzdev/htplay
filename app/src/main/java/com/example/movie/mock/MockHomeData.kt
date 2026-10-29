package com.example.movie.mock

import com.example.movie.entities.ComingSoon
import com.example.movie.entities.ContinueWatching
import com.example.movie.entities.HomeItem
import com.example.movie.entities.ItemMovie
import java.util.UUID

/** Offline fixtures. IDs stay stable when the screen reloads or lists are resubmitted. */
object MockHomeData {
    private fun id(key: String): UUID = UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8))

    val continueWatching = listOf(
        ContinueWatching(id("continue-1"), totalProgress = 7200, currentProgress = 1800),
        ContinueWatching(id("continue-2"), totalProgress = 5400, currentProgress = 4200),
        ContinueWatching(id("continue-3"), totalProgress = 3600, currentProgress = 3600),
        ContinueWatching(id("continue-null")),
        ContinueWatching(id("continue-zero"), totalProgress = 0, currentProgress = 60),
        ContinueWatching(id("continue-overflow"), totalProgress = 600, currentProgress = 900),
    )

    val comingSoon = listOf(
        ComingSoon(id("soon-1"), date = "30/10/2026", name = "The Last Kingdom"),
        ComingSoon(id("soon-2"), date = "06/11/2026", name = "Moonlight Promise"),
        ComingSoon(id("soon-3"), date = "13/11/2026", name = "Beyond the Stars"),
        ComingSoon(id("soon-4"), date = "20/11/2026", name = "A Very Long Movie Title for Testing Two Lines"),
    )

    val popularMovies = listOf(
        ItemMovie(id("popular-1"), tagFilm = "Free", nameFile = "Never Reject a Wolf Princess", rateCount = 4.8),
        ItemMovie(id("popular-2"), tagFilm = "VIP", nameFile = "The Silent Ocean", rateCount = 4.6),
        ItemMovie(id("popular-3"), tagFilm = "New", nameFile = "Chasing Tomorrow", rateCount = 4.9),
        ItemMovie(id("popular-4"), tagFilm = null, nameFile = "City of Dreams", rateCount = 4.2),
        ItemMovie(id("popular-5"), tagFilm = "Free", nameFile = "One More Summer", rateCount = 0.0),
    )

    val actionMovies = listOf(
        ItemMovie(id("action-1"), tagFilm = "Free", nameFile = "Midnight Chase", rateCount = 4.5),
        ItemMovie(id("action-2"), tagFilm = "VIP", nameFile = "Final Mission", rateCount = 4.7),
        ItemMovie(id("action-3"), tagFilm = "New", nameFile = "Shadow Agent", rateCount = 4.3),
    )

    val homeItems: List<HomeItem> = listOf(
        HomeItem.ContinueWatchingSector(id = 1, movies = continueWatching),
        HomeItem.ComingSoonSector(id = 2, movies = comingSoon),
        HomeItem.MovieSector(id = 3, genre = "Popular Movies", movies = popularMovies),
        HomeItem.MovieSector(id = 4, genre = "Action", movies = actionMovies),
    )
}
