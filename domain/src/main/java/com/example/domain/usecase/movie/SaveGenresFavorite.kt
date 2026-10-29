package com.example.domain.usecase.movie

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class SaveGenresFavorite @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(genres : List<String>) = movieRepository.saveGenresFavorite(genres)
}