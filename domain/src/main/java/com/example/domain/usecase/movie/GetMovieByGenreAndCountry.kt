package com.example.domain.usecase.movie

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMovieByGenreAndCountry @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(genreSlug : String,countrySlug : String,limit : Int = 30) = movieRepository.getMoviesByGenreAndCountry(countrySlug,genreSlug,limit)
}