package com.htdev.movie.htplay.domain.entities

/**
 * @author hwa..
 */

data class Genre(
    val name : String,
    val slug : String,
    val poster : String,
)

data class Country(
    val name : String,
    val slug : String,
)