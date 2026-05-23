package com.mapgie.goflo.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stores the user's library of custom symptom names (always lowercase). */
@Entity(tableName = "custom_symptoms")
data class CustomSymptomEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String   // stored and displayed in lowercase
)
