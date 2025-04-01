package com.example.ht2000obd.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ht2000obd.R
import com.example.ht2000obd.databinding.FragmentAdvancedDashboardBinding
import com.example.ht2000obd.viewmodel.AdvancedDiagnosticViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AdvancedDashboardFragment : Fragment() {
    private var _binding: FragmentAdvancedDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdvancedDiagnosticViewModel by viewModels()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private var isRecording = false
    private val recordedData = mutableListOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvancedDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupObservers()
        setupButtons()
    }

    private fun setupCharts() {
        // Setup RPM Chart
        binding.chartRpm.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            legend.isEnabled = true
        }

        // Setup MAF Chart
        binding.chartMaf.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            xAxis.setDrawGridLines(false)
            legend.isEnabled = true
        }
    }

    private fun setupObservers() {
        // Connection Status
        viewModel.connectionStatus.observe(viewLifecycleOwner) { isConnected ->
            binding.textConnectionStatus.text = if (isConnected) "Connected" else "Disconnected"
            binding.textConnectionStatus.setTextColor(
                resources.getColor(
                    if (isConnected) android.R.color.holo_green_dark 
                    else android.R.color.holo_red_dark,
                    null
                )
            )
        }

        // VIN
        viewModel.vehicleVin.observe(viewLifecycleOwner) { vin ->
            binding.textVin.text = "VIN: $vin"
        }

        // Engine Data
        viewModel.engineData.observe(viewLifecycleOwner) { data ->
            binding.textRpm.text = "RPM: ${data.rpm}"
            binding.textMaf.text = "MAF: ${data.massAirFlow} g/s"
            binding.textTiming.text = "Timing: ${data.timingAdvance}°"
            updateRpmChart(data.rpm)
            updateMafChart(data.massAirFlow)
        }

        // Fuel System Data
        viewModel.fuelData.observe(viewLifecycleOwner) { data ->
            binding.textFuelPressure.text = "Pressure: ${data.pressure} kPa"
            binding.textFuelTrim.text = "Trim: ${data.trim}%"
            binding.textInjectionTiming.text = "Injection: ${data.injectionTiming}°"
        }

        // Error handling
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setupButtons() {
        binding.buttonRefresh.setOnClickListener {
            refreshData()
        }

        binding.buttonExport.setOnClickListener {
            exportData()
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            try {
                viewModel.refreshData()
                if (isRecording) {
                    recordData()
                }
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun recordData() {
        val currentData = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "rpm" to viewModel.engineData.value?.rpm,
            "maf" to viewModel.engineData.value?.massAirFlow,
            "timing" to viewModel.engineData.value?.timingAdvance,
            "fuelPressure" to viewModel.fuelData.value?.pressure,
            "fuelTrim" to viewModel.fuelData.value?.trim,
            "injectionTiming" to viewModel.fuelData.value?.injectionTiming
        )
        recordedData.add(currentData)
    }

    private fun exportData() {
        lifecycleScope.launch {
            try {
                val timestamp = dateFormat.format(Date())
                val fileName = "obd_data_$timestamp.csv"
                val file = File(requireContext().getExternalFilesDir(null), fileName)

                file.printWriter().use { out ->
                    // Write header
                    out.println("Timestamp,RPM,MAF,Timing,Fuel Pressure,Fuel Trim,Injection Timing")
                    
                    // Write data
                    recordedData.forEach { data ->
                        out.println(
                            "${data["timestamp"]},${data["rpm"]},${data["maf"]},${data["timing"]}," +
                            "${data["fuelPressure"]},${data["fuelTrim"]},${data["injectionTiming"]}"
                        )
                    }
                }

                showMessage("Data exported to $fileName")
            } catch (e: Exception) {
                showError("Failed to export data: ${e.message}")
            }
        }
    }

    private fun updateRpmChart(rpm: Int) {
        val data = binding.chartRpm.data ?: createInitialRpmChartData()
        
        // Add new entry
        data.addEntry(Entry(data.entryCount.toFloat(), rpm.toFloat()), 0)
        
        // Limit data points
        if (data.entryCount > 50) {
            data.removeEntry(0, 0)
        }
        
        // Update chart
        data.notifyDataChanged()
        binding.chartRpm.notifyDataSetChanged()
        binding.chartRpm.setVisibleXRangeMaximum(50f)
        binding.chartRpm.moveViewToX(data.entryCount.toFloat())
    }

    private fun updateMafChart(maf: Float) {
        val data = binding.chartMaf.data ?: createInitialMafChartData()
        
        // Add new entry
        data.addEntry(Entry(data.entryCount.toFloat(), maf), 0)
        
        // Limit data points
        if (data.entryCount > 50) {
            data.removeEntry(0, 0)
        }
        
        // Update chart
        data.notifyDataChanged()
        binding.chartMaf.notifyDataSetChanged()
        binding.chartMaf.setVisibleXRangeMaximum(50f)
        binding.chartMaf.moveViewToX(data.entryCount.toFloat())
    }

    private fun createInitialRpmChartData(): LineData {
        val set = LineDataSet(ArrayList(), "RPM").apply {
            setDrawValues(false)
            setDrawCircles(false)
            color = resources.getColor(R.color.colorPrimary, null)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        return LineData(set).also { binding.chartRpm.data = it }
    }

    private fun createInitialMafChartData(): LineData {
        val set = LineDataSet(ArrayList(), "MAF").apply {
            setDrawValues(false)
            setDrawCircles(false)
            color = resources.getColor(R.color.colorAccent, null)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        return LineData(set).also { binding.chartMaf.data = it }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}