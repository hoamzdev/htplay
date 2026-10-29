package com.htdev.movie.htplay.entities

import java.util.UUID

data class ContinueWatching(
    val id : UUID = UUID.randomUUID(),
    val urlThumbnail : String? = null,
    val totalProgress : Long? = null,
    val currentProgress : Long? = null,
)
