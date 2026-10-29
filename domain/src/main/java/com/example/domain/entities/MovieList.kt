package com.example.domain.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author hwa..
 */

//lau nhung field poster can
@Parcelize
data class PosterMovieHome(
    val id : String,
    val posterUrl : String,
    val name : String,
    val genre : String,
    val slug : String,
    val quality : String,
    val year : Long,
    val vote : String,
) : Parcelable

@Parcelize
data class PosterMovie(
    val id : String,
    val posterUrl : String,
    val name : String,
    val slug : String,
    val episodeCurrent: String,
) : Parcelable

@Parcelize
data class DataMovieDetail(
    val id : String = "",
    val posterUrl : String = "",
    val thumbUrl : String = "",
    val name : String = "",
    val content : String = "",
    val year : Long = 2026,
    val slug : String = "",
    val episodeCurrent : String = "",
    val countrySlug : String = "",
    val genreSlug : String = "",
    val genre : String = "",
    val vote : String = "",
) : Parcelable

@Parcelize
data class MoviePlayer(
    val id : String,
    val linkM3u8 : String,
) : Parcelable

@Parcelize
data class DataMoviePlayer(
    val vietsub : List<MoviePlayer> = emptyList(),
    val longtieng : List<MoviePlayer> = emptyList(),
) : Parcelable
