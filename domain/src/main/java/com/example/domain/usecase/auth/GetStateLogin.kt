package com.example.domain.usecase.auth

import com.example.domain.repository.LocalStorageRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetStateLogin @Inject constructor(
    private val localStorageRepository: LocalStorageRepository
) {
    operator fun invoke() = localStorageRepository.getStateLogin()
}