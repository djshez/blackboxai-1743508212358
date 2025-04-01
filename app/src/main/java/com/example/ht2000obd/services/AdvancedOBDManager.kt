package com.example.ht2000obd.services

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdvancedOBDManager(
    private val context: Context,
    private val bluetoothManager: OBDBluetoothManager
) {
    companion object {
        // Advanced OBD PIDs
        private val ADVANCED_PIDS = mapOf(
            "OXYGEN_SENSORS" to "0113",
            "MASS_AIR_FLOW" to "0110",
            "TIMING_ADVANCE" to "010E",
            "INTAKE_PRESSURE" to "010B",
            "FUEL_PRESSURE" to "010A",
            "FUEL_TRIM" to "0106",
            "FUEL_INJECTION_TIMING" to "015D",
            "CATALYST_TEMP" to "013C",
            "CONTROL_MODULE_VOLTAGE" to "0142",
            "ABS_STATUS" to "0121",
            "TRANSMISSION_TEMP" to "015C"
        )

        // Enhanced diagnostic commands
        private const val VIN_COMMAND = "0902"
        private const val FREEZE_FRAME_COMMAND = "0202"
        private const val PENDING_CODES_COMMAND = "07"
        private const val PERMANENT_CODES_COMMAND = "0A"
    }

    // StateFlows for real-time data
    private val _advancedData = MutableStateFlow(AdvancedOBDData())
    val advancedData: StateFlow<AdvancedOBDData> = _advancedData

    private val _diagnosticState = MutableStateFlow(DiagnosticState())
    val diagnosticState: StateFlow<DiagnosticState> = _diagnosticState

    data class AdvancedOBDData(
        val oxygenSensors: List<Float> = emptyList(),
        val massAirFlow: Float = 0f,
        val timingAdvance: Float = 0f,
        val intakePressure: Int = 0,
        val fuelPressure: Int = 0,
        val fuelTrim: Float = 0f,
        val fuelInjectionTiming: Float = 0f,
        val catalystTemp: Int = 0,
        val controlModuleVoltage: Float = 0f,
        val absStatus: Boolean = false,
        val transmissionTemp: Int = 0
    )

    data class DiagnosticState(
        val vin: String = "",
        val freezeFrameData: Map<String, String> = emptyMap(),
        val pendingCodes: List<String> = emptyList(),
        val permanentCodes: List<String> = emptyList(),
        val sensorStatus: Map<String, Boolean> = emptyMap()
    )

    suspend fun getVehicleVIN(): String {
        return bluetoothManager.sendCommand(VIN_COMMAND).let { response ->
            // Parse VIN from response
            response.substring(4).replace(" ", "")
        }
    }

    suspend fun getFreezeFrameData(): Map<String, String> {
        return bluetoothManager.sendCommand(FREEZE_FRAME_COMMAND).let { response ->
            // Parse freeze frame data
            parseFreezeFrameData(response)
        }
    }

    suspend fun getPendingCodes(): List<String> {
        return bluetoothManager.sendCommand(PENDING_CODES_COMMAND).let { response ->
            parseDTCResponse(response)
        }
    }

    suspend fun getPermanentCodes(): List<String> {
        return bluetoothManager.sendCommand(PERMANENT_CODES_COMMAND).let { response ->
            parseDTCResponse(response)
        }
    }

    suspend fun refreshAdvancedData() {
        val newData = AdvancedOBDData(
            oxygenSensors = getOxygenSensorValues(),
            massAirFlow = getMassAirFlow(),
            timingAdvance = getTimingAdvance(),
            intakePressure = getIntakePressure(),
            fuelPressure = getFuelPressure(),
            fuelTrim = getFuelTrim(),
            fuelInjectionTiming = getFuelInjectionTiming(),
            catalystTemp = getCatalystTemp(),
            controlModuleVoltage = getControlModuleVoltage(),
            absStatus = getABSStatus(),
            transmissionTemp = getTransmissionTemp()
        )
        _advancedData.value = newData
    }

    private suspend fun getOxygenSensorValues(): List<Float> {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["OXYGEN_SENSORS"]!!)
        return parseOxygenSensors(response)
    }

    private suspend fun getMassAirFlow(): Float {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["MASS_AIR_FLOW"]!!)
        return response.toInt(16) * 0.01f
    }

    private suspend fun getTimingAdvance(): Float {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["TIMING_ADVANCE"]!!)
        return (response.toInt(16) - 128) * 0.5f
    }

    private suspend fun getIntakePressure(): Int {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["INTAKE_PRESSURE"]!!)
        return response.toInt(16)
    }

    private suspend fun getFuelPressure(): Int {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["FUEL_PRESSURE"]!!)
        return response.toInt(16) * 3
    }

    private suspend fun getFuelTrim(): Float {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["FUEL_TRIM"]!!)
        return (response.toInt(16) - 128) * 0.78125f
    }

    private suspend fun getFuelInjectionTiming(): Float {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["FUEL_INJECTION_TIMING"]!!)
        return (response.toInt(16) - 26880) / 128f
    }

    private suspend fun getCatalystTemp(): Int {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["CATALYST_TEMP"]!!)
        return (response.toInt(16) - 40)
    }

    private suspend fun getControlModuleVoltage(): Float {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["CONTROL_MODULE_VOLTAGE"]!!)
        return response.toInt(16) * 0.001f
    }

    private suspend fun getABSStatus(): Boolean {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["ABS_STATUS"]!!)
        return response.toInt(16) and 0x01 == 0
    }

    private suspend fun getTransmissionTemp(): Int {
        val response = bluetoothManager.sendCommand(ADVANCED_PIDS["TRANSMISSION_TEMP"]!!)
        return response.toInt(16) - 40
    }

    private fun parseOxygenSensors(response: String): List<Float> {
        return response.chunked(2).map { it.toInt(16) * 0.0125f }
    }

    private fun parseFreezeFrameData(response: String): Map<String, String> {
        // Parse freeze frame data into key-value pairs
        return response.split(" ").chunked(2).associate { (pid, value) ->
            pid to value
        }
    }

    private fun parseDTCResponse(response: String): List<String> {
        return response.split(" ")
            .filter { it.length == 4 }
            .map { code ->
                val type = when (code[0]) {
                    '0' -> "P"
                    '1' -> "C"
                    '2' -> "B"
                    '3' -> "U"
                    else -> "P"
                }
                "$type${code.substring(1)}"
            }
    }
}