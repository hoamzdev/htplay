package com.example.data.entities.dto

import com.google.gson.annotations.SerializedName
import com.example.data.common.toImageUrl
import com.example.domain.entities.PosterMovie

/**
 * @author hwa..
 */

data class MovieResponse(
    val data : MovieByGenre
)

data class MovieByGenre (
    val items: List<ItemByGenre>,
    val params : Params
)

data class ItemByGenre (
    @SerializedName("_id")
    val id: String,
    val slug: String? = null,
    @SerializedName("origin_name")
    val originName: String? = null,
    @SerializedName("poster_url")
    val posterURL: String? = null,
    @SerializedName("episode_current")
    val episodeCurrent: String,
)

data class Params(
    val pagination: Pagination,
)

data class Pagination(
    val totalPages : Long,
)

fun ItemByGenre.mapToPosterMovie() : PosterMovie{
    return PosterMovie(
        id = this.id,
        posterUrl = this.posterURL.toImageUrl(),
        name = this.originName ?: "",
        slug = this.slug ?: "",
        episodeCurrent = this.episodeCurrent
    )
}