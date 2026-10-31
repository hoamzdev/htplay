package com.htdev.movie.htplay.auth.login

import androidx.credentials.Credential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.User
import com.htdev.movie.htplay.domain.usecase.auth.LoginWithEmailPasswd
import com.htdev.movie.htplay.domain.usecase.auth.SaveStateLogin
import com.htdev.movie.htplay.domain.usecase.auth.SignInAnonymously
import com.htdev.movie.htplay.domain.usecase.auth.SignInWithGoogle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithEmailPasswd: LoginWithEmailPasswd,
    private val signInWithGoogleUseCase: SignInWithGoogle,
    private val signInAnonymously: SignInAnonymously,
    private val saveStateLogin: SaveStateLogin,
) : ViewModel() {

    private var _loginState = MutableStateFlow<Result<User>>(Result.Idle)
    val logState : StateFlow<Result<User>> = _loginState.asStateFlow()

    fun loginWithEmailPassword(email: String, passwd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = Result.Loading
            _loginState.value = loginWithEmailPasswd(email, passwd)
        }
    }

    fun loginInAnonymously() {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = Result.Loading
            _loginState.value = signInAnonymously()
        }
    }

    fun signInWithGoogle(credential: Credential) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = Result.Loading
            _loginState.value = signInWithGoogleUseCase(credential)
        }
    }

    fun saveSession() = saveStateLogin()

}