package com.htdev.movie.htplay.data.entities.dto

import com.google.gson.annotations.SerializedName
import com.htdev.movie.htplay.data.common.toImageUrl
import com.htdev.movie.htplay.domain.entities.PosterMovie

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
    @SerializedName("thumb_url")
    val thumbURL: String? = null,
    @SerializedName("episode_current")
    val episodeCurrent: String,
    val year: Long? = null,
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
        thumbUrl = this.thumbURL.toImageUrl(),
        year = this.year,
        name = this.originName ?: "",
        slug = this.slug ?: "",
        episodeCurrent = this.episodeCurrent
    )
}
