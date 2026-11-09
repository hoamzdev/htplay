package com.htdev.movie.htplay.domain.entities

//data class for continue watching
data class ContinueWatching(
    val id : Long? = null,
    val name : String,
    val slugMovie : String,
    val thumbnail : String? = null,
    val genre : String,
    val currentEpisode : Int,
    // Playback times are milliseconds; currentEpisode is a zero-based index.
    val lengthCurrentEpisode : Long,
    val positionContinue : Long,
)
