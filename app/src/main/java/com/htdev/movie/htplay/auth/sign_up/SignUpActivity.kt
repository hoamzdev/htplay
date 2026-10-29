package com.htdev.movie.htplay.auth.sign_up

import android.text.Annotation
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.util.Patterns
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivitySignUpBinding

class SignUpActivity : BaseActivity<ActivitySignUpBinding>(){
    override fun createViewBinding(): ActivitySignUpBinding {
        return ActivitySignUpBinding.inflate(layoutInflater)
    }
    override fun initView() {
        setupAgreementLinks()
        binding.edtEmail.doAfterTextChanged {
            binding.tilEmail.isErrorEnabled = false
        }
        binding.edtPassword.doAfterTextChanged {
            binding.tilPassword.isErrorEnabled = false
            binding.tilPasswordAgain.isErrorEnabled = false
        }
        binding.edtPasswordAgain.doAfterTextChanged {
            binding.tilPasswordAgain.isErrorEnabled = false
        }
        binding.cbRememberMe.setOnCheckedChangeListener { _, _ ->
            binding.cbRememberMe.isErrorShown = false
        }
    }

    override fun handleClickEvent() {
        binding.btnSignUp.setOnClickListener {
            validateSignUpInputs()
        }
    }

    private fun validateSignUpInputs(): Boolean {
        val email = binding.edtEmail.text?.toString().orEmpty().trim()
        val password = binding.edtPassword.text?.toString().orEmpty()
        val passwordAgain = binding.edtPasswordAgain.text?.toString().orEmpty()

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
        binding.tilPasswordAgain.error = when {
            passwordAgain.isEmpty() -> getString(R.string.signup_error_confirm_password_required)
            passwordAgain != password -> getString(R.string.signup_error_password_mismatch)
            else -> null
        }

        binding.tilEmail.isErrorEnabled = binding.tilEmail.error != null
        binding.tilPassword.isErrorEnabled = binding.tilPassword.error != null
        binding.tilPasswordAgain.isErrorEnabled = binding.tilPasswordAgain.error != null
        binding.cbRememberMe.isErrorShown = !binding.cbRememberMe.isChecked

        val firstInvalidInput = when {
            binding.tilEmail.error != null -> binding.edtEmail
            binding.tilPassword.error != null -> binding.edtPassword
            binding.tilPasswordAgain.error != null -> binding.edtPasswordAgain
            else -> null
        }
        if (firstInvalidInput != null) {
            firstInvalidInput.requestFocus()
            return false
        }
        if (!binding.cbRememberMe.isChecked) {
            binding.cbRememberMe.requestFocus()
            showToastApp(getString(R.string.signup_error_agreement_required))
            return false
        }
        return true
    }

    private fun setupAgreementLinks() {
        val text = SpannableString(getText(R.string.signup_agreement))
        val linkColor = ContextCompat.getColor(this, R.color.primary_color)
        text.getSpans(0, text.length, Annotation::class.java).forEach { annotation ->
            if (annotation.key != "action") return@forEach
            val onClick: () -> Unit = when (annotation.value) {
                "terms" -> ::openTerms
                "conditions" -> ::openConditions
                else -> return@forEach
            }
            text.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClick()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = linkColor
                }
            }, text.getSpanStart(annotation), text.getSpanEnd(annotation),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.tvAgreement.text = text
        binding.tvAgreement.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openTerms()  = Unit

    private fun openConditions() = Unit
}
