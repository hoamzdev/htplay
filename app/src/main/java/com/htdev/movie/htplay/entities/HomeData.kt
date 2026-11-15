package com.htdev.movie.htplay.entities

import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.PosterMovie

data class HomeData(
    val continueWatching: List<ContinueWatching> = emptyList(),
    val selectedGenreSlugs: List<String> = emptyList(),
    val recommendedMovies: Result<List<PosterMovie>> = Result.Loading,
    val htRecommendation: HomeRecommendation = HomeRecommendation(),
    val chineseMovies: Result<List<PosterMovie>> = Result.Loading,
    val koreanMovies: Result<List<PosterMovie>> = Result.Loading,
    val actionMovies: Result<List<PosterMovie>> = Result.Loading,
    val romanceMovies: Result<List<PosterMovie>> = Result.Loading,
    val familyMovies: Result<List<PosterMovie>> = Result.Loading,
    val countries: Result<List<Country>> = Result.Loading,
)
