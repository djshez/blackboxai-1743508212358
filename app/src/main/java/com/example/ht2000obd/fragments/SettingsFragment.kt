package com.example.ht2000obd.fragments

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.ht2000obd.R
import com.example.ht2000obd.databinding.FragmentSettingsBinding
import com.example.ht2000obd.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var pairedDevices: Set<BluetoothDevice>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBluetoothAdapter()
        setupUI()
        setupObservers()
    }

    private fun setupBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            showError(getString(R.string.error_bluetooth_unavailable))
            return
        }
    }

    private fun setupUI() {
        // Bluetooth settings card
        binding.cardBluetooth.setOnClickListener {
            showBluetoothDevicesList()
        }

        // Connection timeout setting
        binding.sliderTimeout.addOnChangeListener { _, value, _ ->
            val timeout = value.toLong() * 1000 // Convert to milliseconds
            viewModel.updateConnectionTimeout(timeout)
            binding.textTimeoutValue.text = "${value.toInt()} seconds"
        }

        // Auto-connect switch
        binding.switchAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoConnect(isChecked)
        }

        // Units preference
        binding.radioGroupUnits.setOnCheckedChangeListener { _, checkedId ->
            val isMetric = checkedId == R.id.radio_metric
            // Save units preference
            activity?.getSharedPreferences("settings", 0)?.edit()?.apply {
                putBoolean("use_metric", isMetric)
                apply()
            }
        }

        // Dark mode switch
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Update theme
            activity?.getSharedPreferences("settings", 0)?.edit()?.apply {
                putBoolean("dark_mode", isChecked)
                apply()
            }
            // Recreate activity to apply theme
            activity?.recreate()
        }

        // Load saved preferences
        activity?.getSharedPreferences("settings", 0)?.apply {
            val useMetric = getBoolean("use_metric", true)
            val darkMode = getBoolean("dark_mode", false)
            
            binding.radioGroupUnits.check(
                if (useMetric) R.id.radio_metric else R.id.radio_imperial
            )
            binding.switchDarkMode.isChecked = darkMode
        }
    }

    private fun setupObservers() {
        viewModel.connectionStatus.observe(viewLifecycleOwner) { isConnected ->
            updateConnectionStatus(isConnected)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    private fun showBluetoothDevicesList() {
        if (bluetoothAdapter?.isEnabled != true) {
            showError(getString(R.string.error_bluetooth_disabled))
            return
        }

        try {
            pairedDevices = bluetoothAdapter?.bondedDevices
            val deviceList = pairedDevices?.map { device ->
                "${device.name} (${device.address})"
            }?.toTypedArray() ?: arrayOf()

            if (deviceList.isEmpty()) {
                showError("No paired devices found. Please pair your OBD adapter first.")
                return
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.select_adapter))
                .setItems(deviceList) { _, which ->
                    pairedDevices?.elementAt(which)?.let { device ->
                        connectToDevice(device)
                    }
                }
                .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            showError("Failed to list Bluetooth devices: ${e.message}")
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.connectToDevice(device.address)
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.textConnectionStatus.text = getString(
            if (isConnected) R.string.connected else R.string.disconnected
        )
        binding.textConnectionStatus.setTextColor(
            resources.getColor(
                if (isConnected) android.R.color.holo_green_dark
                else android.R.color.holo_red_dark,
                null
            )
        )
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}