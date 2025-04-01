package com.example.ht2000obd.utils

import android.content.Context
import android.net.Uri
import com.example.ht2000obd.exceptions.OBDException
import com.example.ht2000obd.model.HistoryEntry
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    /**
     * Export history entries to CSV file
     */
    suspend fun exportToCSV(context: Context, entries: List<HistoryEntry>): File {
        return CoroutineUtils.safeIO {
            val timestamp = dateFormat.format(Date())
            val fileName = "obd_history_$timestamp.csv"
            val file = File(OBDUtils.getExportDirectory(context), fileName)

            FileWriter(file).use { writer ->
                // Write header
                writer.append(
                    "Timestamp,RPM,Speed (km/h),Coolant Temp (°C),Fuel Level (%),Throttle Position (%),Trouble Codes\n"
                )

                // Write data
                entries.forEach { entry ->
                    writer.append(formatCSVLine(entry))
                }
            }

            LogUtils.i("Export", "Exported ${entries.size} entries to CSV: ${file.absolutePath}")
            file
        }.getOrThrow()
    }

    /**
     * Export history entries to JSON file
     */
    suspend fun exportToJSON(context: Context, entries: List<HistoryEntry>): File {
        return CoroutineUtils.safeIO {
            val timestamp = dateFormat.format(Date())
            val fileName = "obd_history_$timestamp.json"
            val file = File(OBDUtils.getExportDirectory(context), fileName)

            FileWriter(file).use { writer ->
                gson.toJson(entries, writer)
            }

            LogUtils.i("Export", "Exported ${entries.size} entries to JSON: ${file.absolutePath}")
            file
        }.getOrThrow()
    }

    /**
     * Import history entries from JSON file
     */
    suspend fun importFromJSON(context: Context, uri: Uri): List<HistoryEntry> {
        return CoroutineUtils.safeIO {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<HistoryEntry>>() {}.type
                gson.fromJson<List<HistoryEntry>>(jsonString, type)
            } ?: throw OBDException.ImportError()
        }.getOrThrow()
    }

    /**
     * Import history entries from CSV file
     */
    suspend fun importFromCSV(context: Context, uri: Uri): List<HistoryEntry> {
        return CoroutineUtils.safeIO {
            val entries = mutableListOf<HistoryEntry>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().useLines { lines ->
                    // Skip header
                    lines.drop(1).forEach { line ->
                        try {
                            entries.add(parseCSVLine(line))
                        } catch (e: Exception) {
                            LogUtils.e("Import", "Failed to parse CSV line: $line", e)
                        }
                    }
                }
            } ?: throw OBDException.ImportError()
            entries
        }.getOrThrow()
    }

    /**
     * Format a history entry as a CSV line
     */
    private fun formatCSVLine(entry: HistoryEntry): String {
        return buildString {
            append(OBDUtils.formatTimestamp(entry.timestamp)).append(",")
            append(entry.rpm).append(",")
            append(entry.speed).append(",")
            append(entry.coolantTemp).append(",")
            append(entry.fuelLevel).append(",")
            append(entry.throttlePosition).append(",")
            append("\"${entry.troubleCodes.joinToString(";")}\"")
            append("\n")
        }
    }

    /**
     * Parse a CSV line into a history entry
     */
    private fun parseCSVLine(line: String): HistoryEntry {
        val parts = line.split(",")
        if (parts.size < 7) throw OBDException.ImportError()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .parse(parts[0])?.time ?: throw OBDException.ImportError()

        val troubleCodes = parts[6].trim('"').split(";").filter { it.isNotEmpty() }

        return HistoryEntry(
            timestamp = timestamp,
            rpm = parts[1].toInt(),
            speed = parts[2].toInt(),
            coolantTemp = parts[3].toInt(),
            fuelLevel = parts[4].toInt(),
            throttlePosition = parts[5].toInt(),
            troubleCodes = troubleCodes
        )
    }

    /**
     * Get MIME type for export file
     */
    fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "csv" -> "text/csv"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }

    /**
     * Check if file is a valid import file
     */
    fun isValidImportFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in listOf("csv", "json")
    }
}