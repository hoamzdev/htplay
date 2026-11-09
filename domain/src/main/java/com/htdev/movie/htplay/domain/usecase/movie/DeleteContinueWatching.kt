package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject

class DeleteContinueWatching @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
) {
    suspend operator fun invoke(id: Long) =
        localStorageRepository.deleteContinueWatching(id)
}