package com.example.domain.repository

/**
 * @author hwa..
 */
// useCase room and sharedPref
interface LocalStorageRepository {
    fun saveStateLogin()
    fun getStateLogin() : Boolean
    fun saveCurrentTabSearch(idTab : Int)
    fun getCurrentTabSearch() : Int
}