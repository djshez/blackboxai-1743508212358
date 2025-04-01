package com.example.ht2000obd.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.ht2000obd.R
import com.example.ht2000obd.databinding.FragmentTroubleCodeBinding
import com.example.ht2000obd.viewmodel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TroubleCodeFragment : Fragment() {
    private var _binding: FragmentTroubleCodeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTroubleCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupRefreshButton()
    }

    private fun setupObservers() {
        viewModel.troubleCodes.observe(viewLifecycleOwner) { codes ->
            binding.troubleCodeList.text = codes.joinToString("\n")
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setupRefreshButton() {
        binding.buttonRefresh.setOnClickListener {
            lifecycleScope.launch {
                try {
                    viewModel.scanTroubleCodes()
                } catch (e: Exception) {
                    showError(e.message ?: getString(R.string.error_generic))
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}