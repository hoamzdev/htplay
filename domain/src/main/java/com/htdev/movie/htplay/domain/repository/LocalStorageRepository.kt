package com.htdev.movie.htplay.domain.repository

import com.htdev.movie.htplay.domain.entities.ContinueWatching
import kotlinx.coroutines.flow.Flow

/**
 * @author hwa..
 */
// useCase room and sharedPref
interface LocalStorageRepository {
    fun saveCurrentTabSearch(idTab : Int)
    fun getCurrentTabSearch() : Int

    suspend fun saveContinueWatching(continueWatching: ContinueWatching)
    suspend fun deleteContinueWatching(id : Long)
    suspend fun getListContinueWatching() : Flow<List<ContinueWatching>>
}
