package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject


class SaveFavoriteMovie @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
) {
    suspend operator fun invoke(favorite: Favorite) =
        localStorageRepository.saveFavorite(favorite)
}