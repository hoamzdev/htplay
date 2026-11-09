package com.htdev.movie.htplay.data.entities.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.htdev.movie.htplay.domain.entities.ContinueWatching


@Entity(tableName = "continue_watching")
data class ContinueWatchingDB(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val name: String,
    val slugMovie: String,
    val thumbnail: String? = null,
    val genre: String,
    val currentEpisode: Int,
    val lengthCurrentEpisode: Long,
    val positionContinue: Long,
)


fun ContinueWatching.mapToDB() : ContinueWatchingDB{
    return ContinueWatchingDB(
        id = id,
        name = name,
        slugMovie = slugMovie,
        thumbnail = thumbnail,
        genre = genre,
        currentEpisode = currentEpisode,
        lengthCurrentEpisode = lengthCurrentEpisode,
        positionContinue = positionContinue
    )
}


fun ContinueWatchingDB.mapToDomain() : ContinueWatching{
    return ContinueWatching(
        id = id,
        name = name,
        slugMovie = slugMovie,
        thumbnail = thumbnail,
        genre = genre,
        currentEpisode = currentEpisode,
        lengthCurrentEpisode = lengthCurrentEpisode,
        positionContinue = positionContinue
    )
}