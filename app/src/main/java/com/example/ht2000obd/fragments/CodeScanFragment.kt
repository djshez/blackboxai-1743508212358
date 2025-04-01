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
import com.example.ht2000obd.adapters.TroubleCodeAdapter
import com.example.ht2000obd.databinding.FragmentCodeScanBinding
import com.example.ht2000obd.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CodeScanFragment : Fragment() {
    private var _binding: FragmentCodeScanBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var codeAdapter: TroubleCodeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCodeScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        setupObservers()
    }

    private fun setupRecyclerView() {
        codeAdapter = TroubleCodeAdapter { code ->
            showCodeDetails(code)
        }
        
        binding.recyclerViewCodes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = codeAdapter
        }
    }

    private fun setupButtons() {
        // Scan button
        binding.buttonScan.setOnClickListener {
            startScan()
        }

        // Clear codes button
        binding.buttonClear.setOnClickListener {
            showClearConfirmationDialog()
        }
    }

    private fun setupObservers() {
        // Observe trouble codes
        viewModel.troubleCodes.observe(viewLifecycleOwner) { codes ->
            updateUI(codes)
        }

        // Observe connection status
        viewModel.connectionStatus.observe(viewLifecycleOwner) { isConnected ->
            binding.buttonScan.isEnabled = isConnected
            binding.buttonClear.isEnabled = isConnected
            
            if (!isConnected) {
                showError(getString(R.string.error_connection_failed))
            }
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    private fun startScan() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonScan.isEnabled = false
        binding.textNoCodes.visibility = View.GONE

        lifecycleScope.launch {
            try {
                viewModel.scanTroubleCodes()
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_generic))
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.buttonScan.isEnabled = true
            }
        }
    }

    private fun updateUI(codes: List<String>) {
        binding.progressBar.visibility = View.GONE
        binding.buttonScan.isEnabled = true
        binding.buttonClear.isEnabled = codes.isNotEmpty()

        if (codes.isEmpty()) {
            binding.textNoCodes.visibility = View.VISIBLE
            binding.recyclerViewCodes.visibility = View.GONE
        } else {
            binding.textNoCodes.visibility = View.GONE
            binding.recyclerViewCodes.visibility = View.VISIBLE
            codeAdapter.submitList(codes)
        }
    }

    private fun showCodeDetails(code: String) {
        // Get description for the trouble code (you would need a database or API for this)
        val description = getCodeDescription(code)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(code)
            .setMessage(description)
            .setPositiveButton(getString(R.string.dialog_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showClearConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_clear_codes_title))
            .setMessage(getString(R.string.dialog_clear_codes_message))
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                clearTroubleCodes()
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun clearTroubleCodes() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                viewModel.clearTroubleCodes()
                Snackbar.make(
                    binding.root,
                    "Trouble codes cleared successfully",
                    Snackbar.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                showError(e.message ?: getString(R.string.error_generic))
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun getCodeDescription(code: String): String {
        // This would typically come from a database or API
        // For now, return a generic description
        return "Trouble code $code. Please consult your vehicle's manual or a professional mechanic for detailed information."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}