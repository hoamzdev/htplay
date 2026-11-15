package com.htdev.movie.htplay.home.favorite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.common.map
import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.domain.usecase.movie.DeleteFavoriteMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetListFavoriteMovies
import com.htdev.movie.htplay.util.searchKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteUiState(
    val favorites: Result<List<Favorite>> = Result.Loading,
    val query: String = "",
    val isSearchOpen: Boolean = false,
)

@HiltViewModel
class FavoriteMovieViewModel @Inject constructor(
    private val deleteFavoriteMovie: DeleteFavoriteMovie,
    private val getListFavoriteMovies: GetListFavoriteMovies,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(){

    private val allFavorites = MutableStateFlow<Result<List<Favorite>>>(Result.Loading)
    private val query = savedStateHandle.getStateFlow(QUERY, "")
    val isSearchOpen = savedStateHandle.getStateFlow(SEARCH_OPEN, false)
    val uiState = combine(allFavorites, query, isSearchOpen) { favorites, keyword, searchOpen ->
        val searchKey = keyword.searchKey()
        FavoriteUiState(
            favorites = favorites.map { items ->
                if (searchKey.isEmpty()) items
                else items.filter { it.name.searchKey().contains(searchKey) || it.slugMovie.searchKey().contains(searchKey) }
            },
            query = keyword,
            isSearchOpen = searchOpen,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        FavoriteUiState(query = query.value, isSearchOpen = isSearchOpen.value),
    )


    init {
        fetchAllFavorites()
    }


    private fun fetchAllFavorites(){
        viewModelScope.launch {
            getListFavoriteMovies()
                .catch { allFavorites.value = Result.Error(it) }
                .collect { items -> allFavorites.value = Result.Success(items) }
        }
    }

    fun setQuery(value: String) {
        savedStateHandle[QUERY] = value
    }

    fun openSearch() {
        savedStateHandle[SEARCH_OPEN] = true
    }

    fun closeSearch() {
        savedStateHandle[QUERY] = ""
        savedStateHandle[SEARCH_OPEN] = false
    }

    fun deleteFavorite(slug: String){
        viewModelScope.launch {
            deleteFavoriteMovie(slug)
        }
    }

    private companion object {
        const val QUERY = "favorite_search_query"
        const val SEARCH_OPEN = "favorite_search_open"
    }
}
