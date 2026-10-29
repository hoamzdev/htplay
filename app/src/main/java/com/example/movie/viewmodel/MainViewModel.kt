package com.example.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.common.Result
import com.example.domain.entities.PosterMovie
import com.example.domain.usecase.movie.GetMoviesLatest
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


@HiltViewModel
class MainViewModel @Inject constructor(
    private val getMovieLastest: GetMoviesLatest,
) : ViewModel() {

    private val _movies = MutableStateFlow<Result<List<PosterMovie>>>(Result.Loading)
    val movies: StateFlow<Result<List<PosterMovie>>> = _movies.asStateFlow()


    init {
        fetchMovieLastest()
    }

    private fun fetchMovieLastest() {
        viewModelScope.launch(Dispatchers.IO) {
            _movies.value = Result.Loading
            _movies.value = getMovieLastest()
        }
    }

}