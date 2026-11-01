package com.htdev.movie.htplay.domain.repository

/**
 * @author hwa..
 */
// useCase room and sharedPref
interface LocalStorageRepository {
    fun saveCurrentTabSearch(idTab : Int)
    fun getCurrentTabSearch() : Int
}
