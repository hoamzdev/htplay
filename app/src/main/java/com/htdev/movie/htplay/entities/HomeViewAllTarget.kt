package com.htdev.movie.htplay.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Describes what the destination should load, without carrying the Home movie list. */
@Parcelize
sealed class HomeViewAllTarget : Parcelable {
    data object Latest : HomeViewAllTarget()
    data object Recommended : HomeViewAllTarget()
    data class ByGenre(val slug: String) : HomeViewAllTarget()
    data class ByCountry(val slug: String) : HomeViewAllTarget()
    data object ContinueWatching : HomeViewAllTarget()
    data object Genres : HomeViewAllTarget()
    data object Countries : HomeViewAllTarget()
}
