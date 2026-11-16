package com.htdev.movie.htplay.home.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.domain.common.onError
import com.htdev.movie.htplay.domain.common.onSuccess
import com.htdev.movie.htplay.domain.usecase.auth.LogoutUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingViewModel @Inject constructor(
    private val logoutUser: LogoutUser
) : ViewModel(){

    private var _stateUser : MutableStateFlow<Result<Boolean>> = MutableStateFlow(Result.Loading)
    val stateUser : StateFlow<Result<Boolean>> = _stateUser.asStateFlow()

    fun handleLogoutUser(){
        viewModelScope.launch {
            _stateUser.value = Result.Loading
            logoutUser().onSuccess {
                _stateUser.value = Result.Success(true)
            }.onError {
                _stateUser.value = Result.Error(it)
            }
        }
    }
}