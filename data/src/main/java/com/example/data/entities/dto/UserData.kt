package com.example.data.entities.dto

/**
 * @author hwa..
 */
//object du lieu user tren firestore

data class UserData(
    val genresFavorite : List<String> = emptyList()
)