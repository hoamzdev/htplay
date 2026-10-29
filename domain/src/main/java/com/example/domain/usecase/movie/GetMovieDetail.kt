package com.example.domain.usecase.movie

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMovieDetail @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(slugMovie : String) = movieRepository.getMovieDetail(slugMovie)
}