package com.example.domain.usecase.movie

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetDataMoviePlayer @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(slugMovie : String) = movieRepository.getDataMoviePlayer(slugMovie)
}