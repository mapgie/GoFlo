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
import com.mapgie.goflo.data.database.dao.TrackingCategoryDao
import com.mapgie.goflo.data.database.dao.TrackingLogDao
import com.mapgie.goflo.data.database.entities.CustomSymptomEntry
import com.mapgie.goflo.data.database.entities.PeriodEntry
import com.mapgie.goflo.data.database.entities.SymptomEntry
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingLog
import com.mapgie.goflo.data.database.entities.TrackingLogValue
import com.mapgie.goflo.data.database.entities.TrackingValue

@Database(
    entities = [
        PeriodEntry::class,
        SymptomEntry::class,
        CustomSymptomEntry::class,
        TrackingCategory::class,
        TrackingValue::class,
        TrackingLog::class,
        TrackingLogValue::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class GoFloDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun symptomDao(): SymptomDao
    abstract fun customSymptomDao(): CustomSymptomDao
    abstract fun trackingCategoryDao(): TrackingCategoryDao
    abstract fun trackingLogDao(): TrackingLogDao

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

        /**
         * Adds the four tracking tables introduced in version 3:
         * tracking_categories, tracking_values, tracking_logs, tracking_log_values.
         * Existing data is untouched. System categories (Flow, Symptoms) are seeded
         * here so existing users get them on upgrade.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `tracking_categories`
                       (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `isSystem` INTEGER NOT NULL DEFAULT 0,
                        `displayOrder` INTEGER NOT NULL DEFAULT 0)"""
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `tracking_values`
                       (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `displayOrder` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`categoryId`) REFERENCES `tracking_categories`(`id`)
                            ON DELETE CASCADE)"""
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tracking_values_categoryId` ON `tracking_values`(`categoryId`)"
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `tracking_logs`
                       (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`categoryId`) REFERENCES `tracking_categories`(`id`)
                            ON DELETE CASCADE)"""
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tracking_logs_categoryId` ON `tracking_logs`(`categoryId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tracking_logs_date` ON `tracking_logs`(`date`)"
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `tracking_log_values`
                       (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `logId` INTEGER NOT NULL,
                        `valueLabel` TEXT NOT NULL,
                        FOREIGN KEY(`logId`) REFERENCES `tracking_logs`(`id`)
                            ON DELETE CASCADE)"""
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tracking_log_values_logId` ON `tracking_log_values`(`logId`)"
                )

                // Seed system categories for existing users upgrading from v2
                seedSystemCategories(database)
            }
        }

        /**
         * Inserts the pre-seeded Flow and Symptoms categories and their default values.
         * Called both from MIGRATION_2_3 (for upgrades) and from the onCreate callback
         * (for fresh installs).
         */
        private fun seedSystemCategories(database: SupportSQLiteDatabase) {
            // Flow category
            database.execSQL(
                "INSERT INTO tracking_categories (name, isSystem, displayOrder) VALUES ('Flow', 1, 0)"
            )
            val flowIdCursor = database.query("SELECT last_insert_rowid()")
            flowIdCursor.moveToFirst()
            val flowId = flowIdCursor.getLong(0)
            flowIdCursor.close()

            listOf("Spotting", "Light", "Medium", "Heavy").forEachIndexed { index, label ->
                database.execSQL(
                    "INSERT INTO tracking_values (categoryId, label, displayOrder) VALUES ($flowId, '$label', $index)"
                )
            }

            // Symptoms category
            database.execSQL(
                "INSERT INTO tracking_categories (name, isSystem, displayOrder) VALUES ('Symptoms', 1, 1)"
            )
            val symptomIdCursor = database.query("SELECT last_insert_rowid()")
            symptomIdCursor.moveToFirst()
            val symptomId = symptomIdCursor.getLong(0)
            symptomIdCursor.close()

            listOf("Cramps", "Headache", "Bloating", "Fatigue", "Back Pain", "Mood Swings")
                .forEachIndexed { index, label ->
                    database.execSQL(
                        "INSERT INTO tracking_values (categoryId, label, displayOrder) VALUES ($symptomId, '$label', $index)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Fresh install: seed the system categories
                            seedSystemCategories(db)
                        }
                    })
                    .build().also { instance = it }
            }
    }
}
