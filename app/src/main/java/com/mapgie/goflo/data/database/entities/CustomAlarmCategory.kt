package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_alarm_categories",
    foreignKeys = [
        ForeignKey(
            entity = CustomAlarm::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackingCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("alarmId"), Index("categoryId")],
)
data class CustomAlarmCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val categoryId: Long,
)
