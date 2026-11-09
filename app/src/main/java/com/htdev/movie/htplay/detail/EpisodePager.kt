package com.htdev.movie.htplay.detail

import com.htdev.movie.htplay.domain.entities.MoviePlayer

internal class EpisodePager(private val batchSize: Int = 50) {
    init { require(batchSize > 0) }

    private var episodes: List<MoviePlayer> = emptyList()
    var loadedCount: Int = 0
        private set

    val visibleEpisodes: List<MoviePlayer>
        get() = episodes.subList(0, loadedCount)
    val hasMore: Boolean
        get() = loadedCount < episodes.size

    fun submit(episodes: List<MoviePlayer>) {
        if (this.episodes == episodes) return
        this.episodes = episodes
        loadedCount = minOf(batchSize, episodes.size)
    }

    fun loadMore(): Boolean {
        if (!hasMore) return false
        loadedCount = minOf(loadedCount + batchSize, episodes.size)
        return true
    }
}
