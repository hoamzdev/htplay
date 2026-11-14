package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.entities.Favorite
import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject

class ToggleFavoriteMovie @Inject constructor(
    private val localStorageRepository: LocalStorageRepository,
) {
    suspend operator fun invoke(favorite: Favorite): Boolean =
        localStorageRepository.toggleFavorite(favorite)
}
