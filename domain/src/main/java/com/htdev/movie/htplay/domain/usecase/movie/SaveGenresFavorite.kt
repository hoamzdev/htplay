package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class SaveGenresFavorite @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(Genres : List<String>) = movieRepository.saveGenresFavorite(Genres)
}