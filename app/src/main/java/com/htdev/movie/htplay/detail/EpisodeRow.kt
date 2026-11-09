package com.htdev.movie.htplay.detail

import com.htdev.movie.htplay.domain.entities.MoviePlayer

internal const val EPISODES_PER_ROW = 6

internal data class EpisodeRow(
    val firstEpisodeIndex: Int,
    val episodes: List<MoviePlayer>,
)

internal fun List<MoviePlayer>.toEpisodeRows(): List<EpisodeRow> =
    chunked(EPISODES_PER_ROW).mapIndexed { rowIndex, episodes ->
        EpisodeRow(rowIndex * EPISODES_PER_ROW, episodes)
    }
