package com.htdev.movie.htplay.data.di

import android.content.Context
import android.content.SharedPreferences
import com.htdev.movie.htplay.data.api.MovieService
import com.htdev.movie.htplay.data.common.Api
import com.htdev.movie.htplay.data.common.DispatcherProvideImpl
import com.htdev.movie.htplay.data.repository.AuthenticationRepositoryImpl
import com.htdev.movie.htplay.data.repository.LocalStorageRepositoryImpl
import com.htdev.movie.htplay.data.repository.MovieRepositoryImpl
import com.htdev.movie.htplay.domain.common.DispatcherProvide
import com.htdev.movie.htplay.domain.repository.AuthenticationRepository
import com.htdev.movie.htplay.domain.repository.LocalStorageRepository
import com.htdev.movie.htplay.domain.repository.MovieRepository
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
                "Configure Firebase for com.htdev.movie.htplay before using authentication or saved Genres."
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
