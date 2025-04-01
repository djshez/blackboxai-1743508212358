package com.example.ht2000obd.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ht2000obd.R
import com.example.ht2000obd.databinding.FragmentDashboardBinding
import com.example.ht2000obd.viewmodel.MainViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupObservers()
        setupRefreshButton()
    }

    private fun setupCharts() {
        with(binding.chartRpm) {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
        }
    }

    private fun setupObservers() {
        // Observe connection status
        viewModel.connectionStatus.observe(viewLifecycleOwner) { isConnected ->
            binding.connectionStatus.text = getString(
                if (isConnected) R.string.connected else R.string.disconnected
            )
            binding.connectionStatus.setTextColor(
                resources.getColor(
                    if (isConnected) android.R.color.holo_green_dark 
                    else android.R.color.holo_red_dark,
                    null
                )
            )
        }

        // Observe OBD data
        viewModel.engineRpm.observe(viewLifecycleOwner) { rpm ->
            binding.valueRpm.text = rpm.toString()
            updateRpmChart(rpm)
        }

        viewModel.vehicleSpeed.observe(viewLifecycleOwner) { speed ->
            binding.valueSpeed.text = "$speed km/h"
        }

        viewModel.coolantTemp.observe(viewLifecycleOwner) { temp ->
            binding.valueCoolantTemp.text = "$temp°C"
        }

        viewModel.fuelLevel.observe(viewLifecycleOwner) { level ->
            binding.valueFuelLevel.text = "$level%"
        }

        viewModel.throttlePosition.observe(viewLifecycleOwner) { position ->
            binding.valueThrottle.text = "$position%"
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setupRefreshButton() {
        binding.buttonRefresh.setOnClickListener {
            lifecycleScope.launch {
                try {
                    viewModel.refreshData()
                } catch (e: Exception) {
                    showError(e.message ?: getString(R.string.error_generic))
                }
            }
        }
    }

    private fun updateRpmChart(rpm: Int) {
        val data = binding.chartRpm.data ?: createInitialChartData()
        
        // Add new entry
        data.addEntry(Entry(data.entryCount.toFloat(), rpm.toFloat()), 0)
        
        // Limit data points to show
        if (data.entryCount > 50) {
            data.removeEntry(0, 0)
        }
        
        // Notify chart
        data.notifyDataChanged()
        binding.chartRpm.notifyDataSetChanged()
        binding.chartRpm.setVisibleXRangeMaximum(50f)
        binding.chartRpm.moveViewToX(data.entryCount.toFloat())
    }

    private fun createInitialChartData(): LineData {
        val set = LineDataSet(ArrayList(), "RPM").apply {
            setDrawValues(false)
            setDrawCircles(false)
            color = resources.getColor(R.color.colorPrimary, null)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        return LineData(set).also { binding.chartRpm.data = it }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}