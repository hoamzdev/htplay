package com.htdev.movie.htplay.auth.login

import com.htdev.movie.htplay.widget.onClickListener
import android.util.Patterns
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityForgotPassWactivityBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class ForgotPassWActivity : BaseActivity<ActivityForgotPassWactivityBinding>() {

    private val loginViewModel : LoginViewModel by viewModels()
    override fun createViewBinding(): ActivityForgotPassWactivityBinding {
        return ActivityForgotPassWactivityBinding.inflate(layoutInflater)
    }
    override fun initView() {
        binding.edtEmail.doAfterTextChanged {
            binding.tilEmail.isErrorEnabled = false
        }
    }

    override fun handleClickEvent() {
        binding.apply {
            btnReset.onClickListener {
                val isValid = validateEmail()
                btnReset.isEnabled = isValid
                handleResetPassword()
            }
            btnLogin.onClickListener {
                finish()
            }
        }
    }

    override fun observerData() {
        launchRepeatOnStarted {
            loginViewModel.stateResetPasswd.collectLatest { state ->
                when(state){
                    is Result.Success -> {
                        showToastApp(state.data)
                        finish()
                    }
                    is Result.Error -> {
                        showToastApp(state.throwable.message.toString())
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleResetPassword(){
        val email = binding.edtEmail.text?.toString().orEmpty().trim()
        loginViewModel.resetPassword(email)
    }

    private fun validateEmail(): Boolean {
        val email = binding.edtEmail.text?.toString().orEmpty().trim()

        binding.tilEmail.error = when {
            email.isEmpty() -> getString(R.string.login_error_email_required)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                getString(R.string.login_error_email_invalid)
            else -> null
        }
        binding.tilEmail.isErrorEnabled = binding.tilEmail.error != null

        return if (binding.tilEmail.error != null) {
            binding.edtEmail.requestFocus()
            false
        } else {
            true
        }
    }
}
