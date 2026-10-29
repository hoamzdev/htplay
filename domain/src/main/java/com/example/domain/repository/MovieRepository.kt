package com.example.domain.repository
import androidx.paging.PagingData
import com.example.domain.common.Result
import com.example.domain.entities.DataMovieDetail
import com.example.domain.entities.DataMoviePlayer
import com.example.domain.entities.Genre
import com.example.domain.entities.PosterMovie
import com.example.domain.entities.PosterMovieHome
import kotlinx.coroutines.flow.Flow

/**
 * @author hwa..
 */
interface MovieRepository {
    suspend fun getMovieGenres() : Result<List<Genre>>
    suspend fun saveGenresFavorite(genres : List<String>) : Result<Boolean>

    //1): lay ra 1 page film moi nhat ko can phan trang -> do nen poster
    //du lieu ko phan trang o man hinh homa
    suspend fun getMoviesByPage() : Result<List<PosterMovieHome>>
    suspend fun getMoviesByGenre(genre : String,limit : Int) : Result<List<PosterMovie>>
    suspend fun getMovies() : Result<List<PosterMovie>>
    suspend fun getMoviesSeries(genre : String,limit : Int) : Result<List<PosterMovie>>
    suspend fun getMoviesByCountry(countrySlug : String,limit : Int) : Result<List<PosterMovie>>
    suspend fun getMoviesByGenreAndCountry(countrySlug : String,genreSlug : String,limit : Int) : Result<List<PosterMovie>>
    //lay ve danh sach genres user da chon trong onboarding
    suspend fun getSavedGenres() : Result<List<String>>

    //du lieu phan trang trong detail screen
    //2): loc ra ds film theo genres ma user chon(that ra la lay genre dau tien ma user chon) -> selected for you
    fun getMoviesByGenrePager(genre : String,limit : Int) : Flow<PagingData<PosterMovie>>
    //3): lay ra nhung film moi nhat co phan trang -> Latest Release
    fun getMoviesPager() : Flow<PagingData<PosterMovie>>
    //4): lay ra ds tong hop phim bo,tv-shows.. -> Series
    fun getMoviesSeriesPager(genre : String,limit : Int) : Flow<PagingData<PosterMovie>>
    //5): lay du lieu tu viec nhap keyword
    //co phan trang
    fun getMoviesBySearch(keyword : String,genre : String,limit : Int) : Flow<PagingData<PosterMovie>>

    //lay du lieu movie page detail
    suspend fun getMovieDetail(slug : String) : Result<DataMovieDetail>
    suspend fun getDataMoviePlayer(slug : String) : Result<DataMoviePlayer>
}