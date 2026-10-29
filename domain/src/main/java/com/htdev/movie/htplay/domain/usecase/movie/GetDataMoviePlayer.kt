package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetDataMoviePlayer @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(slugMovie : String) = movieRepository.getDataMoviePlayer(slugMovie)
}