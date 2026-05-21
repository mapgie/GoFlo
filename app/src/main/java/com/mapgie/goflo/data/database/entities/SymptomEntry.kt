package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "symptoms",
    foreignKeys = [
        ForeignKey(
            entity = PeriodEntry::class,
            parentColumns = ["id"],
            childColumns = ["periodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("periodId")]
)
data class SymptomEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodId: Long,
    val symptomType: String
)
