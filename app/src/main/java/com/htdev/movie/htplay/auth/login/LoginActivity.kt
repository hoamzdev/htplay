package com.htdev.movie.htplay.auth.login

import android.util.Patterns
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.htdev.movie.htplay.MainActivity
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.auth.sign_up.SignUpActivity
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityLoginBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.favorite.ChooseFavoritesActivity
import com.htdev.movie.htplay.util.AuthWrapper
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.widget.CustomToast.showToast
import com.htdev.movie.htplay.widget.gone
import com.htdev.movie.htplay.widget.onClickListener
import com.htdev.movie.htplay.widget.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    override fun createViewBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    private val loginViewModel: LoginViewModel by viewModels()

    override fun initView() {
        binding.edtEmail.doAfterTextChanged {
            binding.tilEmail.isErrorEnabled = false
        }
        binding.edtPassword.doAfterTextChanged {
            binding.tilPassword.isErrorEnabled = false
        }
    }

    override fun observerData() {
        launchRepeatOnStarted {
            loginViewModel.logState.collectLatest { state ->
                when (state) {
                    is Result.Idle -> Unit

                    is Result.Loading -> {
                        binding.pgLoading.visible()
                    }

                    is Result.Error -> {
                        binding.pgLoading.gone()
                        showToast(this@LoginActivity, state.throwable.message.toString())
                    }

                    is Result.Success -> {
                        binding.pgLoading.gone()
                        newActivity(ChooseFavoritesActivity::class.java)
                        finish()
                    }
                }
            }
        }
    }

    override fun handleClickEvent() {
        binding.apply {
            btnLogin.onClickListener {
                val isValid = validateLoginInputs()
                btnLogin.isEnabled = isValid
                if (isValid) {
                    handleLoginEmailPassword()
                }
            }

            tvSignUp.onClickListener {
                newActivity(SignUpActivity::class.java)
            }

            tvForgotPassw.onClickListener {
                newActivity(ForgotPassWActivity::class.java)
            }
            loginByGg.onClickListener {
                handleLoginByGoogle()
            }

            loginByGuest.onClickListener {
                handleLoginByGuest()
            }
        }
    }

    private fun validateLoginInputs(): Boolean {
        val email = binding.edtEmail.text?.toString().orEmpty().trim()
        val password = binding.edtPassword.text?.toString().orEmpty()

        binding.tilEmail.error = when {
            email.isEmpty() -> getString(R.string.login_error_email_required)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                getString(R.string.login_error_email_invalid)

            else -> null
        }
        binding.tilPassword.error = if (password.isEmpty()) {
            getString(R.string.login_error_password_required)
        } else {
            null
        }
        binding.tilEmail.isErrorEnabled = binding.tilEmail.error != null
        binding.tilPassword.isErrorEnabled = binding.tilPassword.error != null

        return when {
            binding.tilEmail.error != null -> {
                binding.edtEmail.requestFocus()
                false
            }

            binding.tilPassword.error != null -> {
                binding.edtPassword.requestFocus()
                false
            }

            else -> true
        }
    }

    private fun handleLoginEmailPassword() {
        val email = binding.edtEmail.text?.toString().orEmpty().trim()
        val password = binding.edtPassword.text?.toString().orEmpty()
        loginViewModel.loginWithEmailPassword(email, password)
    }

    private fun handleLoginByGuest() {
        loginViewModel.loginInAnonymously()
    }

    private fun handleLoginByGoogle() {
        lifecycleScope.launch {
            binding.loginByGg.isEnabled = false
            try {
                val credential = AuthWrapper.createCredential(this@LoginActivity)
                loginViewModel.signInWithGoogle(credential)
            } catch (e: GetCredentialCancellationException) {
                // Closing the account picker cancels this sign-in attempt.
            } catch (e: GetCredentialException) {
                showToast(this@LoginActivity, e.message ?: e.type)
            } finally {
                binding.loginByGg.isEnabled = true
            }
        }
    }

}
