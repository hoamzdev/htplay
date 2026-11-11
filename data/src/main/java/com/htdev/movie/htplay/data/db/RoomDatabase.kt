package com.htdev.movie.htplay.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.htdev.movie.htplay.data.entities.dto.ContinueWatchingDB
import com.htdev.movie.htplay.data.entities.dto.FavoriteDB


@Database(entities = [ContinueWatchingDB::class, FavoriteDB::class], version = 3, exportSchema = false)
abstract class AppRoomDataBase : RoomDatabase(){
    abstract fun continueWatchingDao() : ContinueWatchingDao
    abstract fun favoriteDao() : FavoriteDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Keep the most recently saved row for each movie before enforcing uniqueness.
                db.execSQL("DELETE FROM favorite WHERE id NOT IN (SELECT MAX(id) FROM favorite GROUP BY slugMovie)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_favorite_slugMovie ON favorite (slugMovie)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE favorite_new (name TEXT NOT NULL, slugMovie TEXT NOT NULL, genre TEXT NOT NULL, posterUrl TEXT, PRIMARY KEY(slugMovie))")
                db.execSQL("INSERT INTO favorite_new (name, slugMovie, genre, posterUrl) SELECT name, slugMovie, genre, posterUrl FROM favorite ORDER BY id ASC")
                db.execSQL("DROP TABLE favorite")
                db.execSQL("ALTER TABLE favorite_new RENAME TO favorite")
            }
        }
    }
}
