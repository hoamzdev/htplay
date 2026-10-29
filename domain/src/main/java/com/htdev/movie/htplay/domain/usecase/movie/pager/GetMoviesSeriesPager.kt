package com.htdev.movie.htplay.domain.usecase.movie.pager

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesSeriesPager @Inject constructor(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(genre : String,limit : Int = 20) = movieRepository.getMoviesSeriesPager(genre,limit)
}