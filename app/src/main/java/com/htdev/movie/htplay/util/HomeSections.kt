package com.htdev.movie.htplay.util

import com.htdev.movie.htplay.data.db.LocalMovieCatalog
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.entities.HomeData
import com.htdev.movie.htplay.entities.HomeItem
import com.htdev.movie.htplay.entities.ItemMovie
import java.util.UUID

// Ho tro ca ten the loai da luu o cac ban cu va slug tu man Favorite.
fun normalizeFavoriteGenres(savedGenres: List<String>): List<String> =
    savedGenres.mapNotNull { saved ->
        LocalMovieCatalog.genres.firstOrNull {
            it.slug.equals(saved.trim(), ignoreCase = true) ||
                    it.name.equals(saved.trim(), ignoreCase = true)
        }?.slug ?: saved.trim().takeIf { it.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*")) }
    }.distinct()

fun buildHomeSections(
    latestMovies: Result<List<PosterMovie>>,
    home: HomeData,
    latestTitle: String,
    recommendedTitle: String,
    chineseTitle: String,
    koreanTitle: String,
    actionTitle: String,
    romanceTitle: String,
    familyTitle: String,
    localizeCountry: (Country) -> Country = { it },
): List<HomeItem> = buildList {
    if (home.continueWatching.isNotEmpty()) {
        add(HomeItem.ContinueWatchingSector(HomeItem.CONTINUE_WATCHING, home.continueWatching))
    }
    add(latestMovies.toMovieSector(HomeItem.LATEST_MOVIES, latestTitle))
    add(home.recommendedMovies.toMovieSector(HomeItem.SELECTED_FOR_YOU, recommendedTitle))
    add(home.chineseMovies.toMovieSector(HomeItem.CHINESE_MOVIES, chineseTitle))
    add(home.koreanMovies.toMovieSector(HomeItem.KOREAN_MOVIES, koreanTitle))
    add(home.actionMovies.toMovieSector(HomeItem.ACTION_MOVIES, actionTitle))
    add(home.romanceMovies.toMovieSector(HomeItem.ROMANCE_MOVIES, romanceTitle))
    add(home.familyMovies.toMovieSector(HomeItem.FAMILY_MOVIES, familyTitle))
    add(HomeItem.CountrySector(
        HomeItem.COUNTRIES,
        (home.countries as? Result.Success)?.data.orEmpty().map(localizeCountry),
        isLoading = home.countries is Result.Loading,
        hasError = home.countries is Result.Error,
    ))
}

private fun Result<List<PosterMovie>>.toMovieSector(id: Int, title: String): HomeItem.MovieSector =
    HomeItem.MovieSector(
        id = id,
        Genre = title,
        movies = (this as? Result.Success)?.data.orEmpty().map { movie ->
            ItemMovie(
                id = UUID.nameUUIDFromBytes(movie.id.ifBlank { movie.slug }
                    .toByteArray(Charsets.UTF_8)),
                urlPoster = movie.posterUrl,
                tagFilm = movie.episodeCurrent,
                nameFile = movie.name,
                rateCount = null,
                slug = movie.slug,
            )
        },
        isLoading = this is Result.Loading,
        hasError = this is Result.Error,
    )
