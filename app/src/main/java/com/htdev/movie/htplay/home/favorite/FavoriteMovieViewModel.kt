package com.htdev.movie.htplay.home.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.domain.usecase.movie.DeleteFavoriteMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetListFavoriteMovies
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteMovieViewModel @Inject constructor(
    private val deleteFavoriteMovie: DeleteFavoriteMovie,
    private val getListFavoriteMovies: GetListFavoriteMovies
) : ViewModel(){

    private var _favorites : MutableStateFlow<Result<List<Favorite>>> = MutableStateFlow(Result.Idle)
    val favorites : StateFlow<Result<List<Favorite>>> = _favorites.asStateFlow()


    init {
        fetchAllFavorites()
    }


    private fun fetchAllFavorites(){
        viewModelScope.launch {
            _favorites.value = Result.Loading
            getListFavoriteMovies().collect { items ->
                _favorites.value = Result.Success(items)
            }
        }
    }


    fun deleteFavorite(slug: String){
        viewModelScope.launch {
            deleteFavoriteMovie(slug)
        }
    }

}
