package com.mapgie.goflo.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "color_profiles")
data class ColorProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(defaultValue = "") val name: String = "",
    @ColumnInfo(defaultValue = "0") val primaryArgb: Int = 0,
    @ColumnInfo(defaultValue = "0") val secondaryArgb: Int = 0,
    @ColumnInfo(defaultValue = "0") val tertiaryArgb: Int = 0,
    @ColumnInfo(defaultValue = "0") val lightBackgroundArgb: Int = 0,
    @ColumnInfo(defaultValue = "0") val darkBackgroundArgb: Int = 0,
)
