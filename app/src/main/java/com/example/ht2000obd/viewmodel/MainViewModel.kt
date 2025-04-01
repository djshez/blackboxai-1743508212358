package com.example.ht2000obd.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ht2000obd.services.OBDBluetoothManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val obdManager = OBDBluetoothManager(application)

    // Connection Status
    private val _connectionStatus = MutableLiveData<Boolean>()
    val connectionStatus: LiveData<Boolean> = _connectionStatus

    // OBD Data
    private val _engineRpm = MutableLiveData<Int>()
    val engineRpm: LiveData<Int> = _engineRpm

    private val _vehicleSpeed = MutableLiveData<Int>()
    val vehicleSpeed: LiveData<Int> = _vehicleSpeed

    private val _coolantTemp = MutableLiveData<Int>()
    val coolantTemp: LiveData<Int> = _coolantTemp

    private val _fuelLevel = MutableLiveData<Int>()
    val fuelLevel: LiveData<Int> = _fuelLevel

    private val _throttlePosition = MutableLiveData<Int>()
    val throttlePosition: LiveData<Int> = _throttlePosition

    // Error handling
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Trouble Codes
    private val _troubleCodes = MutableLiveData<List<String>>()
    val troubleCodes: LiveData<List<String>> = _troubleCodes

    init {
        // Start observing OBD data when ViewModel is created
        observeOBDData()
    }

    private fun observeOBDData() {
        viewModelScope.launch {
            obdManager.obdDataFlow
                .flowOn(Dispatchers.IO)
                .catch { e -> handleError(e) }
                .collect { data ->
                    _engineRpm.value = data.rpm
                    _vehicleSpeed.value = data.speed
                    _coolantTemp.value = data.coolantTemp
                    _fuelLevel.value = data.fuelLevel
                    _throttlePosition.value = data.throttlePosition
                }
        }

        viewModelScope.launch {
            obdManager.connectionStatus
                .flowOn(Dispatchers.IO)
                .catch { e -> handleError(e) }
                .collect { isConnected ->
                    _connectionStatus.value = isConnected
                }
        }
    }

    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            try {
                obdManager.connect(deviceAddress)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleError(e)
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                obdManager.disconnect()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleError(e)
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                obdManager.refreshData()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleError(e)
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = throwable.message
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            obdManager.disconnect()
        }
    }

    fun scanTroubleCodes() {
        viewModelScope.launch {
            try {
                val codes = obdManager.scanTroubleCodes()
                _troubleCodes.value = codes
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleError(e)
                }
            }
        }
    }

    fun clearTroubleCodes() {
        viewModelScope.launch {
            try {
                obdManager.clearTroubleCodes()
                _troubleCodes.value = emptyList()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleError(e)
                }
            }
        }
    }

    // Settings
    fun updateConnectionTimeout(timeout: Long) {
        obdManager.updateConnectionTimeout(timeout)
    }

    fun setAutoConnect(enabled: Boolean) {
        obdManager.setAutoConnect(enabled)
    }
}