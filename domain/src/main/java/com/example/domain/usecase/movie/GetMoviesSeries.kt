package com.example.domain.usecase.movie

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesSeries @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(genre : String,limit : Int = 20) = movieRepository.getMoviesSeries(genre,limit)
}