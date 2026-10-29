package com.example.domain.repository

import androidx.credentials.Credential
import com.example.domain.entities.User
import com.example.domain.common.Result

interface AuthenticationRepository {
    suspend fun createAccountWithEmailPasswd(username : String,email : String,passwd : String) : Result<User>
    suspend fun loginWithEmailPasswd(email : String,passwd: String) : Result<User>
    suspend fun resetPassword(email : String) : Result<String>
    suspend fun signInWithGoogle(credential: Credential) : Result<User>
    suspend fun signInAnonymously() : Result<User>
    suspend fun getUserSignIn() : Result<User>
}
