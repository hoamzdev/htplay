package com.example.data.di

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.MovieService
import com.example.data.common.Api
import com.example.data.common.DispatcherProvideImpl
import com.example.data.repository.AuthenticationRepositoryImpl
import com.example.data.repository.LocalStorageRepositoryImpl
import com.example.data.repository.MovieRepositoryImpl
import com.example.domain.common.DispatcherProvide
import com.example.domain.repository.AuthenticationRepository
import com.example.domain.repository.LocalStorageRepository
import com.example.domain.repository.MovieRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun dispatchers(): DispatcherProvide = DispatcherProvideImpl(
        default = Dispatchers.Default, io = Dispatchers.IO, main = Dispatchers.Main
    )

    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("MovieSharedPref", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun httpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http_cache"), 50L * 1024 * 1024))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun movieService(client: OkHttpClient): MovieService = Retrofit.Builder()
        .baseUrl(Api.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MovieService::class.java)

    @Provides
    @Singleton
    fun firebaseApp(@ApplicationContext context: Context): FirebaseApp =
        FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: checkNotNull(FirebaseApp.initializeApp(context)) {
                "Configure Firebase for com.example.movie before using authentication or saved genres."
            }

    @Provides
    @Singleton
    fun firebaseAuth(app: FirebaseApp): FirebaseAuth = FirebaseAuth.getInstance(app)

    @Provides
    @Singleton
    fun firestore(app: FirebaseApp): FirebaseFirestore = FirebaseFirestore.getInstance(app)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun movies(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun authentication(impl: AuthenticationRepositoryImpl): AuthenticationRepository

    @Binds
    @Singleton
    abstract fun localStorage(impl: LocalStorageRepositoryImpl): LocalStorageRepository
}
