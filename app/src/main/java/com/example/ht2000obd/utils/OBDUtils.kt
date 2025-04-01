package com.example.ht2000obd.utils

import android.content.Context
import android.os.Build
import com.example.ht2000obd.OBDApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object OBDUtils {
    private const val KPH_TO_MPH = 0.621371
    private const val CELSIUS_TO_FAHRENHEIT_MULTIPLIER = 1.8
    private const val CELSIUS_TO_FAHRENHEIT_OFFSET = 32

    // Date formatting
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val filenameDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // Unit conversions
    fun kphToMph(kph: Int): Int = (kph * KPH_TO_MPH).roundToInt()

    fun mphToKph(mph: Int): Int = (mph / KPH_TO_MPH).roundToInt()

    fun celsiusToFahrenheit(celsius: Int): Int =
        (celsius * CELSIUS_TO_FAHRENHEIT_MULTIPLIER + CELSIUS_TO_FAHRENHEIT_OFFSET).roundToInt()

    fun fahrenheitToCelsius(fahrenheit: Int): Int =
        ((fahrenheit - CELSIUS_TO_FAHRENHEIT_OFFSET) / CELSIUS_TO_FAHRENHEIT_MULTIPLIER).roundToInt()

    // Date formatting
    fun formatTimestamp(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun generateFilename(prefix: String, extension: String): String =
        "${prefix}_${filenameDateFormat.format(Date())}.$extension"

    // Hex conversions
    fun hexToInt(hex: String): Int = hex.toInt(16)

    fun intToHex(value: Int): String = value.toString(16).uppercase()

    // Byte operations
    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it) }

    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val result = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            result[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
        }
        return result
    }

    // File operations
    fun getExportDirectory(context: Context): File {
        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(null), "exports")
        } else {
            File(context.filesDir, "exports")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // Unit preferences
    fun useMetricUnits(): Boolean = OBDApplication.getInstance().useMetricUnits()

    fun formatSpeed(speedKph: Int): String {
        return if (useMetricUnits()) {
            "$speedKph km/h"
        } else {
            "${kphToMph(speedKph)} mph"
        }
    }

    fun formatTemperature(tempCelsius: Int): String {
        return if (useMetricUnits()) {
            "$tempCelsius°C"
        } else {
            "${celsiusToFahrenheit(tempCelsius)}°F"
        }
    }

    // PID calculations
    object PID {
        // Mode 01 PIDs
        const val ENGINE_RPM = "010C"
        const val VEHICLE_SPEED = "010D"
        const val MAF_SENSOR = "0110"
        const val O2_VOLTAGE = "0114"
        const val THROTTLE_POS = "0111"
        const val ENGINE_COOLANT_TEMP = "0105"
        const val INTAKE_TEMP = "010F"
        const val FUEL_LEVEL = "012F"
        const val BAROMETRIC_PRESSURE = "0133"
        const val TIMING_ADVANCE = "010E"
        const val FUEL_TYPE = "0151"
        const val FUEL_PRESSURE = "010A"
        const val ENGINE_OIL_TEMP = "015C"
        const val ENGINE_RUNTIME = "011F"

        // Mode 03 - Show stored Diagnostic Trouble Codes
        const val SHOW_DTC = "03"

        // Mode 04 - Clear Diagnostic Trouble Codes
        const val CLEAR_DTC = "04"

        // AT Commands
        const val RESET = "ATZ"
        const val ECHO_OFF = "ATE0"
        const val LINE_FEED_OFF = "ATL0"
        const val HEADERS_OFF = "ATH0"
        const val SPACES_OFF = "ATS0"
        const val DESCRIBE_PROTOCOL = "ATDP"
        const val DESCRIBE_PROTOCOL_NUMBER = "ATDPN"
    }

    // DTC parsing
    fun parseDTC(code: String): String {
        if (code.length != 4) return "INVALID"
        
        val type = when (code[0]) {
            '0' -> "P" // Powertrain
            '1' -> "C" // Chassis
            '2' -> "B" // Body
            '3' -> "U" // Network
            else -> "?"
        }
        return "$type${code.substring(1)}"
    }

    fun getBasicDTCDescription(dtc: String): String {
        return when (dtc.first()) {
            'P' -> "Powertrain - Engine or Transmission Issue"
            'C' -> "Chassis - ABS, Suspension, or Steering Issue"
            'B' -> "Body - Airbags, Lighting, or Climate Control Issue"
            'U' -> "Network - Communication Bus Issue"
            else -> "Unknown System Issue"
        }
    }

    // Validation
    fun isValidResponse(response: String): Boolean {
        if (response.isBlank()) return false
        if (response.contains("ERROR") || response.contains("UNABLE TO CONNECT")) return false
        if (response.contains("NO DATA")) return false
        return true
    }

    fun isValidPID(pid: String): Boolean {
        if (pid.length != 4) return false
        return pid.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }
    }
}