package com.htdev.movie.htplay.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.usecase.movie.SaveGenresFavorite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val saveGenresFavoriteUC: SaveGenresFavorite
) : ViewModel(){


    fun saveGenresFavorite(genres : List<String>){
        viewModelScope.launch(Dispatchers.IO) {
            saveGenresFavoriteUC.invoke(genres)
        }
    }
}