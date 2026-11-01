package com.htdev.movie.htplay.intro

import androidx.lifecycle.ViewModel
import com.htdev.movie.htplay.domain.usecase.auth.GetUserSignIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class IntroViewModel @Inject constructor(
    private val getUserSignIn: GetUserSignIn
) : ViewModel(){

    suspend fun checkSessionLogin() = getUserSignIn()
}
