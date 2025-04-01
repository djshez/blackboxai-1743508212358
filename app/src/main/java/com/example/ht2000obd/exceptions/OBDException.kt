package com.example.ht2000obd.exceptions

sealed class OBDException(message: String) : Exception(message) {
    class BluetoothNotAvailable : OBDException("Bluetooth is not available on this device")
    class BluetoothNotEnabled : OBDException("Bluetooth is not enabled")
    class BluetoothPermissionDenied : OBDException("Bluetooth permission not granted")
    class ConnectionFailed(deviceAddress: String) : OBDException("Failed to connect to device: $deviceAddress")
    class ConnectionLost : OBDException("Connection to OBD adapter was lost")
    class DeviceNotFound(deviceAddress: String) : OBDException("Device not found: $deviceAddress")
    class CommandFailed(command: String) : OBDException("OBD command failed: $command")
    class NoResponse : OBDException("No response from OBD adapter")
    class InvalidResponse(response: String) : OBDException("Invalid response from OBD adapter: $response")
    class Timeout : OBDException("Operation timed out")
    class NotConnected : OBDException("Not connected to OBD adapter")
    class UnsupportedCommand(pid: String) : OBDException("Unsupported OBD command: $pid")
    class UnsupportedProtocol(protocol: String) : OBDException("Unsupported protocol: $protocol")
    class InitializationFailed : OBDException("Failed to initialize OBD adapter")
    class BusyError : OBDException("OBD adapter is busy")
    class DataError : OBDException("Error reading data from OBD adapter")
    class StorageError : OBDException("Error accessing storage")
    class ExportError : OBDException("Error exporting data")
    class ImportError : OBDException("Error importing data")
    class DatabaseError : OBDException("Database operation failed")
    
    companion object {
        fun fromErrorCode(code: String): OBDException {
            return when (code) {
                "NO DATA" -> NoResponse()
                "UNABLE TO CONNECT" -> ConnectionFailed("Unknown device")
                "BUS INIT ERROR" -> InitializationFailed()
                "BUS BUSY" -> BusyError()
                "DATA ERROR" -> DataError()
                "STOPPED" -> ConnectionLost()
                "ERROR" -> CommandFailed("Unknown command")
                else -> InvalidResponse(code)
            }
        }
    }
}