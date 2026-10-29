package com.example.domain.usecase.auth

import com.example.domain.repository.AuthenticationRepository
import javax.inject.Inject

/**
 * @author hwa..
 */
class GetUserSignIn @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) {
    suspend operator fun invoke() = authenticationRepository.getUserSignIn()
}