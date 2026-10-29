package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesByCountry @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(countrySlug : String,limit : Int = 20) = movieRepository.getMoviesByCountry(countrySlug,limit)
}