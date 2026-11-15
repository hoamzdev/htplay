package com.htdev.movie.htplay.entities

import com.htdev.movie.htplay.domain.entities.Country

sealed class HomeItem {
    abstract val id : Int
    abstract val viewAllTarget: HomeViewAllTarget?

    data class ContinueWatchingSector(override val id: Int, val movies: List<ContinueWatching>) : HomeItem() {
        override val viewAllTarget: HomeViewAllTarget = HomeViewAllTarget.ContinueWatching
    }
    data class MovieSector(
        override val id: Int,
        val Genre: String,
        val movies: List<ItemMovie>,
        override val viewAllTarget: HomeViewAllTarget,
        val isLoading: Boolean = false,
        val hasError: Boolean = false,
    ) : HomeItem()

    data class CountrySector(
        override val id: Int,
        val countries: List<Country>,
        val isLoading: Boolean = false,
        val hasError: Boolean = false,
    ) : HomeItem() {
        override val viewAllTarget: HomeViewAllTarget = HomeViewAllTarget.Countries
    }

    data class TrailerSector(val recommendation: HomeRecommendation) : HomeItem() {
        override val id: Int = HT_RECOMMENDATION
        override val viewAllTarget: HomeViewAllTarget? = null
    }

    companion object {
        const val CONTINUE_WATCHING = 1
        const val LATEST_MOVIES = 2
        const val SELECTED_FOR_YOU = 3
        const val CHINESE_MOVIES = 4
        const val KOREAN_MOVIES = 5
        const val COUNTRIES = 7
        const val ACTION_MOVIES = 8
        const val ROMANCE_MOVIES = 9
        const val FAMILY_MOVIES = 10
        const val HT_RECOMMENDATION = 11
    }
}
