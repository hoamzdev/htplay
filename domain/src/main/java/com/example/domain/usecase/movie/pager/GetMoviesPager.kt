package com.example.domain.usecase.movie.pager

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesPager @Inject constructor(
    private val movieRepository: MovieRepository
) {
    operator fun invoke()  = movieRepository.getMoviesPager()
}