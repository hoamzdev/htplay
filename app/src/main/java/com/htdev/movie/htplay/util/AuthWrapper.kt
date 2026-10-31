package com.htdev.movie.htplay.util

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.htdev.movie.htplay.R

object AuthWrapper {
    // Pass an Activity context so Credential Manager can display the account picker.
    suspend fun createCredential(context: Context): Credential {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.google_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return CredentialManager.create(context)
            .getCredential(context = context, request = request)
            .credential
    }
}
