package com.example.ht2000obd.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.ht2000obd.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_entries WHERE date(timestamp/1000, 'unixepoch') = date(:timestamp/1000, 'unixepoch') ORDER BY timestamp DESC")
    fun getEntriesForDate(timestamp: Long): Flow<List<HistoryEntry>>

    @Insert
    suspend fun insert(entry: HistoryEntry)

    @Delete
    suspend fun delete(entry: HistoryEntry)

    @Query("DELETE FROM history_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM history_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): HistoryEntry?

    @Query("SELECT COUNT(*) FROM history_entries")
    suspend fun getCount(): Int

    @Query("SELECT * FROM history_entries WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEntriesInTimeRange(startTime: Long, endTime: Long): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history_entries WHERE EXISTS (SELECT 1 FROM json_each(troubleCodes) WHERE value LIKE :codePattern) ORDER BY timestamp DESC")
    fun getEntriesWithTroubleCode(codePattern: String): Flow<List<HistoryEntry>>
}