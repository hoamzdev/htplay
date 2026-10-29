package com.htdev.movie.htplay.intro

import android.view.View
import androidx.lifecycle.ViewModel
import com.htdev.movie.htplay.domain.usecase.auth.GetStateLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class IntroViewModel @Inject constructor(
    private val getStateLogin: GetStateLogin
) : ViewModel(){

    val isLogin = getStateLogin()

}