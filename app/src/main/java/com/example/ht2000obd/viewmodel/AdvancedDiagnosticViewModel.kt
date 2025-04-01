package com.example.ht2000obd.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ht2000obd.data.DiagnosticDatabase
import com.example.ht2000obd.services.AdvancedOBDManager
import com.example.ht2000obd.services.OBDBluetoothManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.IOException

class AdvancedDiagnosticViewModel(application: Application) : AndroidViewModel(application) {
    private val obdManager = OBDBluetoothManager(application)
    private val advancedManager = AdvancedOBDManager(application, obdManager)
    private val diagnosticDb = DiagnosticDatabase.getDatabase(application)

    // LiveData for UI updates
    private val _connectionStatus = MutableLiveData<Boolean>()
    val connectionStatus: LiveData<Boolean> = _connectionStatus

    private val _vehicleVin = MutableLiveData<String>()
    val vehicleVin: LiveData<String> = _vehicleVin

    private val _engineData = MutableLiveData<EngineData>()
    val engineData: LiveData<EngineData> = _engineData

    private val _fuelData = MutableLiveData<FuelData>()
    val fuelData: LiveData<FuelData> = _fuelData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    data class EngineData(
        val rpm: Int = 0,
        val massAirFlow: Float = 0f,
        val timingAdvance: Float = 0f
    )

    data class FuelData(
        val pressure: Int = 0,
        val trim: Float = 0f,
        val injectionTiming: Float = 0f
    )

    init {
        viewModelScope.launch {
            // Observe OBD connection status
            obdManager.connectionStatus.collect { status ->
                _connectionStatus.value = status
                if (status) {
                    try {
                        _vehicleVin.value = advancedManager.getVehicleVIN()
                    } catch (e: Exception) {
                        _error.value = "Failed to get VIN: ${e.message}"
                    }
                }
            }

            // Observe advanced OBD data
            advancedManager.advancedData.collect { data ->
                _engineData.value = EngineData(
                    rpm = data.rpm,
                    massAirFlow = data.massAirFlow,
                    timingAdvance = data.timingAdvance
                )

                _fuelData.value = FuelData(
                    pressure = data.fuelPressure,
                    trim = data.fuelTrim,
                    injectionTiming = data.fuelInjectionTiming
                )
            }
        }
    }

    suspend fun connect(deviceAddress: String) {
        try {
            obdManager.connect(deviceAddress)
        } catch (e: Exception) {
            _error.value = "Connection failed: ${e.message}"
            throw e
        }
    }

    suspend fun disconnect() {
        try {
            obdManager.disconnect()
        } catch (e: Exception) {
            _error.value = "Disconnect failed: ${e.message}"
            throw e
        }
    }

    suspend fun refreshData() {
        if (_connectionStatus.value != true) {
            _error.value = "Not connected to OBD adapter"
            return
        }

        try {
            advancedManager.refreshAdvancedData()
        } catch (e: Exception) {
            _error.value = "Failed to refresh data: ${e.message}"
            throw e
        }
    }

    suspend fun getDiagnosticDetails(code: String) = 
        diagnosticDb.diagnosticDao().getCodeDetails(code)

    suspend fun getVehicleProfile(make: String, model: String, year: Int) =
        diagnosticDb.diagnosticDao().getVehicleProfile(make, model, year)

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                disconnect()
            } catch (e: Exception) {
                // Ignore disconnect errors during cleanup
            }
        }
    }
}