package com.htdev.movie.htplay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.htdev.movie.htplay.data.entities.dto.FavoriteDB
import kotlinx.coroutines.flow.Flow


@Dao
abstract class FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun saveFavoriteMovie(favorite: FavoriteDB): Long

    @Query("DELETE FROM favorite WHERE slugMovie = :slug")
    abstract suspend fun deleteFavorite(slug: String): Int

    // SQLite rowid preserves the order of additions without a separate movie ID.
    @Query("SELECT * FROM favorite ORDER BY rowid DESC")
    abstract fun getListFavorites() : Flow<List<FavoriteDB>>


    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE slugMovie = :slug)")
    abstract fun checkMovieExistsFavorite(slug : String): Flow<Boolean>

    @Transaction
    open suspend fun toggleFavorite(favorite: FavoriteDB): Boolean {
        if (deleteFavorite(favorite.slugMovie) > 0) return false
        saveFavoriteMovie(favorite)
        return true
    }
}
