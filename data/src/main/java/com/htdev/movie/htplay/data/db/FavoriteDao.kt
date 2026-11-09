package com.htdev.movie.htplay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.htdev.movie.htplay.data.entities.dto.FavoriteDB
import kotlinx.coroutines.flow.Flow


@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavoriteMovie(favorite: FavoriteDB)

    @Query("delete from favorite where id =:id")
    suspend fun deleteFavorite(id : Long)

    @Query("select * from favorite order by id desc")
    fun getListFavorites() : Flow<List<FavoriteDB>>
}