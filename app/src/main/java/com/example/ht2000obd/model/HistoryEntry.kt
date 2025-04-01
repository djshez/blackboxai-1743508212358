package com.example.ht2000obd.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val rpm: Int,
    val speed: Int,
    val coolantTemp: Int,
    val fuelLevel: Int,
    val throttlePosition: Int,
    val troubleCodes: List<String>
)