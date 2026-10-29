package com.htdev.movie.htplay.entities

import java.util.UUID

data class ComingSoon(
    val id : UUID = UUID.randomUUID(),
    val thumbnail : String? = "",
    val date : String? = null,
    val name : String? = "",
)