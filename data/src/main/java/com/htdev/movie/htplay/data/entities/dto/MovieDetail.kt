package com.htdev.movie.htplay.data.entities.dto

import com.htdev.movie.htplay.data.common.toImageUrl
import com.google.gson.annotations.SerializedName
import com.htdev.movie.htplay.data.db.genreSlugToEnglish
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.DataMoviePlayer
import com.htdev.movie.htplay.domain.entities.MoviePlayer

/**
 * @author hwa..
 */

data class MovieDetailPage (
    val movie: MovieDetail,
    val episodes: List<Episode>
)

data class Episode (
    @SerializedName("server_data")
    val serverData: List<ServerDatum>
)

data class ServerDatum (
    val slug: String,
    @SerializedName("link_m3u8")
    val linkM3U8: String
)

data class MovieDetail (
    val tmdb: Tmdb,
    @SerializedName("_id")
    val id: String,
    val slug: String,
    @SerializedName("origin_name")
    val originName: String,
    val content: String,
    @SerializedName("poster_url")
    val posterURL: String?,
    @SerializedName("thumb_url")
    val thumbUrl : String?,
    @SerializedName("episode_current")
    val episodeCurrent: String,
    val quality: String,
    val year: Long,
    val category: List<Category>?,
    val country: List<Country>?,
    @SerializedName("trailer_url")
    val trailerUrl: String? = null,
)

fun MovieDetail.mapToDataMovieDetail() : DataMovieDetail{
    return DataMovieDetail(
        id = this.id,
        posterUrl = this.posterURL.toImageUrl(),
        name = this.originName,
        content = this.content,
        year = this.year,
        countrySlug = this.country?.firstOrNull()?.slug.orEmpty(),
        episodeCurrent = this.episodeCurrent,
        genre = if(this.category.isNullOrEmpty()) "Unknown" else this.category[0].slug,
        vote = String.format(java.util.Locale.US, "%.1f", this.tmdb.voteAverage ?: 0.0),
        genreSlug = if(this.category.isNullOrEmpty()) "Unknown" else this.category[0].slug,
        slug = this.slug,
        thumbUrl = this.thumbUrl.toImageUrl(),
        trailerUrl = this.trailerUrl?.trim().orEmpty(),
    )
}

fun List<Episode>.mapToDataMoviePlayer() : DataMoviePlayer{
    if(this.isEmpty()) return DataMoviePlayer()//ko co du lieu
    if(this.size == 1){//chi co server vietsub
        val episodeVietSub = this[0].serverData
        return DataMoviePlayer(
            vietsub = episodeVietSub.map { (slug, linkM3U8) -> MoviePlayer(id = slug, linkM3u8 = linkM3U8) }
        )
    }
    //co 2 -> co ca vietsub va co ca long tieng
    val episodeVietSub = this[0].serverData
    val episodeLongTieng = this[1].serverData
    return DataMoviePlayer(
        vietsub = episodeVietSub.map { (slug, linkM3U8) -> MoviePlayer(id = slug, linkM3u8 = linkM3U8) },
        longtieng = episodeLongTieng.map { (slug, linkM3U8) -> MoviePlayer(id = slug, linkM3u8 = linkM3U8) }
    )
}
