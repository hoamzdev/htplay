package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject

class GetListFavoriteMovies @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
) {
    operator fun invoke() =
        localStorageRepository.getListFavorites()
}