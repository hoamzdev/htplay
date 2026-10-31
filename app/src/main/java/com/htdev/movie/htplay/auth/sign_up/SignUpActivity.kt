package com.htdev.movie.htplay.auth.sign_up

import com.htdev.movie.htplay.widget.onClickListener
import android.text.Annotation
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.util.Patterns
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.htdev.movie.htplay.MainActivity
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.base.BaseActivity
import com.htdev.movie.htplay.databinding.ActivitySignUpBinding
import com.htdev.movie.htplay.domain.common.Result
import com.htdev.movie.htplay.util.AuthWrapper
import com.htdev.movie.htplay.util.launchRepeatOnStarted
import com.htdev.movie.htplay.widget.gone
import com.htdev.movie.htplay.widget.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest


@AndroidEntryPoint
class SignUpActivity : BaseActivity<ActivitySignUpBinding>(){

    private val signUpViewModel : SignUpViewModel by viewModels()

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
        binding.apply {
            btnSignUp.onClickListener {
                val isValid = validateSignUpInputs()
                btnSignUp.isEnabled = isValid
                handleSignUpEmailPassword()
            }
            loginByGg.onClickListener {
                handleLoginByGoogle()
            }
            loginByGuest.onClickListener {
                handleLoginByGuest()
            }
        }
    }

    override fun observerData() {
        launchRepeatOnStarted {
            signUpViewModel.createAccountState.collectLatest { state ->
                when(state){
                    is Result.Success -> {
                        signUpViewModel.saveSession()
                        binding.pgLoading.gone()
                        newActivity(MainActivity::class.java)
                        finish()
                    }
                    is Result.Error -> {
                        binding.pgLoading.gone()
                        showToastApp(state.throwable.message.toString())
                    }
                    is Result.Idle -> Unit
                    is Result.Loading -> {
                        binding.pgLoading.visible()
                    }
                }
            }
        }
    }

    private fun handleSignUpEmailPassword(){
        val email = binding.edtEmail.text?.toString().orEmpty().trim()
        val password = binding.edtPassword.text?.toString().orEmpty()
        signUpViewModel.createAccountWithEmailPasswd(email, password)
    }

    private fun handleLoginByGoogle(){
        launchRepeatOnStarted {
            val credential = AuthWrapper.createCredential(this@SignUpActivity)
            signUpViewModel.signInWithGoogle(credential)
        }
    }

    private fun handleLoginByGuest() {
        signUpViewModel.loginInAnonymously()
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
