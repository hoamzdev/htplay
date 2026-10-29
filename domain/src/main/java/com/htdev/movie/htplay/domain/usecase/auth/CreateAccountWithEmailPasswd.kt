package com.htdev.movie.htplay.domain.usecase.auth

import com.htdev.movie.htplay.domain.repository.AuthenticationRepository
import javax.inject.Inject

class CreateAccountWithEmailPasswd @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) {
    suspend operator fun invoke(username : String,email : String,passwd : String) =
        authenticationRepository.createAccountWithEmailPasswd(username,email,passwd)
}