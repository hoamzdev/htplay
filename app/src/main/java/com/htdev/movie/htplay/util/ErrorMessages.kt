package com.htdev.movie.htplay.util

import android.content.Context
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.htdev.movie.htplay.R
import com.htdev.movie.htplay.data.exception.AppException
import com.htdev.movie.htplay.domain.common.SessionExpiredException
import java.io.IOException

/** Resolve messages at display time so errors follow the current UI language. */
fun Context.getLocalizedErrorMessage(error: Throwable): String = getString(
    when (error) {
        is AppException.EmailInvalidException -> R.string.error_email_not_found
        is AppException.EmailAlreadyExistsException -> R.string.error_email_in_use
        is AppException.PasswordWrongException -> R.string.error_invalid_credentials
        is AppException.NetworkException, is FirebaseNetworkException, is IOException -> R.string.error_network
        is SessionExpiredException -> R.string.error_session_expired
        is NoCredentialException -> R.string.error_google_no_accounts
        is GetCredentialInterruptedException -> R.string.error_google_interrupted
        is GetCredentialProviderConfigurationException -> R.string.error_sign_in_unavailable
        is GetCredentialException -> R.string.error_google_sign_in
        is FirebaseTooManyRequestsException -> R.string.error_too_many_requests
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> R.string.login_error_email_invalid
            "ERROR_USER_NOT_FOUND" -> R.string.error_email_not_found
            "ERROR_EMAIL_ALREADY_IN_USE" -> R.string.error_email_in_use
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                R.string.error_invalid_credentials
            "ERROR_USER_DISABLED" -> R.string.error_account_disabled
            "ERROR_TOO_MANY_REQUESTS" -> R.string.error_too_many_requests
            "ERROR_OPERATION_NOT_ALLOWED" -> R.string.error_sign_in_unavailable
            "ERROR_WEAK_PASSWORD" -> R.string.error_weak_password
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> R.string.error_account_different_provider
            "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> R.string.error_session_expired
            else -> R.string.error_generic
        }
        else -> R.string.error_generic
    }
)
