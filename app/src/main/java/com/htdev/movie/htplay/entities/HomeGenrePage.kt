package com.htdev.movie.htplay.entities

import androidx.paging.PagingData
import com.htdev.movie.htplay.domain.entities.PosterMovie

data class HomeGenrePage(
    val genreSlug: String?,
    val movies: PagingData<PosterMovie>,
)
