package com.htdev.movie.htplay.viewmodel

import androidx.paging.PagingData
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.*
import com.htdev.movie.htplay.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeMovieRepository : MovieRepository {
    var savedGenres = listOf("Action", "tinh-cam")
    val requestedGenres = mutableListOf<String>()
    var GenreMovies: suspend (String) -> Result<List<PosterMovie>> = { Result.Success(emptyList()) }
    var countryMovies: suspend (String) -> Result<List<PosterMovie>> = { Result.Success(emptyList()) }
    var saveGenres: suspend (List<String>) -> Result<Boolean> = { Result.Success(true) }
    var genres: suspend () -> Result<List<Genre>> = { Result.Success(emptyList()) }
    var genrePager: (String) -> Flow<PagingData<PosterMovie>> = { emptyFlow() }
    var countryPager: (String) -> Flow<PagingData<PosterMovie>> = { emptyFlow() }
    var latestPager: () -> Flow<PagingData<PosterMovie>> = { emptyFlow() }
    var countries: suspend () -> Result<List<Country>> = { Result.Success(listOf(Country("China", "trung-quoc"))) }
    var latestMovies: suspend () -> Result<List<PosterMovie>> = { Result.Success(emptyList()) }
    var bannerMovies: suspend () -> Result<List<PosterMovieHome>> = { Result.Success(emptyList()) }
    var movieDetail: suspend (String) -> Result<DataMovieDetail> = { Result.Success(DataMovieDetail()) }

    override suspend fun saveGenresFavorite(Genres: List<String>) = saveGenres(Genres)
    override suspend fun getSavedGenres() = Result.Success(savedGenres)
    override suspend fun getMovies() = latestMovies()
    override suspend fun getMoviesByPage() = bannerMovies()
    override suspend fun getMoviesByGenre(Genre: String, limit: Int): Result<List<PosterMovie>> {
        requestedGenres += Genre
        return GenreMovies(Genre)
    }
    override suspend fun getMoviesByCountry(countrySlug: String, limit: Int) = countryMovies(countrySlug)
    override suspend fun getMoviesSeries(Genre: String, limit: Int) = Result.Success(emptyList<PosterMovie>())
    override suspend fun getMoviesByGenreAndCountry(countrySlug: String, GenreSlug: String, limit: Int) =
        Result.Success(emptyList<PosterMovie>())
    override suspend fun getCountries() = countries()
    override suspend fun getGenres() = genres()
    override suspend fun getMovieDetail(slug: String) = movieDetail(slug)
    override suspend fun getDataMoviePlayer(slug: String) = Result.Success(DataMoviePlayer())
    override fun getMoviesPager(): Flow<PagingData<PosterMovie>> = latestPager()
    override fun getMoviesByCountryPager(countrySlug: String, limit: Int): Flow<PagingData<PosterMovie>> = countryPager(countrySlug)
    override fun getMoviesByGenrePager(Genre: String, limit: Int): Flow<PagingData<PosterMovie>> = genrePager(Genre)
    override fun getMoviesSeriesPager(Genre: String, limit: Int): Flow<PagingData<PosterMovie>> = emptyFlow()
    override fun getMoviesBySearch(keyword: String, Genre: String, limit: Int): Flow<PagingData<PosterMovie>> = emptyFlow()
}
