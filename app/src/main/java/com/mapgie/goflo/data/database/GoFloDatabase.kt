package com.mapgie.goflo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mapgie.goflo.data.database.dao.CustomSymptomDao
import com.mapgie.goflo.data.database.dao.PeriodDao
import com.mapgie.goflo.data.database.dao.SymptomDao
import com.mapgie.goflo.data.database.entities.CustomSymptomEntry
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry

@Database(
    entities = [PeriodEntry::class, SymptomEntry::class, CustomSymptomEntry::class],
    version = 2,
    exportSchema = false
)
abstract class GoFloDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun symptomDao(): SymptomDao
    abstract fun customSymptomDao(): CustomSymptomDao

    companion object {
        @Volatile private var instance: GoFloDatabase? = null

        /** Adds the custom_symptoms library table introduced in version 2. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_symptoms` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): GoFloDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GoFloDatabase::class.java,
                    "goflo_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
