package com.htdev.movie.htplay.mock

import android.content.Context
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.entities.ComingSoon
import com.htdev.movie.htplay.entities.ContinueWatching
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.entities.ItemMovie
import java.util.UUID

/** Offline fixtures. IDs stay stable when the screen reloads or lists are resubmitted. */
class MockHomeData(context: Context) {
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
        ComingSoon(id("soon-1"), date = context.getString(R.string.mock_date_1), name = context.getString(R.string.mock_movie_last_kingdom)),
        ComingSoon(id("soon-2"), date = context.getString(R.string.mock_date_2), name = context.getString(R.string.mock_movie_moonlight_promise)),
        ComingSoon(id("soon-3"), date = context.getString(R.string.mock_date_3), name = context.getString(R.string.mock_movie_beyond_stars)),
        ComingSoon(id("soon-4"), date = context.getString(R.string.mock_date_4), name = context.getString(R.string.mock_movie_long_title)),
    )

    val popularMovies = listOf(
        ItemMovie(id("popular-1"), tagFilm = context.getString(R.string.movie_tag_free), nameFile = context.getString(R.string.mock_movie_wolf_princess), rateCount = 4.8),
        ItemMovie(id("popular-2"), tagFilm = context.getString(R.string.movie_tag_vip), nameFile = context.getString(R.string.mock_movie_silent_ocean), rateCount = 4.6),
        ItemMovie(id("popular-3"), tagFilm = context.getString(R.string.movie_tag_new), nameFile = context.getString(R.string.mock_movie_chasing_tomorrow), rateCount = 4.9),
        ItemMovie(id("popular-4"), tagFilm = null, nameFile = context.getString(R.string.mock_movie_city_dreams), rateCount = 4.2),
        ItemMovie(id("popular-5"), tagFilm = context.getString(R.string.movie_tag_free), nameFile = context.getString(R.string.mock_movie_one_more_summer), rateCount = 0.0),
    )

    val actionMovies = listOf(
        ItemMovie(id("action-1"), tagFilm = context.getString(R.string.movie_tag_free), nameFile = context.getString(R.string.mock_movie_midnight_chase), rateCount = 4.5),
        ItemMovie(id("action-2"), tagFilm = context.getString(R.string.movie_tag_vip), nameFile = context.getString(R.string.mock_movie_final_mission), rateCount = 4.7),
        ItemMovie(id("action-3"), tagFilm = context.getString(R.string.movie_tag_new), nameFile = context.getString(R.string.mock_movie_shadow_agent), rateCount = 4.3),
    )

    val homeItems: List<HomeItem> = listOf(
        HomeItem.ContinueWatchingSector(id = 1, movies = continueWatching),
        HomeItem.ComingSoonSector(id = 2, movies = comingSoon),
        HomeItem.MovieSector(id = 3, genre = context.getString(R.string.popular_movies), movies = popularMovies),
        HomeItem.MovieSector(id = 4, genre = context.getString(R.string.genre_action), movies = actionMovies),
    )
}
