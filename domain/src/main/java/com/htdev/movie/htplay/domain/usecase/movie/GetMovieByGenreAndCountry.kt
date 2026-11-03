package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMovieByGenreAndCountry @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(GenreSlug : String,countrySlug : String,limit : Int = 30) = movieRepository.getMoviesByGenreAndCountry(countrySlug,GenreSlug,limit)
}