package com.htdev.movie.htplay.util

import android.content.Context
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.data.db.LocalMovieCatalog
import com.htdev.movie.htplay.domain.entities.Country
import com.htdev.movie.htplay.domain.entities.Genre
import java.util.Locale

fun LocalMovieCatalog.getLocalizedGenres(context: Context): List<Genre> =
    genres.map { it.copy(name = context.getGenreName(it.slug)) }


fun Context.getGenreName(slug: String): String {
    // Older cached movie metadata contains English genre names instead of slugs.
    val resource = when (slug.trim().lowercase(Locale.ROOT)) {
        "", "unknown" -> R.string.metadata_unknown
        "hanh-dong", "action" -> R.string.GenreLocal_action
        "hoat-hinh", "animation" -> R.string.GenreLocal_animation
        "mien-tay", "western" -> R.string.GenreLocal_western
        "tre-em", "kids" -> R.string.GenreLocal_kids
        "lich-su", "history" -> R.string.GenreLocal_history
        "co-trang", "costume" -> R.string.GenreLocal_costume
        "chien-tranh", "war" -> R.string.GenreLocal_war
        "vien-tuong", "sci-fi" -> R.string.GenreLocal_sci_fi
        "kinh-di", "horror" -> R.string.GenreLocal_horror
        "tai-lieu", "documentary" -> R.string.GenreLocal_documentary
        "bi-an", "mystery" -> R.string.GenreLocal_mystery
        "phim-18", "adult" -> R.string.GenreLocal_adult
        "tinh-cam", "romance" -> R.string.GenreLocal_romance
        "tam-ly", "psychological" -> R.string.GenreLocal_psychological
        "the-thao", "sports" -> R.string.GenreLocal_sports
        "phieu-luu", "adventure" -> R.string.GenreLocal_adventure
        "am-nhac", "music" -> R.string.GenreLocal_music
        "gia-dinh", "family" -> R.string.GenreLocal_family
        "hoc-duong", "school" -> R.string.GenreLocal_school
        "hai-huoc", "comedy" -> R.string.GenreLocal_comedy
        "hinh-su", "crime" -> R.string.GenreLocal_crime
        "vo-thuat", "martial arts" -> R.string.GenreLocal_martial_arts
        "khoa-hoc", "science" -> R.string.GenreLocal_science
        "than-thoai", "mythology" -> R.string.GenreLocal_mythology
        "chinh-kich", "drama" -> R.string.GenreLocal_drama
        "kinh-dien", "classic" -> R.string.GenreLocal_classic
        "phim-ngan", "short film" -> R.string.GenreLocal_short_film
        else -> return slug
    }
    return getString(resource)
}

fun Context.getCountryName(slug: String): String {
    val resource = when (slug.trim().lowercase(Locale.ROOT)) {
        "", "unknown" -> R.string.metadata_unknown
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
        "brazil" -> R.string.country_brazil
        "bo-dao-nha" -> R.string.country_portugal
        "chau-phi" -> R.string.country_africa
        "ha-lan" -> R.string.country_netherlands
        "indonesia" -> R.string.country_indonesia
        "malaysia" -> R.string.country_malaysia
        "mexico" -> R.string.country_mexico
        "nam-phi" -> R.string.country_south_africa
        "nhat-ban" -> R.string.country_japan
        "philippines" -> R.string.country_philippines
        "phap" -> R.string.country_france
        "quoc-gia-khac" -> R.string.country_other
        "tho-nhi-ky" -> R.string.country_turkey
        "thuy-si" -> R.string.country_switzerland
        "thuy-dien" -> R.string.country_sweden
        "uae" -> R.string.country_uae
        "ukraina" -> R.string.country_ukraine
        "viet-nam" -> R.string.country_vietnam
        "au-my" -> R.string.country_europe_america
        "dan-mach" -> R.string.country_denmark
        "duc" -> R.string.country_germany
        "a-rap-xe-ut" -> R.string.country_saudi_arabia
        else -> return slug
    }
    return getString(resource)
}
