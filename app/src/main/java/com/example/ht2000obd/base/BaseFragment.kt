package com.example.ht2000obd.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.ht2000obd.utils.CoroutineUtils
import com.example.ht2000obd.utils.LogUtils
import com.example.ht2000obd.utils.UIUtils.showSnackbar
import com.example.ht2000obd.utils.UIUtils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    // Abstract function to be implemented by child fragments
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initViews()
            setupObservers()
            setupListeners()
        } catch (e: Exception) {
            LogUtils.e("BaseFragment", "Error in onViewCreated", e)
            showError("Failed to initialize view")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Template methods to be overridden by child fragments
    protected open fun initViews() {}
    protected open fun setupObservers() {}
    protected open fun setupListeners() {}

    // Utility methods for showing messages
    protected fun showError(message: String) {
        view?.showSnackbar(message)
    }

    protected fun showMessage(message: String) {
        requireContext().showToast(message)
    }

    // Coroutine utility methods
    protected fun launchInViewScope(
        block: suspend CoroutineScope.() -> Unit
    ) = viewLifecycleOwner.lifecycleScope.launch(
        CoroutineUtils.createExceptionHandler("BaseFragment")
    ) {
        block()
    }

    protected fun launchWhenStarted(
        block: suspend CoroutineScope.() -> Unit
    ) = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }

    // Safe navigation methods
    protected fun navigateBack() {
        try {
            requireActivity().onBackPressed()
        } catch (e: Exception) {
            LogUtils.e("BaseFragment", "Error navigating back", e)
        }
    }

    // Loading state handling
    protected fun showLoading() {
        // Override in child fragments if needed
    }

    protected fun hideLoading() {
        // Override in child fragments if needed
    }

    // Permission handling
    protected fun checkPermissions(
        permissions: Array<String>,
        onGranted: () -> Unit,
        onDenied: () -> Unit = { showError("Required permissions not granted") }
    ) {
        val permissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }

        permissionLauncher.launch(permissions)
    }

    // Lifecycle logging for debugging
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onCreate")
    }

    override fun onStart() {
        super.onStart()
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onStart")
    }

    override fun onResume() {
        super.onResume()
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onResume")
    }

    override fun onPause() {
        super.onPause()
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onPause")
    }

    override fun onStop() {
        super.onStop()
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onDestroy")
    }

    // Utility method for safe fragment transactions
    protected fun replaceFragment(
        containerId: Int,
        fragment: Fragment,
        addToBackStack: Boolean = true
    ) {
        try {
            childFragmentManager.beginTransaction().apply {
                replace(containerId, fragment)
                if (addToBackStack) {
                    addToBackStack(null)
                }
                commit()
            }
        } catch (e: Exception) {
            LogUtils.e("BaseFragment", "Error replacing fragment", e)
        }
    }

    // Utility method for handling back press
    protected fun handleBackPress(callback: () -> Unit) {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    callback()
                }
            }
        )
    }
}