package com.htdev.movie.htplay.auth.sign_up

import androidx.credentials.Credential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.entities.User
import com.htdev.movie.htplay.domain.usecase.auth.CreateAccountWithEmailPasswd
import com.htdev.movie.htplay.domain.usecase.auth.SignInAnonymously
import com.htdev.movie.htplay.domain.usecase.auth.SignInWithGoogle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val createAccountWithEmailPasswdUC: CreateAccountWithEmailPasswd,
    private val signInWithGoogleUseCase: SignInWithGoogle,
    private val signInAnonymously: SignInAnonymously,
)  : ViewModel(){

    private val _createAccountState = MutableStateFlow<Result<User>>(Result.Idle)
    val createAccountState = _createAccountState.asStateFlow()

    fun createAccountWithEmailPasswd(email : String,passwd : String){
        viewModelScope.launch(Dispatchers.IO) {
            _createAccountState.value = Result.Loading
            _createAccountState.value = createAccountWithEmailPasswdUC(email, email,passwd)
        }
    }

    fun loginInAnonymously() {
        viewModelScope.launch(Dispatchers.IO) {
            _createAccountState.value = Result.Loading
            _createAccountState.value = signInAnonymously()
        }
    }

    fun signInWithGoogle(credential: Credential) {
        viewModelScope.launch(Dispatchers.IO) {
            _createAccountState.value = Result.Loading
            _createAccountState.value = signInWithGoogleUseCase(credential)
        }
    }

}
