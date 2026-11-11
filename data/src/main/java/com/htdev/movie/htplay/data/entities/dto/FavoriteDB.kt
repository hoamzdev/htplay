package com.htdev.movie.htplay.data.entities.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.Favorite


@Entity(tableName = "favorite")
data class FavoriteDB(
    val name : String,
    @PrimaryKey
    val slugMovie : String,
    val genre : String,
    val posterUrl : String? = null,
)


// DataMovieDetail map to Favorite(domain)
fun DataMovieDetail.mapToFavorite() : Favorite{
    return Favorite(
        name = this.name,
        posterUrl = this.posterUrl,
        genre = this.genre,
        slugMovie = this.slug
    )
}

fun Favorite.mapToDB() : FavoriteDB{
    return FavoriteDB(
        name = name,
        posterUrl = posterUrl,
        slugMovie = slugMovie,
        genre = genre
    )
}

fun FavoriteDB.mapToDomain() : Favorite{
    return Favorite(
        name = name,
        posterUrl = posterUrl,
        slugMovie = slugMovie,
        genre = genre
    )
}
