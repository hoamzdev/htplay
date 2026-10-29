package com.htdev.movie.htplay.domain.usecase.auth

import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class SaveStateLogin @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
) {
    operator fun invoke() = localStorageRepository.saveStateLogin()
}