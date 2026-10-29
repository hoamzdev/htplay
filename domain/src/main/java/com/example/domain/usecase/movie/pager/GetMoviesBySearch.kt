package com.example.domain.usecase.movie.pager

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesBySearch @Inject constructor(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(keyword : String,genre : String = "All",limit : Int = 20) = movieRepository.getMoviesBySearch(keyword = keyword,genre = genre,limit = limit)
}