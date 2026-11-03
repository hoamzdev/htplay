package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

class GetGenres @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke() = movieRepository.getGenres()
}
