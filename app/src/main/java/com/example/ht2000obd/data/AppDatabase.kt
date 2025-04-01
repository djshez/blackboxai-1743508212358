package com.example.ht2000obd.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.ht2000obd.model.HistoryEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(
    entities = [HistoryEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "obd_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return gson.toJson(list)
    }
}

// Repository class to handle data operations
class HistoryRepository(private val historyDao: HistoryDao) {
    val allEntries = historyDao.getAllEntries()

    fun getEntriesForDate(timestamp: Long) = historyDao.getEntriesForDate(timestamp)

    fun getEntriesInTimeRange(startTime: Long, endTime: Long) = 
        historyDao.getEntriesInTimeRange(startTime, endTime)

    fun getEntriesWithTroubleCode(codePattern: String) = 
        historyDao.getEntriesWithTroubleCode(codePattern)

    suspend fun insert(entry: HistoryEntry) {
        historyDao.insert(entry)
    }

    suspend fun delete(entry: HistoryEntry) {
        historyDao.delete(entry)
    }

    suspend fun deleteAll() {
        historyDao.deleteAll()
    }

    suspend fun getEntryById(id: Long): HistoryEntry? {
        return historyDao.getEntryById(id)
    }

    suspend fun getCount(): Int {
        return historyDao.getCount()
    }

    suspend fun exportToCSV(context: Context): java.io.File {
        val entries = allEntries.value ?: emptyList()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val timestamp = System.currentTimeMillis()
        val fileName = "obd_history_${timestamp}.csv"
        val file = java.io.File(context.getExternalFilesDir(null), fileName)

        file.bufferedWriter().use { writer ->
            // Write CSV header
            writer.write("Timestamp,RPM,Speed (km/h),Coolant Temp (°C),Fuel Level (%),Throttle Position (%),Trouble Codes\n")

            // Write entries
            entries.forEach { entry ->
                writer.write("""
                    ${dateFormat.format(java.util.Date(entry.timestamp))},
                    ${entry.rpm},
                    ${entry.speed},
                    ${entry.coolantTemp},
                    ${entry.fuelLevel},
                    ${entry.throttlePosition},
                    "${entry.troubleCodes.joinToString(";")}"
                    """.trimIndent().replace("\n", "") + "\n")
            }
        }

        return file
    }
}