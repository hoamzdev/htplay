package com.htdev.movie.htplay.domain.usecase.movie

import com.htdev.movie.htplay.domain.entities.ContinueWatching
import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject

class SaveContinueWatching @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
) {
    suspend operator fun invoke(continueWatching: ContinueWatching) =
        localStorageRepository.saveContinueWatching(continueWatching)
}