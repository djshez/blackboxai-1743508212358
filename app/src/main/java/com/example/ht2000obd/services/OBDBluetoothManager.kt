package com.example.ht2000obd.services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class OBDBluetoothManager(private val context: Context) {
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectionTimeout: Long = 10000L // 10 seconds default
    private var autoConnect: Boolean = false

    companion object {
        private const val TAG = "OBDBluetoothManager"
        private val STANDARD_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        // OBD Commands
        private const val RESET_COMMAND = "ATZ"
        private const val ECHO_OFF = "ATE0"
        private const val LINE_FEED_OFF = "ATL0"
        private const val HEADERS_OFF = "ATH0"
        private const val SPACES_OFF = "ATS0"
        private const val ENGINE_RPM = "010C"
        private const val VEHICLE_SPEED = "010D"
        private const val COOLANT_TEMP = "0105"
        private const val FUEL_LEVEL = "012F"
        private const val THROTTLE_POS = "0111"
        private const val READ_DTCS = "03"
        private const val CLEAR_DTCS = "04"
    }

    // StateFlows for real-time data
    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus

    private val _obdDataFlow = MutableStateFlow(OBDData())
    val obdDataFlow: Flow<OBDData> = _obdDataFlow

    data class OBDData(
        val rpm: Int = 0,
        val speed: Int = 0,
        val coolantTemp: Int = 0,
        val fuelLevel: Int = 0,
        val throttlePosition: Int = 0
    )

    suspend fun connect(deviceAddress: String) {
        withContext(Dispatchers.IO) {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    ?: throw IOException("Bluetooth is not available on this device")

                if (!bluetoothAdapter.isEnabled) {
                    throw IOException("Bluetooth is disabled")
                }

                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                connectToDevice(device)
                initializeOBDConnection()
                _connectionStatus.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionStatus.value = false
                throw e
            }
        }
    }

    private suspend fun connectToDevice(device: BluetoothDevice) = suspendCoroutine { continuation ->
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(STANDARD_SPP_UUID)
            bluetoothSocket?.connect()
            continuation.resume(Unit)
        } catch (e: Exception) {
            bluetoothSocket?.close()
            bluetoothSocket = null
            continuation.resumeWithException(e)
        }
    }

    private suspend fun initializeOBDConnection() {
        // Reset OBD adapter
        sendCommand(RESET_COMMAND)
        delay(1000) // Wait for adapter to reset

        // Initialize communication parameters
        sendCommand(ECHO_OFF)
        sendCommand(LINE_FEED_OFF)
        sendCommand(HEADERS_OFF)
        sendCommand(SPACES_OFF)
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                bluetoothSocket?.close()
                bluetoothSocket = null
                _connectionStatus.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect failed", e)
                throw e
            }
        }
    }

    suspend fun refreshData() {
        if (!_connectionStatus.value) {
            throw IOException("Not connected to OBD adapter")
        }

        withContext(Dispatchers.IO) {
            try {
                val rpm = sendCommand(ENGINE_RPM).toInt(16)
                val speed = sendCommand(VEHICLE_SPEED).toInt(16)
                val coolantTemp = sendCommand(COOLANT_TEMP).toInt(16) - 40 // Convert to Celsius
                val fuelLevel = sendCommand(FUEL_LEVEL).toInt(16) * 100 / 255 // Convert to percentage
                val throttle = sendCommand(THROTTLE_POS).toInt(16) * 100 / 255 // Convert to percentage

                _obdDataFlow.value = OBDData(
                    rpm = rpm,
                    speed = speed,
                    coolantTemp = coolantTemp,
                    fuelLevel = fuelLevel,
                    throttlePosition = throttle
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh OBD data", e)
                throw e
            }
        }
    }

    suspend fun scanTroubleCodes(): List<String> {
        if (!_connectionStatus.value) {
            throw IOException("Not connected to OBD adapter")
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = sendCommand(READ_DTCS)
                parseDTCResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read DTCs", e)
                throw e
            }
        }
    }

    suspend fun clearTroubleCodes() {
        if (!_connectionStatus.value) {
            throw IOException("Not connected to OBD adapter")
        }

        withContext(Dispatchers.IO) {
            try {
                sendCommand(CLEAR_DTCS)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear DTCs", e)
                throw e
            }
        }
    }

    private suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val outputStream = bluetoothSocket?.outputStream
                ?: throw IOException("Bluetooth socket not connected")
            val inputStream = bluetoothSocket?.inputStream
                ?: throw IOException("Bluetooth socket not connected")

            // Send command
            outputStream.write("${command}\r".toByteArray())
            outputStream.flush()

            // Read response
            val buffer = ByteArray(1024)
            val bytes = inputStream.read(buffer)
            val response = String(buffer, 0, bytes).trim()

            // Process response
            if (response.contains("ERROR") || response.contains("UNABLE TO CONNECT")) {
                throw IOException("OBD command error: $response")
            }

            response
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command: $command", e)
            throw e
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

    fun updateConnectionTimeout(timeout: Long) {
        connectionTimeout = timeout
    }

    fun setAutoConnect(enabled: Boolean) {
        autoConnect = enabled
    }

    private suspend fun delay(millis: Long) {
        withContext(Dispatchers.IO) {
            Thread.sleep(millis)
        }
    }
}