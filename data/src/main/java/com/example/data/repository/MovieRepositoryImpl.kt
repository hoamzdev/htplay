package com.example.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.data.api.MovieService
import com.example.data.db.DataGenre
import com.example.data.entities.dto.UserData
import com.example.data.entities.dto.mapToDataMovieDetail
import com.example.data.entities.dto.mapToDataMoviePlayer
import com.example.data.entities.dto.mapToPosterMovie
import com.example.data.entities.dto.mapToPosterMovieHome
import com.example.data.exception.AppException
import com.example.domain.common.DispatcherProvide
import com.example.domain.common.Result
import com.example.domain.entities.DataMovieDetail
import com.example.domain.entities.DataMoviePlayer
import com.example.domain.entities.Genre
import com.example.domain.entities.PosterMovie
import com.example.domain.entities.PosterMovieHome
import com.example.domain.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import javax.inject.Inject
import dagger.Lazy

/**
 * @author hwa..
 */
class MovieRepositoryImpl @Inject constructor(
    private val movieService: MovieService,
    private val dispatcherProvide: DispatcherProvide,
    private val firestoreProvider: Lazy<FirebaseFirestore>,
    private val authProvider: Lazy<FirebaseAuth>
) : MovieRepository {
    private val auth: FirebaseAuth get() = authProvider.get()
    private val firestore: FirebaseFirestore get() = firestoreProvider.get()

    //lay tat ca cac genre
    override suspend fun getMovieGenres(): Result<List<Genre>> {
        return withContext(dispatcherProvide.io) {
            Result.Success(DataGenre.genres)
        }
    }

    //ham nay chi goi 1 lan luc onboarding lan dau vao app
    override suspend fun saveGenresFavorite(genres: List<String>): Result<Boolean> {
        return try {
            val user = checkNotNull(auth.currentUser) { "Sign in before accessing saved genres" }
            val uid = user.uid
            firestore.collection("users").document(uid).set(
                mapOf("genresFavorite" to genres), SetOptions.merge()
            ).await()
            Result.Success(true)
        } catch (e: FirebaseNetworkException) {
            Result.Error(AppException.NetworkException())
        } catch (e: Exception) {
            if(e is CancellationException) throw e
            Result.Error(e)
        }
    }

    //get movie by page ko phan trang -> do nen poster Home
    override suspend fun getMoviesByPage(): Result<List<PosterMovieHome>> {
        return withContext(dispatcherProvide.io) {
            try {
                coroutineScope {
                    val dataOfPage1 = async { movieService.getMoviesByPage(1) }//chay //
                    val dataOfPage2 = async { movieService.getMoviesByPage(2) }//
                    val movies1 = dataOfPage1.await().items
                    val movies2 = dataOfPage2.await().items
                    val movies = movies1 + movies2
                    if (movies.isNotEmpty()) {
                        Result.Success(movies.distinctBy { movie -> movie.id }
                            .map { movie -> movie.mapToPosterMovieHome() }
                            .sortedByDescending { movieHome -> movieHome.vote.toDoubleOrNull() ?: 0.0 }
                            .take(10))
                    } else {
                        Result.Success(emptyList())
                    }
                }
            } catch (e: UnknownHostException) {
                Result.Error(e)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(e)
            }
        }
    }

    //lay film them the loai co phan trang
    override fun getMoviesByGenrePager(genre: String, limit: Int): Flow<PagingData<PosterMovie>> {
        return Pager(
            config = PagingConfig(
                pageSize = limit, initialLoadSize = limit, enablePlaceholders = false, prefetchDistance = 4
            ), pagingSourceFactory = {
                MoviePagingSource { page ->
                    val response =
                        movieService.getMoviesByGenre(type = genre, page = page, limit = limit)
                    val movies = response.data.items ?: emptyList()
                    movies.map { movie -> movie.mapToPosterMovie() }
                }
            }).flow
    }

    //lay tat ca cac film ms nhat co phan trang
    override fun getMoviesPager(): Flow<PagingData<PosterMovie>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10, initialLoadSize = 10, enablePlaceholders = false,//hien thi space cho item chua load
                prefetchDistance = 6
            ), pagingSourceFactory = {
                MoviePagingSource { page ->
                    val response = movieService.getMoviesByPage(page)
                    val movies = response.items ?: emptyList()
                    movies.map { movie -> movie.mapToPosterMovie() }
                }
            }).flow
    }

    //lay film theo the loai (co phan trang)
    override fun getMoviesSeriesPager(genre: String, limit: Int): Flow<PagingData<PosterMovie>> {
        return Pager(
            config = PagingConfig(
                pageSize = limit, initialLoadSize = limit, enablePlaceholders = false, prefetchDistance = 4
            ), pagingSourceFactory = {
                MoviePagingSource { page ->
                    val response =
                        movieService.getMoviesByType(type = genre, page = page, limit = limit)
                    val movies = response.data.items ?: emptyList()
                    movies.map { movie -> movie.mapToPosterMovie() }
                }
            }).flow
    }

    //lay films nhat ko phan trang
    override suspend fun getMovies(): Result<List<PosterMovie>> {
        return withContext(dispatcherProvide.io) {
            try {
                val dataOfPage = movieService.getMoviesByPage(1)
                val movies = dataOfPage.items
                if (movies.isNotEmpty()) {
                    Result.Success(movies.distinctBy { movie -> movie.id }
                        .map { movie -> movie.mapToPosterMovie() })
                } else {
                    Result.Success(emptyList())
                }
            } catch (e: UnknownHostException) {
                Log.e("TAG", e.message.toString())
                Result.Error(e)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(e)
            }
        }
    }

    //lay film theo kieu chuoi film -> ko phan trang
    override suspend fun getMoviesSeries(
        genre: String, limit: Int
    ): Result<List<PosterMovie>> {
        return withContext(dispatcherProvide.io) {
            try {
                val dataOfPage = movieService.getMoviesByType(type = genre, page = 1,limit = limit)
                val movies = dataOfPage.data.items
                if (movies.isNotEmpty()) {
                    Result.Success(movies.distinctBy { movie -> movie.id }
                        .map { movie -> movie.mapToPosterMovie() })
                } else {
                    Result.Success(emptyList())
                }
            } catch (e: UnknownHostException) {
                Log.e("TAG", e.message.toString())
                Result.Error(e)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(e)
            }
        }
    }

    //lay film theo the loai
    override suspend fun getMoviesByGenre(genre: String, limit: Int): Result<List<PosterMovie>> {
        return withContext(dispatcherProvide.io) {
            try {
                val dataOfPage = movieService.getMoviesByGenre(type = genre, page = 1,limit = limit)
                val movies = dataOfPage.data.items
                if (movies.isNotEmpty()) {
                    Result.Success(movies.distinctBy { movie -> movie.id }
                        .map { movie -> movie.mapToPosterMovie() })
                } else {
                    Result.Success(emptyList())
                }
            } catch (e: UnknownHostException) {
                Log.e("TAG", e.message.toString())
                Result.Error(e)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("Bug_here",e.message.toString())
                Result.Error(e)
            }
        }
    }

    override suspend fun getMoviesByCountry(
        countrySlug: String,
        limit: Int
    ): Result<List<PosterMovie>> {
        return withContext(dispatcherProvide.io){
            try{
                val response = movieService.getMoviesByCountry(type = countrySlug, page = 1, limit = limit)
                val movies = response.data.items ?: emptyList()
                if(movies.isNotEmpty()){
                    Result.Success(
                        movies.distinctBy {movie -> movie.id}
                            .map { movie -> movie.mapToPosterMovie() }
                    )
                }
                else{
                    Result.Success(emptyList())
                }
            }catch (e : UnknownHostException){
                Log.e("TAG", e.message.toString())
                Result.Error(e)
            }catch (e : Exception){
                if(e is CancellationException) throw e
                Result.Error(e)
            }
        }
    }

    override suspend fun getSavedGenres(): Result<List<String>> {
        return try{
            val currentUser = checkNotNull(auth.currentUser) { "Sign in before accessing saved genres" }
            val uid = currentUser.uid
            val data = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            val genres = data.toObject(UserData::class.java)?.genresFavorite ?: emptyList<String>()
            Result.Success(genres)
        }catch (e : FirebaseNetworkException){
            Log.e("Bug_here",e.message.toString())
            Result.Error(e)
        } catch (e : Exception){
            if(e is CancellationException) throw e
            Log.e("Bug_here",e.message.toString())
            Result.Error(e)
        }
    }

    //search
    override fun getMoviesBySearch(keyword: String,genre : String, limit: Int): Flow<PagingData<PosterMovie>> {
        return Pager(
            config = PagingConfig(
                pageSize = limit, initialLoadSize = limit,
                prefetchDistance = 4,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                MoviePagingSource{page ->
                    val response = if(genre == "All"){
                        movieService.getMoviesBySearch(keyword,page,limit)
                    }
                    else{
                        movieService.getMoviesBySearch(keyword,page,limit,genre)
                    }
                    val movies = response.data.items ?: emptyList()
                    movies.map { movie -> movie.mapToPosterMovie() }
                }
            }
        ).flow
    }


    override suspend fun getMovieDetail(slug: String): Result<DataMovieDetail> {
        return withContext(dispatcherProvide.io){
            try{
                val response = movieService.getMovieDetail(slug)
                val data = response.movie
                Result.Success(
                    data.mapToDataMovieDetail()
                )
            }catch (e: UnknownHostException) {
                Result.Error(e)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(e)
            }
        }
    }

    override suspend fun getDataMoviePlayer(slug: String): Result<DataMoviePlayer> {
        return withContext(dispatcherProvide.io){
            try{
                val response = movieService.getMovieDetail(slug)
                val data = response.episodes
                Result.Success(data.mapToDataMoviePlayer())
            }catch (e: UnknownHostException) {
                Result.Error(e)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(e)
            }
        }
    }

    override suspend fun getMoviesByGenreAndCountry(
        countrySlug: String,
        genreSlug: String,
        limit: Int
    ): Result<List<PosterMovie>> {
        return withContext(dispatcherProvide.io){
            try {
                val response = movieService.getMoviesByGenreAndCountry(type = genreSlug, country = countrySlug, page = 1,limit = limit)
                val movies = response.data.items ?: emptyList()
                Result.Success(
                    movies.map { movie -> movie.mapToPosterMovie() }
                )
            }catch (e: UnknownHostException) {
                Result.Error(e)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(e)
            }
        }
    }
}