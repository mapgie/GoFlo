package com.mapgie.goflo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mapgie.goflo.data.database.dao.PeriodDao
import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry

@Database(
    entities = [PeriodEntry::class, SymptomEntry::class],
    version = 1,
    exportSchema = false
)
abstract class GoFloDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun symptomDao(): SymptomDao

    companion object {
        @Volatile private var instance: GoFloDatabase? = null

        fun getInstance(context: Context): GoFloDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GoFloDatabase::class.java,
                    "goflo_database"
                ).build().also { instance = it }
            }
    }
}
