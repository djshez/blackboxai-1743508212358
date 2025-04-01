package com.example.ht2000obd.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ht2000obd.R
import com.example.ht2000obd.adapters.HistoryAdapter
import com.example.ht2000obd.databinding.FragmentHistoryBinding
import com.example.ht2000obd.model.HistoryEntry
import com.example.ht2000obd.viewmodel.MainViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter
    private var selectedDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        setupObservers()
        loadHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { entry -> showHistoryDetails(entry) },
            onDeleteClick = { entry -> showDeleteConfirmation(entry) }
        )

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun setupButtons() {
        // Date filter button
        binding.buttonDateFilter.setOnClickListener {
            showDatePicker()
        }

        // Clear filter button
        binding.buttonClearFilter.setOnClickListener {
            selectedDate = null
            binding.buttonClearFilter.visibility = View.GONE
            binding.textDateFilter.visibility = View.GONE
            loadHistory()
        }

        // Clear all history button
        binding.buttonClearAll.setOnClickListener {
            showClearAllConfirmation()
        }

        // Export button
        binding.buttonExport.setOnClickListener {
            exportHistory()
        }
    }

    private fun setupObservers() {
        viewModel.historyEntries.observe(viewLifecycleOwner) { entries ->
            updateUI(entries)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    private fun loadHistory() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                viewModel.loadHistory(selectedDate)
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_generic))
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateUI(entries: List<HistoryEntry>) {
        binding.progressBar.visibility = View.GONE

        if (entries.isEmpty()) {
            binding.textNoHistory.visibility = View.VISIBLE
            binding.recyclerViewHistory.visibility = View.GONE
            binding.buttonClearAll.isEnabled = false
            binding.buttonExport.isEnabled = false
        } else {
            binding.textNoHistory.visibility = View.GONE
            binding.recyclerViewHistory.visibility = View.VISIBLE
            binding.buttonClearAll.isEnabled = true
            binding.buttonExport.isEnabled = true
            historyAdapter.submitList(entries)
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(selectedDate ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { timestamp ->
            selectedDate = timestamp
            updateDateFilter(timestamp)
            loadHistory()
        }

        datePicker.show(parentFragmentManager, "date_picker")
    }

    private fun updateDateFilter(timestamp: Long) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateString = dateFormat.format(Date(timestamp))
        
        binding.textDateFilter.apply {
            text = "Filtered by: $dateString"
            visibility = View.VISIBLE
        }
        binding.buttonClearFilter.visibility = View.VISIBLE
    }

    private fun showHistoryDetails(entry: HistoryEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("History Details")
            .setMessage(formatHistoryDetails(entry))
            .setPositiveButton(getString(R.string.dialog_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun formatHistoryDetails(entry: HistoryEntry): String {
        return """
            Time: ${SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                .format(Date(entry.timestamp))}
            
            Engine RPM: ${entry.rpm}
            Vehicle Speed: ${entry.speed} km/h
            Coolant Temp: ${entry.coolantTemp}°C
            Fuel Level: ${entry.fuelLevel}%
            Throttle Position: ${entry.throttlePosition}%
            
            ${if (entry.troubleCodes.isNotEmpty()) 
                "Trouble Codes:\n${entry.troubleCodes.joinToString("\n")}" 
                else "No trouble codes"}
        """.trimIndent()
    }

    private fun showDeleteConfirmation(entry: HistoryEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this history entry?")
            .setPositiveButton("Delete") { _, _ ->
                deleteHistoryEntry(entry)
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showClearAllConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_clear_history_title))
            .setMessage(getString(R.string.dialog_clear_history_message))
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteHistoryEntry(entry: HistoryEntry) {
        lifecycleScope.launch {
            try {
                viewModel.deleteHistoryEntry(entry)
                showSuccess("History entry deleted")
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun clearAllHistory() {
        lifecycleScope.launch {
            try {
                viewModel.clearAllHistory()
                showSuccess("History cleared")
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun exportHistory() {
        lifecycleScope.launch {
            try {
                val file = viewModel.exportHistory()
                showSuccess("History exported to: ${file.absolutePath}")
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}