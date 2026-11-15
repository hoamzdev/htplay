package com.htdev.movie.htplay.entities

import com.htdev.movie.htplay.domain.entities.DataMovieDetail

data class HomeRecommendation(
    val movie: DataMovieDetail? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val isFavorite: Boolean? = null,
    val isUpdatingFavorite: Boolean = false,
)
