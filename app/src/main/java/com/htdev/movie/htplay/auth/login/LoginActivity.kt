package com.htdev.movie.htplay.auth.login

import android.util.Patterns
import androidx.core.widget.doAfterTextChanged
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.auth.sign_up.SignUpActivity
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityLoginBinding

class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    override fun createViewBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.edtEmail.doAfterTextChanged {
            binding.tilEmail.isErrorEnabled = false
        }
        binding.edtPassword.doAfterTextChanged {
            binding.tilPassword.isErrorEnabled = false
        }
    }

    override fun initViewModel() {

    }

    override fun handleClickEvent() {
        binding.btnLogin.setOnClickListener {
            validateLoginInputs()
        }

        binding.tvSignUp.setOnClickListener {
            newActivity(SignUpActivity::class.java)
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
}
