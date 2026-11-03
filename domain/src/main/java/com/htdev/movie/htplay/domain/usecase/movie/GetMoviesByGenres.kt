package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesByGenres @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(Genre : String,limit : Int = 20) = movieRepository.getMoviesByGenre(Genre,limit)

}