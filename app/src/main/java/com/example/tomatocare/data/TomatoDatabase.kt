package com.example.tomatocare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AnalysisEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TomatoDatabase : RoomDatabase() {

    abstract fun analysisDao(): AnalysisDao

    companion object {
        @Volatile
        private var INSTANCE: TomatoDatabase? = null

        fun getInstance(context: Context): TomatoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TomatoDatabase::class.java,
                    "tomato_care.db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}