package com.htdev.movie.htplay.util

import android.content.Context
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.data.db.DataGenre
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.Genre

fun DataGenre.getLocalizedGenres(context: Context): List<Genre> =
    genres.map { it.copy(name = context.getGenreName(it.slug)) }

fun DataGenre.getExploreCountries(context: Context): List<Country> =
    exploreCountries.map { it.copy(name = context.getCountryName(it.slug)) }

fun Context.getGenreName(slug: String): String {
    val resource = when (slug) {
        "hanh-dong" -> R.string.genre_action
        "mien-tay" -> R.string.genre_western
        "tre-em" -> R.string.genre_kids
        "lich-su" -> R.string.genre_history
        "co-trang" -> R.string.genre_costume
        "chien-tranh" -> R.string.genre_war
        "vien-tuong" -> R.string.genre_sci_fi
        "kinh-di" -> R.string.genre_horror
        "tai-lieu" -> R.string.genre_documentary
        "bi-an" -> R.string.genre_mystery
        "phim-18" -> R.string.genre_adult
        "tinh-cam" -> R.string.genre_romance
        "tam-ly" -> R.string.genre_psychological
        "the-thao" -> R.string.genre_sports
        "phieu-luu" -> R.string.genre_adventure
        "am-nhac" -> R.string.genre_music
        "gia-dinh" -> R.string.genre_family
        "hoc-duong" -> R.string.genre_school
        "hai-huoc" -> R.string.genre_comedy
        "hinh-su" -> R.string.genre_crime
        "vo-thuat" -> R.string.genre_martial_arts
        "khoa-hoc" -> R.string.genre_science
        "than-thoai" -> R.string.genre_mythology
        "chinh-kich" -> R.string.genre_drama
        "kinh-dien" -> R.string.genre_classic
        "phim-ngan" -> R.string.genre_short_film
        else -> return slug
    }
    return getString(resource)
}

fun Context.getCountryName(slug: String): String {
    val resource = when (slug) {
        "thai-lan" -> R.string.country_thailand
        "hong-kong" -> R.string.country_hong_kong
        "an-do" -> R.string.country_india
        "canada" -> R.string.country_canada
        "tay-ban-nha" -> R.string.country_spain
        "ba-lan" -> R.string.country_poland
        "dai-loan" -> R.string.country_taiwan
        "anh" -> R.string.country_uk
        "nga" -> R.string.country_russia
        "uc" -> R.string.country_australia
        "y" -> R.string.country_italy
        "na-uy" -> R.string.country_norway
        else -> return slug
    }
    return getString(resource)
}
