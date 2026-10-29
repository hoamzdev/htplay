package com.htdev.movie.htplay.domain.usecase.movie.pager

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesPager @Inject constructor(
    private val movieRepository: MovieRepository
) {
    operator fun invoke()  = movieRepository.getMoviesPager()
}