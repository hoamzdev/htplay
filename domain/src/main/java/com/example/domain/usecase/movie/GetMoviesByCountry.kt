package com.example.domain.usecase.movie

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesByCountry @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(countrySlug : String,limit : Int = 20) = movieRepository.getMoviesByCountry(countrySlug,limit)
}