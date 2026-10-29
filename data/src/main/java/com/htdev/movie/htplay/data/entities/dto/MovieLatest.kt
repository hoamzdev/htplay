package com.htdev.movie.htplay.data.entities.dto

import com.google.gson.annotations.SerializedName
import com.htdev.movie.htplay.data.common.toImageUrl
import com.htdev.movie.htplay.data.db.genreSlugToEnglish
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.entities.PosterMovieHome

/**
 * @author hwa..
 */

data class Movie(
    val items : List<Item>,
    val pagination : Pagination
)

data class Item (
    val tmdb: Tmdb,
    @SerializedName("_id")
    val id: String,
    val slug: String? = null,
    @SerializedName("origin_name")
    val originName: String? = null,
    @SerializedName("poster_url")
    val posterURL: String? = null,
    val year: Long? = 2026,
    @SerializedName("episode_current")
    val episodeCurrent: String? = "Full",
    val quality: String?,
    val category : List<Category>? = emptyList(),
)

data class Category(
    val slug : String,
)

data class Tmdb (
    @SerializedName("vote_average")
    val voteAverage: Double? = 0.0,
)

fun Item.mapToPosterMovieHome() : PosterMovieHome{
    return PosterMovieHome(
        id = this.id,
        posterUrl = this.posterURL.toImageUrl(),
        name = this.originName ?: "",
        slug = this.slug ?: "",
        year = this.year ?: 2026,
        vote = String.format(java.util.Locale.US, "%.1f", this.tmdb.voteAverage ?: 0.0),
        genre = if(this.category.isNullOrEmpty()) "Unknown" else genreSlugToEnglish(this.category[0].slug),
        quality = this.quality ?: "FHD"
    )
}

fun Item.mapToPosterMovie() : PosterMovie{
    return PosterMovie(
        id = this.id,
        posterUrl = this.posterURL.toImageUrl(),
        name = this.originName ?: "",
        slug = this.slug ?: "",
        episodeCurrent = this.episodeCurrent ?: ""
    )
}