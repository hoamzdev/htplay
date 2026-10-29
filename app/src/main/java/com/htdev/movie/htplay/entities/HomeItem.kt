package com.htdev.movie.htplay.entities


sealed class HomeItem {
    abstract val id : Int
    data class ContinueWatchingSector(override val id: Int, val movies: List<ContinueWatching>) : HomeItem()
    data class ComingSoonSector(override val id : Int, val movies : List<ComingSoon>) : HomeItem()
    data class MovieSector(override val id : Int, val genre : String, val movies : List<ItemMovie>) : HomeItem()
}