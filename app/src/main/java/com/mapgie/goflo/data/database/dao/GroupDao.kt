package com.mapgie.goflo.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapgie.goflo.data.database.entities.Group
import kotlinx.coroutines.flow.Flow

/**
 * Data access for category groups.
 *
 * `groups` is close to a SQL keyword, so the table name is always backticked
 * in raw queries here and in migrations.
 */
@Dao
interface GroupDao {

    @Query("SELECT * FROM `groups` ORDER BY displayOrder ASC, name ASC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM `groups` ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllGroupsOnce(): List<Group>

    @Query("SELECT * FROM `groups` WHERE id = :id")
    suspend fun getGroupById(id: Long): Group?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Update
    suspend fun updateGroup(group: Group)

    @Delete
    suspend fun deleteGroup(group: Group)
}
