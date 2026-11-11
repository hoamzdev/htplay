package com.htdev.movie.htplay.domain.entities

data class Favorite(
    val name : String,
    val slugMovie : String,
    val genre : String,
    val posterUrl : String? = null,
)
