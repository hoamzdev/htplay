package com.htdev.movie.htplay.language

import java.util.UUID


data class LanguageModel(
    val id : UUID = UUID.randomUUID(),
    val name : String,
    val code : String,
    val image : Int
)