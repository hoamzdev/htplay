package com.htdev.movie.htplay.home.favorite

import androidx.lifecycle.ViewModel
import com.htdev.movie.htplay.domain.usecase.movie.DeleteFavoriteMovie
import com.htdev.movie.htplay.domain.usecase.movie.GetListFavoriteMovies
import javax.inject.Inject

class FavoriteMovieViewModel @Inject constructor(
    private val deleteFavoriteMovie: DeleteFavoriteMovie,
    private val getListFavoriteMovies: GetListFavoriteMovies
) : ViewModel(){

}