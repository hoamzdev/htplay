package com.htdev.movie.htplay.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.usecase.movie.SaveGenresFavorite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import com.htdev.movie.htplay.domain.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val saveGenresFavoriteUC: SaveGenresFavorite
) : ViewModel(){
    private val _saveResult = MutableStateFlow<Result<Boolean>>(Result.Idle)
    val saveResult = _saveResult.asStateFlow()

    fun saveGenresFavorite(Genres : List<String>){
        if (_saveResult.value is Result.Loading) return
        _saveResult.value = Result.Loading
        viewModelScope.launch {
            _saveResult.value = try {
                saveGenresFavoriteUC(Genres)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = Result.Idle
    }
}
