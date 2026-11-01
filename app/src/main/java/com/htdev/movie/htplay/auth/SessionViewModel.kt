package com.htdev.movie.htplay.auth

import androidx.lifecycle.ViewModel
import com.htdev.movie.htplay.domain.usecase.auth.GetUserSignIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val getUserSignIn: GetUserSignIn,
) : ViewModel() {
    suspend fun validateSession() = getUserSignIn()
}
