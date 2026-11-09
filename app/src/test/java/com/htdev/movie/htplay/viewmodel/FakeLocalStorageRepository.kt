package com.htdev.movie.htplay.viewmodel

import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLocalStorageRepository : LocalStorageRepository {
    val movies = MutableStateFlow<List<ContinueWatching>>(emptyList())
    val saved = mutableListOf<ContinueWatching>()
    var save: suspend (ContinueWatching) -> Unit = { saved.add(it) }

    override fun saveCurrentTabSearch(idTab: Int) = Unit
    override fun getCurrentTabSearch() = -1
    override suspend fun saveContinueWatching(continueWatching: ContinueWatching) = save(continueWatching)
    override suspend fun deleteContinueWatching(id: Long) = Unit
    override suspend fun getListContinueWatching() = movies
}
