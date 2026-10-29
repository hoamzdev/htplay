package com.example.domain.usecase.movie.pager

import com.example.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetMoviesByGenrePager @Inject constructor(
    private val movieRepository: MovieRepository
){
    operator fun invoke(genre : String,limit : Int = 20) = movieRepository.getMoviesByGenrePager(genre,limit)
}