package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapgie.goflo.data.database.entities.TrackingCategory
import com.mapgie.goflo.data.database.entities.TrackingValue
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingCategoryDao {

    // ── Categories ────────────────────────────────────────────────────

    @Query("SELECT * FROM tracking_categories ORDER BY displayOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<TrackingCategory>>

    @Query("SELECT * FROM tracking_categories WHERE isArchived = 0 ORDER BY displayOrder ASC, id ASC")
    fun getActiveCategories(): Flow<List<TrackingCategory>>

    @Query("SELECT * FROM tracking_categories WHERE id = :id")
    fun getCategoryById(id: Long): Flow<TrackingCategory?>

    @Query("SELECT * FROM tracking_categories WHERE id = :id")
    suspend fun getCategoryByIdOnce(id: Long): TrackingCategory?

    @Query("SELECT * FROM tracking_categories ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllCategoriesOnce(): List<TrackingCategory>

    /** Returns the first system category with the given name, or null. */
    @Query("SELECT * FROM tracking_categories WHERE isSystem = 1 AND name = :name LIMIT 1")
    suspend fun getSystemCategoryByName(name: String): TrackingCategory?

    /** Returns the system category with the given stable key ("flow", "symptoms"), or null. */
    @Query("SELECT * FROM tracking_categories WHERE systemKey = :key LIMIT 1")
    suspend fun getSystemCategoryByKey(key: String): TrackingCategory?

    /** Returns active (non-archived) categories with showInLogPeriod = 1. */
    @Query("SELECT * FROM tracking_categories WHERE isArchived = 0 AND showInLogPeriod = 1 ORDER BY displayOrder ASC, id ASC")
    suspend fun getShowInLogPeriodCategoriesOnce(): List<TrackingCategory>

    /** Returns the first category (any type) with the given name, or null. Used for import matching. */
    @Query("SELECT * FROM tracking_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): TrackingCategory?

    /** Returns the category linked to a tracking-mode preset key, or null if none exists. */
    @Query("SELECT * FROM tracking_categories WHERE modeKey = :key AND modeKey != '' LIMIT 1")
    suspend fun getCategoryByModeKey(key: String): TrackingCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TrackingCategory): Long

    @Update
    suspend fun updateCategory(category: TrackingCategory)

    @Delete
    suspend fun deleteCategory(category: TrackingCategory)

    // ── Values ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM tracking_values WHERE categoryId = :categoryId ORDER BY displayOrder ASC, id ASC")
    fun getValuesForCategory(categoryId: Long): Flow<List<TrackingValue>>

    @Query("SELECT * FROM tracking_values WHERE categoryId = :categoryId ORDER BY displayOrder ASC, id ASC")
    suspend fun getValuesForCategoryOnce(categoryId: Long): List<TrackingValue>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertValue(value: TrackingValue): Long

    @Update
    suspend fun updateValue(value: TrackingValue)

    @Delete
    suspend fun deleteValue(value: TrackingValue)

    /** Deletes all non-system (user-created) categories. Cascades to tracking_values. */
    @Query("DELETE FROM tracking_categories WHERE isSystem = 0")
    suspend fun deleteAllCustomCategories()

    /** Restores all system categories to visible (not archived). */
    @Query("UPDATE tracking_categories SET isArchived = 0 WHERE isSystem = 1")
    suspend fun unarchiveAllSystemCategories()

    /**
     * Bulk-update the label of every tracking_log_values row whose label matches
     * [oldLabel] and whose logId belongs to logs in [categoryId].
     * Used when the user chooses "Fix everywhere" on a rename.
     */
    @Query(
        """
        UPDATE tracking_log_values
        SET valueLabel = :newLabel
        WHERE valueLabel = :oldLabel
          AND logId IN (
              SELECT id FROM tracking_logs WHERE categoryId = :categoryId
          )
        """
    )
    suspend fun bulkRenameLogValues(categoryId: Long, oldLabel: String, newLabel: String)
}
