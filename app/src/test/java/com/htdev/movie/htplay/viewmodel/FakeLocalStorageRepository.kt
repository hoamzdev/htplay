package com.htdev.movie.htplay.viewmodel

import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeLocalStorageRepository : LocalStorageRepository {
    val movies = MutableStateFlow<List<ContinueWatching>>(emptyList())
    val saved = mutableListOf<ContinueWatching>()
    var save: suspend (ContinueWatching) -> Unit = { saved.add(it) }
    val favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favoriteSaves = mutableListOf<Favorite>()
    var saveFavorite: suspend (Favorite) -> Boolean = { favorite ->
        val exists = favorites.value.any { it.slugMovie == favorite.slugMovie }
        if (!exists) favorites.value += favorite
        !exists
    }
    val favoriteToggles = mutableListOf<Favorite>()
    var toggle: suspend (Favorite) -> Boolean = { favorite ->
        val exists = favorites.value.any { it.slugMovie == favorite.slugMovie }
        if (exists) favorites.value = favorites.value.filterNot { it.slugMovie == favorite.slugMovie }
        else saveFavorite.invoke(favorite)
        !exists
    }

    override fun saveCurrentTabSearch(idTab: Int) = Unit
    override fun getCurrentTabSearch() = -1
    override suspend fun saveContinueWatching(continueWatching: ContinueWatching) = save(continueWatching)
    override suspend fun deleteContinueWatching(id: Long) = Unit
    override fun getListContinueWatching() = movies
    override suspend fun saveFavorite(favorite: Favorite): Boolean {
        favoriteSaves.add(favorite)
        return saveFavorite.invoke(favorite)
    }
    override suspend fun deleteFavorite(slug: String) {
        favorites.value = favorites.value.filterNot { it.slugMovie == slug }
    }
    override suspend fun toggleFavorite(favorite: Favorite): Boolean {
        favoriteToggles.add(favorite)
        return toggle(favorite)
    }
    override fun getListFavorites() = favorites
    override fun isExistsInFavorite(slug: String) = favorites
        .map { items -> items.any { it.slugMovie == slug } }
        .distinctUntilChanged()
}
