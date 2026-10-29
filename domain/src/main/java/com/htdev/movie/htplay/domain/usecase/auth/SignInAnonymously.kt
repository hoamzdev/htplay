package com.htdev.movie.htplay.domain.usecase.auth

import com.htdev.movie.htplay.domain.repository.AuthenticationRepository
import javax.inject.Inject

class SignInAnonymously @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
){
    suspend operator fun invoke() = authenticationRepository.signInAnonymously()
}