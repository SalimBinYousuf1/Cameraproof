package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, PhotoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SalimDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: SalimDatabase? = null

        fun getInstance(context: Context): SalimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalimDatabase::class.java,
                    "salim_proof.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
