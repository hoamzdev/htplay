package com.htdev.movie.htplay.domain.repository
import androidx.paging.PagingData
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.DataMovieDetail
import com.htdev.movie.htplay.domain.entities.DataMoviePlayer
import com.htdev.movie.htplay.domain.entities.Genre
import com.htdev.movie.htplay.domain.entities.PosterMovie
import com.htdev.movie.htplay.domain.entities.PosterMovieHome
import kotlinx.coroutines.flow.Flow

/**
 * @author hwa..
 */
interface MovieRepository {
    suspend fun saveGenresFavorite(Genres : List<String>) : Result<Boolean>
    //1): lay ra 1 page film moi nhat ko can phan trang -> do nen poster
    //du lieu ko phan trang o man hinh homa
    suspend fun getMoviesByPage() : Result<List<PosterMovieHome>>
    suspend fun getMoviesByGenre(Genre : String,limit : Int) : Result<List<PosterMovie>>
    suspend fun getMovies() : Result<List<PosterMovie>>
    suspend fun getMoviesSeries(Genre : String,limit : Int) : Result<List<PosterMovie>>
    suspend fun getMoviesByCountry(countrySlug : String,limit : Int) : Result<List<PosterMovie>>
    suspend fun getMoviesByGenreAndCountry(countrySlug : String,GenreSlug : String,limit : Int) : Result<List<PosterMovie>>
    //lay ve danh sach Genres user da chon trong onboarding
    suspend fun getSavedGenres() : Result<List<String>>

    //du lieu phan trang trong detail screen
    //2): loc ra ds film theo Genres ma user chon(that ra la lay Genre dau tien ma user chon) -> selected for you
    fun getMoviesByGenrePager(Genre : String,limit : Int) : Flow<PagingData<PosterMovie>>
    fun getMoviesByCountryPager(countrySlug: String, limit: Int) : Flow<PagingData<PosterMovie>>
    //3): lay ra nhung film moi nhat co phan trang -> Latest Release
    fun getMoviesPager() : Flow<PagingData<PosterMovie>>
    //4): lay ra ds tong hop phim bo,tv-shows.. -> Series
    fun getMoviesSeriesPager(Genre : String,limit : Int) : Flow<PagingData<PosterMovie>>
    //5): lay du lieu tu viec nhap keyword
    //co phan trang
    fun getMoviesBySearch(keyword : String,Genre : String,limit : Int) : Flow<PagingData<PosterMovie>>

    //lay du lieu movie page detail
    suspend fun getMovieDetail(slug : String) : Result<DataMovieDetail>
    suspend fun getDataMoviePlayer(slug : String) : Result<DataMoviePlayer>

    suspend fun getCountries() : Result<List<Country>>

    suspend fun getGenres() : Result<List<Genre>>
}
