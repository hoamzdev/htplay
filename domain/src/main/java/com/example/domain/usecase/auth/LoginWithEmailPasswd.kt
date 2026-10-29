package com.example.domain.usecase.auth

import com.example.domain.repository.AuthenticationRepository
import javax.inject.Inject

class LoginWithEmailPasswd @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) {
    suspend operator fun invoke(email: String, passwd: String) =
        authenticationRepository.loginWithEmailPasswd(email, passwd)
}