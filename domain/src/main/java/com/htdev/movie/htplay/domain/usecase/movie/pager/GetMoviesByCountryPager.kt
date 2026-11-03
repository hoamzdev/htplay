package com.htdev.movie.htplay.domain.usecase.movie.pager

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

class GetMoviesByCountryPager @Inject constructor(
    private val movieRepository: MovieRepository,
) {
    operator fun invoke(countrySlug: String, limit: Int = 20) =
        movieRepository.getMoviesByCountryPager(countrySlug, limit)
}
