package com.mapgie.goflo.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An optional grouping of tracking categories (e.g. "Sleep", "Pain").
 *
 * A group owns a colour role and a default input type for the categories filed
 * under it. Categories reference a group via [TrackingCategory.groupId]; a
 * category whose [TrackingCategory.colorToken] is the "inherit" sentinel
 * resolves its colour from the group's [colorRole] (see
 * [com.mapgie.goflo.ui.util.effectiveColorToken]).
 *
 * [colorRole] is always a [com.mapgie.goflo.ui.util.CategoryColor] key
 * ("primary", "secondary", "tertiary", "quaternary", "quinary", "senary") —
 * never a raw hex value; groups are in-theme by design.
 *
 * [defaultInputType] is a [com.mapgie.goflo.ui.util.CategoryType] key, used to
 * pre-select the input type when creating a category inside the group.
 *
 * Deleting a group never deletes its categories — members are unfiled
 * (groupId set to null) first; see TrackingRepository.deleteGroup.
 */
@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "primary") val colorRole: String = "primary",
    @ColumnInfo(defaultValue = "default") val defaultInputType: String = "default",
    @ColumnInfo(defaultValue = "0") val displayOrder: Int = 0,
)
