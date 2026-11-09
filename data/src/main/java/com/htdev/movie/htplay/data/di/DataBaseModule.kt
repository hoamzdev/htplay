package com.htdev.movie.htplay.data.di

import android.content.Context
import androidx.room.Room
import com.htdev.movie.htplay.data.db.AppRoomDataBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DataBaseModule {

    @Provides
    @Singleton
    fun providerAppDataBase(
        @ApplicationContext context: Context
    ): AppRoomDataBase {
        return Room.databaseBuilder(
            context,
            klass = AppRoomDataBase::class.java,
            name = "app_data_base"
        ).build()
    }

    @Provides
    @Singleton
    fun providerContinueWatchingDao(appRoomDataBase: AppRoomDataBase) =
        appRoomDataBase.continueWatchingDao()

    @Provides
    @Singleton
    fun providerFavoriteDao(appRoomDataBase: AppRoomDataBase) =
        appRoomDataBase.favoriteDao()

}