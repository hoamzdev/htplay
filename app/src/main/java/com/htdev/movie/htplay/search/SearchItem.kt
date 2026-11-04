package com.htdev.movie.htplay.search

import com.htdev.movie.htplay.domain.entities.PosterMovie
import java.util.UUID

data class SearchItem(
    val uuid: UUID = UUID.randomUUID(),
    val name : String,
    val thumbnail: String? = "",
    val slug : String,
    val year : Long? = null,
)

fun PosterMovie.toSearchItem() = SearchItem(
    uuid = UUID.nameUUIDFromBytes(id.ifBlank { slug }.toByteArray(Charsets.UTF_8)),
    name = name,
    thumbnail = thumbUrl.ifBlank { posterUrl },
    slug = slug,
    year = year,
)
