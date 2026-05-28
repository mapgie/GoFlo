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
    version = 8,
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
         * Adds the four tracking tables introduced in version 3.
         * Seeds system categories using ONLY the v3 schema columns (name, isSystem,
         * displayOrder) — iconName and colorToken are not added until later migrations.
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
                // Seed with v3-only columns; subsequent migrations add iconName + colorToken
                seedSystemCategoriesV3(database)
            }
        }

        /**
         * Adds iconName + colorArgb to tracking_categories (v4).
         * colorArgb is superseded by colorToken in MIGRATION_4_5 and is kept here
         * only to maintain a valid migration chain for users upgrading from v3.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE tracking_categories ADD COLUMN `iconName` TEXT NOT NULL DEFAULT 'category'"
                )
                database.execSQL(
                    "ALTER TABLE tracking_categories ADD COLUMN `colorArgb` INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE tracking_categories SET `iconName`='water'   WHERE isSystem=1 AND name='Flow'"
                )
                database.execSQL(
                    "UPDATE tracking_categories SET `iconName`='healing' WHERE isSystem=1 AND name='Symptoms'"
                )
            }
        }

        /**
         * Replaces the hardcoded colorArgb (ARGB integer) with colorToken (semantic
         * string key: "primary" | "secondary" | "tertiary" | "error").  The token is
         * resolved to an actual colour from MaterialTheme.colorScheme at render time,
         * so category bubbles automatically follow the user's chosen palette and
         * light/dark mode.
         *
         * Migration strategy: table reconstruction (SQLite cannot DROP or ALTER
         * COLUMN type on Android API 26+).  All existing log and value rows are
         * preserved through their foreign-key relationships.
         *
         * Token defaults:
         *   Flow     → "primary"   (matches the period-circle colour)
         *   Symptoms → "tertiary"  (matches the ovulation/accent colour)
         *   Custom   → "secondary" (neutral, distinct from the two built-ins)
         */
        /**
         * Adds numeric-mode columns to tracking_categories (v6).
         *
         * All existing categories default to isNumeric=0 (text mode), preserving
         * all existing data and behaviour.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE tracking_categories ADD COLUMN `isNumeric` INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE tracking_categories ADD COLUMN `numericMin` REAL NOT NULL DEFAULT 0.0"
                )
                database.execSQL(
                    "ALTER TABLE tracking_categories ADD COLUMN `numericMax` REAL NOT NULL DEFAULT 10.0"
                )
                database.execSQL(
                    "ALTER TABLE tracking_categories ADD COLUMN `allowDecimals` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Replaces [isNumeric] boolean with [categoryType] string, and adds
         * [numericUnit] and [isArchived].  Table reconstruction is required
         * because SQLite (API 26) cannot drop columns directly.
         *
         * Existing numeric categories (isNumeric=1) are migrated to
         * categoryType='numeric_slider'.  All other data is preserved.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE `tracking_categories_new`
                       (`id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name`         TEXT NOT NULL,
                        `isSystem`     INTEGER NOT NULL DEFAULT 0,
                        `displayOrder` INTEGER NOT NULL DEFAULT 0,
                        `iconName`     TEXT NOT NULL DEFAULT 'category',
                        `colorToken`   TEXT NOT NULL DEFAULT 'secondary',
                        `categoryType` TEXT NOT NULL DEFAULT 'default',
                        `numericMin`   REAL NOT NULL DEFAULT 0.0,
                        `numericMax`   REAL NOT NULL DEFAULT 10.0,
                        `allowDecimals` INTEGER NOT NULL DEFAULT 0,
                        `numericUnit`  TEXT NOT NULL DEFAULT '',
                        `isArchived`   INTEGER NOT NULL DEFAULT 0)"""
                )
                database.execSQL(
                    """INSERT INTO tracking_categories_new
                       (id, name, isSystem, displayOrder, iconName, colorToken,
                        categoryType, numericMin, numericMax, allowDecimals)
                       SELECT id, name, isSystem, displayOrder, iconName, colorToken,
                           CASE WHEN isNumeric=1 THEN 'numeric_slider' ELSE 'default' END,
                           numericMin, numericMax, allowDecimals
                       FROM tracking_categories"""
                )
                database.execSQL("DROP TABLE tracking_categories")
                database.execSQL("ALTER TABLE tracking_categories_new RENAME TO tracking_categories")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE tracking_categories ADD COLUMN `allowMultiple` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create the new table with colorToken instead of colorArgb
                database.execSQL(
                    """CREATE TABLE `tracking_categories_new`
                       (`id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name`         TEXT NOT NULL,
                        `isSystem`     INTEGER NOT NULL DEFAULT 0,
                        `displayOrder` INTEGER NOT NULL DEFAULT 0,
                        `iconName`     TEXT NOT NULL DEFAULT 'category',
                        `colorToken`   TEXT NOT NULL DEFAULT 'secondary')"""
                )
                // 2. Copy data, mapping system categories to appropriate theme tokens
                database.execSQL(
                    """INSERT INTO tracking_categories_new
                       (id, name, isSystem, displayOrder, iconName, colorToken)
                       SELECT id, name, isSystem, displayOrder, iconName,
                           CASE
                               WHEN isSystem=1 AND name='Flow'     THEN 'primary'
                               WHEN isSystem=1 AND name='Symptoms' THEN 'tertiary'
                               ELSE 'secondary'
                           END
                       FROM tracking_categories"""
                )
                // 3. Swap tables
                database.execSQL("DROP TABLE tracking_categories")
                database.execSQL("ALTER TABLE tracking_categories_new RENAME TO tracking_categories")
            }
        }

        // ── Seeding helpers ───────────────────────────────────────────────────

        /**
         * Seeds system categories using only the original v3 schema columns.
         * Called exclusively from [MIGRATION_2_3] to avoid referencing columns that
         * don't exist yet in the database at that migration step.
         */
        private fun seedSystemCategoriesV3(database: SupportSQLiteDatabase) {
            database.execSQL(
                "INSERT INTO tracking_categories (name, isSystem, displayOrder) VALUES ('Flow', 1, 0)"
            )
            val flowIdCursor = database.query("SELECT last_insert_rowid()")
            flowIdCursor.moveToFirst()
            val flowId = flowIdCursor.getLong(0)
            flowIdCursor.close()

            listOf("Spotting", "Light", "Medium", "Heavy").forEachIndexed { index, label ->
                database.execSQL(
                    "INSERT INTO tracking_values (categoryId, label, displayOrder) VALUES (?, ?, ?)",
                    arrayOf(flowId, label, index)
                )
            }

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
                        "INSERT INTO tracking_values (categoryId, label, displayOrder) VALUES (?, ?, ?)",
                        arrayOf(symptomId, label, index)
                    )
                }
        }

        /**
         * Seeds system categories with ALL current (v7) columns.
         * Called only from [RoomDatabase.Callback.onCreate] for fresh installs.
         */
        private fun seedSystemCategories(database: SupportSQLiteDatabase) {
            // Flow — water drop icon, primary colour token
            database.execSQL(
                "INSERT INTO tracking_categories (name, isSystem, displayOrder, `iconName`, `colorToken`, `categoryType`) " +
                "VALUES ('Flow', 1, 0, 'water', 'primary', 'default')"
            )
            val flowIdCursor = database.query("SELECT last_insert_rowid()")
            flowIdCursor.moveToFirst()
            val flowId = flowIdCursor.getLong(0)
            flowIdCursor.close()

            listOf("Spotting", "Light", "Medium", "Heavy").forEachIndexed { index, label ->
                database.execSQL(
                    "INSERT INTO tracking_values (categoryId, label, displayOrder) VALUES (?, ?, ?)",
                    arrayOf(flowId, label, index)
                )
            }

            // Symptoms — healing icon, tertiary (accent) colour token
            database.execSQL(
                "INSERT INTO tracking_categories (name, isSystem, displayOrder, `iconName`, `colorToken`, `categoryType`) " +
                "VALUES ('Symptoms', 1, 1, 'healing', 'tertiary', 'default')"
            )
            val symptomIdCursor = database.query("SELECT last_insert_rowid()")
            symptomIdCursor.moveToFirst()
            val symptomId = symptomIdCursor.getLong(0)
            symptomIdCursor.close()

            listOf("Cramps", "Headache", "Bloating", "Fatigue", "Back Pain", "Mood Swings")
                .forEachIndexed { index, label ->
                    database.execSQL(
                        "INSERT INTO tracking_values (categoryId, label, displayOrder) VALUES (?, ?, ?)",
                        arrayOf(symptomId, label, index)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // SQLite has foreign-key enforcement OFF by default.
                            // Must be re-enabled on every connection open.
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedSystemCategories(db)
                        }
                    })
                    .build().also { instance = it }
            }
    }
}
