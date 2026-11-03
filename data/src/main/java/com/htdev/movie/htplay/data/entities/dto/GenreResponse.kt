package com.htdev.movie.htplay.data.entities.dto


data class GenreResponse(
    val data: GenreData
)


data class Genre(
    val name : String,
    val slug : String
)


data class GenreData(
    val items : List<Genre>
)

