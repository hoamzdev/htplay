package com.htdev.movie.htplay.util

import android.content.Context
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.data.db.LocalMovieCatalog
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.Genre

fun LocalMovieCatalog.getLocalizedGenres(context: Context): List<Genre> =
    genres.map { it.copy(name = context.getGenreName(it.slug)) }

fun LocalMovieCatalog.getExploreCountries(context: Context): List<Country> =
    exploreCountries.map { it.copy(name = context.getCountryName(it.slug)) }

fun Context.getGenreName(slug: String): String {
    val resource = when (slug) {
        "hanh-dong" -> R.string.GenreLocal_action
        "mien-tay" -> R.string.GenreLocal_western
        "tre-em" -> R.string.GenreLocal_kids
        "lich-su" -> R.string.GenreLocal_history
        "co-trang" -> R.string.GenreLocal_costume
        "chien-tranh" -> R.string.GenreLocal_war
        "vien-tuong" -> R.string.GenreLocal_sci_fi
        "kinh-di" -> R.string.GenreLocal_horror
        "tai-lieu" -> R.string.GenreLocal_documentary
        "bi-an" -> R.string.GenreLocal_mystery
        "phim-18" -> R.string.GenreLocal_adult
        "tinh-cam" -> R.string.GenreLocal_romance
        "tam-ly" -> R.string.GenreLocal_psychological
        "the-thao" -> R.string.GenreLocal_sports
        "phieu-luu" -> R.string.GenreLocal_adventure
        "am-nhac" -> R.string.GenreLocal_music
        "gia-dinh" -> R.string.GenreLocal_family
        "hoc-duong" -> R.string.GenreLocal_school
        "hai-huoc" -> R.string.GenreLocal_comedy
        "hinh-su" -> R.string.GenreLocal_crime
        "vo-thuat" -> R.string.GenreLocal_martial_arts
        "khoa-hoc" -> R.string.GenreLocal_science
        "than-thoai" -> R.string.GenreLocal_mythology
        "chinh-kich" -> R.string.GenreLocal_drama
        "kinh-dien" -> R.string.GenreLocal_classic
        "phim-ngan" -> R.string.GenreLocal_short_film
        else -> return slug
    }
    return getString(resource)
}

fun Context.getCountryName(slug: String): String {
    val resource = when (slug) {
        "trung-quoc" -> R.string.country_china
        "han-quoc" -> R.string.country_south_korea
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
