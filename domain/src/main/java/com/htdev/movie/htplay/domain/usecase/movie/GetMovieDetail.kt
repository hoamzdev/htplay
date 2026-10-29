package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMovieDetail @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(slugMovie : String) = movieRepository.getMovieDetail(slugMovie)
}