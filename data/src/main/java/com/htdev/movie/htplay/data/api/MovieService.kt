package com.htdev.movie.htplay.data.api

import com.htdev.movie.htplay.data.common.Api
import com.htdev.movie.htplay.data.entities.dto.Movie
import com.htdev.movie.htplay.data.entities.dto.MovieByGenre
import com.htdev.movie.htplay.data.entities.dto.MovieDetail
import com.htdev.movie.htplay.data.entities.dto.MovieDetailPage
import com.htdev.movie.htplay.data.entities.dto.MovieResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * @author hwa..
 */

interface MovieService {
    //api func write here

    @GET(Api.ENDPOINT_ALL_FILM)
    suspend fun getMoviesByPage(
        @Query("page") page : Int
    ) : Movie

    @GET(Api.ENDPOINT_FILM_GENRES)
    suspend fun getMoviesByGenre(//get theo the loai
        @Path("type_list") type : String,
        @Query("page") page : Int,
        @Query("limit") limit : Int = 20,
    ) : MovieResponse

    @GET(Api.ENDPOINT_FILM_GENRES)
    suspend fun getMoviesByGenreAndCountry(//get theo the loai
        @Path("type_list") type : String,
        @Query("page") page : Int,
        @Query("country") country : String,
        @Query("limit") limit : Int = 20,
    ) : MovieResponse


    @GET(Api.ENDPOINT_FILM_SERIES)
    suspend fun getMoviesByType(//get theo the loai nhung la series,TV show....
        @Path("type_list") type : String,
        @Query("page") page : Int,
        @Query("limit") limit : Int = 20,
    ) : MovieResponse


    @GET(Api.ENDPOINT_FILM_COUNTRY)
    suspend fun getMoviesByCountry(//get theo the loai nhung la series,TV show....
        @Path("type_list") type : String,
        @Query("page") page : Int,
        @Query("limit") limit : Int = 20,
    ) : MovieResponse

    //tim kiem film
    @GET(Api.ENDPOINT_SEARCH_FILM)
    suspend fun getMoviesBySearch(
        @Query("keyword") keyword : String,
        @Query("page") page : Int,
        @Query("limit") limit : Int = 20,
    ) : MovieResponse

    @GET(Api.ENDPOINT_SEARCH_FILM)
    suspend fun getMoviesBySearch(
        @Query("keyword") keyword : String,
        @Query("page") page : Int,
        @Query("limit") limit : Int = 20,
        @Query("category") category : String,
    ) : MovieResponse

    @GET(Api.ENDPOINT_MOVIE_DETAIL)
    suspend fun getMovieDetail(
        @Path("slug") slug : String
    ) : MovieDetailPage
}