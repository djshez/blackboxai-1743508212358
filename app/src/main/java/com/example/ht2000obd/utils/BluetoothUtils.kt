package com.example.ht2000obd.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.ht2000obd.exceptions.OBDException

object BluetoothUtils {
    private const val HT2000_PREFIX = "HT-OBD"
    private const val ELM327_PREFIX = "OBDII"

    /**
     * Get the BluetoothAdapter, throwing an exception if Bluetooth is not available
     */
    fun getBluetoothAdapter(context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter ?: throw OBDException.BluetoothNotAvailable()
    }

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        return try {
            getBluetoothAdapter(context).isEnabled
        } catch (e: OBDException.BluetoothNotAvailable) {
            false
        }
    }

    /**
     * Get list of paired OBD devices
     */
    fun getPairedOBDDevices(context: Context): List<BluetoothDevice> {
        val adapter = getBluetoothAdapter(context)
        
        if (!hasBluetoothPermission(context)) {
            throw OBDException.BluetoothPermissionDenied()
        }

        return adapter.bondedDevices.filter { device ->
            device.name?.let { name ->
                name.startsWith(HT2000_PREFIX) || name.startsWith(ELM327_PREFIX)
            } ?: false
        }
    }

    /**
     * Get a specific Bluetooth device by address
     */
    fun getDeviceByAddress(context: Context, address: String): BluetoothDevice? {
        if (!hasBluetoothPermission(context)) {
            throw OBDException.BluetoothPermissionDenied()
        }

        return try {
            getBluetoothAdapter(context).getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Check if we have the necessary Bluetooth permissions
     */
    private fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT) &&
                    hasPermission(context, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPermission(context, Manifest.permission.BLUETOOTH) &&
                    hasPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    /**
     * Check for a specific permission
     */
    private fun hasPermission(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Format device name and address for display
     */
    fun formatDeviceDetails(device: BluetoothDevice): String {
        if (!hasBluetoothPermission(device.context)) {
            return "Unknown Device"
        }
        return "${device.name ?: "Unknown"} (${device.address})"
    }

    /**
     * Check if a device is likely an OBD adapter
     */
    fun isOBDDevice(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermission(device.context)) {
            return false
        }
        return device.name?.let { name ->
            name.startsWith(HT2000_PREFIX) || name.startsWith(ELM327_PREFIX)
        } ?: false
    }

    /**
     * Get device name safely (handling permissions)
     */
    fun getDeviceName(device: BluetoothDevice): String {
        return if (hasBluetoothPermission(device.context)) {
            device.name ?: "Unknown Device"
        } else {
            "Unknown Device"
        }
    }

    /**
     * Get device address safely (handling permissions)
     */
    fun getDeviceAddress(device: BluetoothDevice): String {
        return if (hasBluetoothPermission(device.context)) {
            device.address
        } else {
            "Unknown Address"
        }
    }

    /**
     * Extension property to get context from BluetoothDevice
     */
    private val BluetoothDevice.context: Context
        get() = OBDApplication.getInstance()

    /**
     * Check if a device is connected
     */
    fun isDeviceConnected(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermission(device.context)) {
            return false
        }
        return try {
            device.bondState == BluetoothDevice.BOND_BONDED
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Constants for Bluetooth operations
     */
    object Constants {
        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_PERMISSIONS = 2
        
        // Standard UUID for SPP (Serial Port Profile)
        const val UUID_SPP = "00001101-0000-1000-8000-00805F9B34FB"
        
        // Connection timeouts
        const val CONNECT_TIMEOUT = 10000L // 10 seconds
        const val READ_TIMEOUT = 5000L // 5 seconds
    }
}