package com.example.data.repository

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.example.data.exception.AppException
import com.example.domain.entities.User
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.common.Result
import kotlinx.coroutines.CancellationException
import dagger.Lazy
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class AuthenticationRepositoryImpl @Inject constructor(
    private val authProvider: Lazy<FirebaseAuth>,
) : AuthenticationRepository {
    private val auth: FirebaseAuth get() = authProvider.get()
    override suspend fun createAccountWithEmailPasswd(
        username: String, email: String, passwd: String
    ): Result<User> {
        return try {
            val response = auth.createUserWithEmailAndPassword(email, passwd).await()
            Result.Success(User(username, response.user?.uid))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.Error(AppException.EmailAlreadyExistsException())
        } catch (e: FirebaseNetworkException) {
            Result.Error(AppException.NetworkException())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(AppException.UnknownException())
        }
    }

    override suspend fun loginWithEmailPasswd(email: String, passwd: String): Result<User> {
        return try {
            val response = auth.signInWithEmailAndPassword(email, passwd).await()
            Result.Success(User(uid = response.user?.uid))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(AppException.EmailInvalidException())
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error(AppException.PasswordWrongException())
        } catch (e: FirebaseNetworkException) {
            Result.Error(AppException.NetworkException())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(AppException.UnknownException())
        }
    }

    override suspend fun signInWithGoogle(credential: Credential): Result<User> {
        return try {
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val result = auth.signInWithCredential(firebaseCredential).await()
                val user = result.user
                if (user != null) {
                    Result.Success(User(username = user.uid, uid = user.uid))
                } else {
                    Result.Error(Exception("Login fail. Please try again"))
                }
            } else Result.Error(Exception("Login fail. Please try again"))
        }
        catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(Exception("Login fail. Please try again"))
        }
    }

    override suspend fun resetPassword(email: String): Result<String> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.Success("Please check your registered Email")
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error(AppException.EmailInvalidException())
        } catch (e: FirebaseNetworkException) {
            Result.Error(AppException.NetworkException())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(AppException.UnknownException())
        }
    }

    override suspend fun signInAnonymously(): Result<User> {
        return try{
            val auth = auth.signInAnonymously().await()
            Result.Success(User(uid = auth.user?.uid, username = ""))
        }catch (e: FirebaseNetworkException) {
            Result.Error(AppException.NetworkException())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(AppException.UnknownException())
        }
    }

    override suspend fun getUserSignIn(): Result<User> {
        return try{
            val response = auth.currentUser
            if(response != null){
                val user = User(uid = response.uid)
                Result.Success(user)
            }
            else{
                Result.Error(AppException.UnknownException())
            }
        }catch (e : Exception){
            if (e is CancellationException) throw e
            Result.Error(AppException.UnknownException())
        }
    }

}