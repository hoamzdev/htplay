package com.htdev.movie.htplay.auth.login

import com.htdev.movie.htplay.widget.onClickListener
import android.util.Patterns
import androidx.core.widget.doAfterTextChanged
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivityForgotPassWactivityBinding

class ForgotPassWActivity : BaseActivity<ActivityForgotPassWactivityBinding>() {
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
                validateEmail()
            }
            btnLogin.onClickListener {
                finish()
            }
        }
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
