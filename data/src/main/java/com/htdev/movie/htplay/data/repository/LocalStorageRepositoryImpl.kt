package com.htdev.movie.htplay.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.htdev.movie.htplay.data.common.ConstantKey
import com.htdev.movie.htplay.data.db.ContinueWatchingDao
import com.htdev.movie.htplay.data.db.FavoriteDao
import com.htdev.movie.htplay.data.entities.dto.mapToDB
import com.htdev.movie.htplay.data.entities.dto.mapToDomain
import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * @author hwa..
 */
class LocalStorageRepositoryImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val continueWatchingDao: ContinueWatchingDao,
    private val favoriteDao: FavoriteDao
) : LocalStorageRepository {
    override fun getCurrentTabSearch(): Int {
        return sharedPreferences
            .getInt(ConstantKey.TAB_SEARCH_CURRENT, -1)
    }

    override fun saveCurrentTabSearch(idTab: Int) {
        sharedPreferences.edit {
            putInt(ConstantKey.TAB_SEARCH_CURRENT, idTab)
        }
    }

    override suspend fun saveContinueWatching(continueWatching: ContinueWatching) {
        if (continueWatching.slugMovie.isBlank() || continueWatching.lengthCurrentEpisode <= 0 ||
            continueWatching.positionContinue < 1_000
        ) return
        if (continueWatching.positionContinue >= continueWatching.lengthCurrentEpisode) {
            continueWatchingDao.deleteBySlug(continueWatching.slugMovie)
        } else {
            continueWatchingDao.saveLatest(continueWatching.mapToDB())
        }
    }

    override suspend fun deleteContinueWatching(id: Long) {
        continueWatchingDao.deleteContinueWatching(id)
    }

    override fun getListContinueWatching(): Flow<List<ContinueWatching>> {
        return continueWatchingDao.getAllContinueWatching()
            .map { it -> it.map { it.mapToDomain() } }
    }

    override suspend fun saveFavorite(favorite: Favorite) {
        favoriteDao.saveFavoriteMovie(favorite.mapToDB())
    }

    override suspend fun deleteFavorite(id: Long) {
        favoriteDao.deleteFavorite(id)
    }

    override fun getListFavorites(): Flow<List<Favorite>> {
        return favoriteDao.getListFavorites().map { items ->
            items.map { it.mapToDomain() }
        }
    }
}
