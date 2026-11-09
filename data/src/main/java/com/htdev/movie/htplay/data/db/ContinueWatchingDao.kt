package com.htdev.movie.htplay.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.htdev.movie.htplay.data.entities.dto.ContinueWatchingDB
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ContinueWatchingDao {

    @Query("select * from continue_watching order by id desc limit 5")
    abstract fun getAllContinueWatching(): Flow<List<ContinueWatchingDB>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveContinueWatching(continueWatching: ContinueWatchingDB)

    @Query("delete from continue_watching where id =:id")
    abstract suspend fun deleteContinueWatching(id: Long)

    @Query("delete from continue_watching where slugMovie = :slug")
    abstract suspend fun deleteBySlug(slug: String)

    @Query("delete from continue_watching where id not in (select id from continue_watching order by id desc limit 5)")
    abstract suspend fun trimHistory()

    @Transaction
    open suspend fun saveLatest(continueWatching: ContinueWatchingDB) {
        // Reinsert to move this movie to the top without creating a duplicate.
        deleteBySlug(continueWatching.slugMovie)
        saveContinueWatching(continueWatching.copy(id = null))
        trimHistory()
    }
}
