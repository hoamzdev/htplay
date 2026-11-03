package com.htdev.movie.htplay.data.entities.dto

data class CountryResponse(
    val data: CountryData
)


data class Country(
    val name : String,
    val slug : String
)

data class CountryData(
    val items : List<Country>
)
