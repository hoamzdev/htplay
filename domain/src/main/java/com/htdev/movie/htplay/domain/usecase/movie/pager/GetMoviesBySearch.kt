package com.htdev.movie.htplay.domain.usecase.movie.pager

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesBySearch @Inject constructor(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(keyword : String,Genre : String = "All",limit : Int = 20) = movieRepository.getMoviesBySearch(keyword = keyword,Genre = Genre,limit = limit)
}