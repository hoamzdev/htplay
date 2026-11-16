package com.htdev.movie.htplay.auth

import androidx.lifecycle.ViewModel
import com.htdev.movie.htplay.domain.usecase.auth.GetUserSignIn
import com.htdev.movie.htplay.domain.usecase.auth.HasCachedSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val getUserSignIn: GetUserSignIn,
    private val hasCachedSession: HasCachedSession,
) : ViewModel() {
    fun isLoggedInLocally(): Boolean = hasCachedSession()

    suspend fun validateSession() = getUserSignIn()
}
