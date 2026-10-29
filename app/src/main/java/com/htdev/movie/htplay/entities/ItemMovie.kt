package com.htdev.movie.htplay.entities

import java.util.UUID

data class ItemMovie(
    val id : UUID = UUID.randomUUID(),
    val urlPoster : String? = null,
    val tagFilm : String? = null,
    val nameFile : String? = null,
    val rateCount : Double? = 0.0
)