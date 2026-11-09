package com.htdev.movie.htplay.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.htdev.movie.htplay.data.entities.dto.ContinueWatchingDB


@Database(entities = [ContinueWatchingDB::class], version = 1, exportSchema = false)
abstract class AppRoomDataBase : RoomDatabase(){
    abstract fun continueWatchingDao() : ContinueWatchingDao
}