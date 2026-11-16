package com.htdev.movie.htplay.domain.usecase.auth

import com.htdev.movie.htplay.domain.repository.AuthenticationRepository
import javax.inject.Inject

class HasCachedSession @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
) {
    operator fun invoke(): Boolean = authenticationRepository.hasCachedSession()
}
