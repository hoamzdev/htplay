package com.htdev.movie.htplay.domain.usecase.auth

import androidx.credentials.Credential
import com.htdev.movie.htplay.domain.repository.AuthenticationRepository
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) {
    suspend operator fun invoke(credential: Credential) = authenticationRepository.signInWithGoogle(credential)
}