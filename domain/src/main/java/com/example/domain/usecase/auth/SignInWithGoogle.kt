package com.example.domain.usecase.auth

import androidx.credentials.Credential
import com.example.domain.repository.AuthenticationRepository
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) {
    suspend operator fun invoke(credential: Credential) = authenticationRepository.signInWithGoogle(credential)
}