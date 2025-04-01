package com.example.ht2000obd.base

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
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

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    // Abstract method to be implemented by child dialog fragments
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            // Request a window with no title
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            
            // Set transparent background and remove window background
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

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
            LogUtils.e("BaseDialogFragment", "Error in onViewCreated", e)
            showError("Failed to initialize dialog")
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Make dialog width match parent
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Template methods to be overridden by child dialog fragments
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
    protected fun launchInScope(
        block: suspend CoroutineScope.() -> Unit
    ) = viewLifecycleOwner.lifecycleScope.launch(
        CoroutineUtils.createExceptionHandler("BaseDialogFragment")
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

    // Dialog size utility methods
    protected fun setDialogSize(width: Int, height: Int) {
        dialog?.window?.setLayout(width, height)
    }

    protected fun setDialogWidthMatchParent() {
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    protected fun setDialogWidthPercentage(percentage: Int) {
        val width = (resources.displayMetrics.widthPixels * (percentage.toFloat() / 100)).toInt()
        dialog?.window?.setLayout(
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Animation utility methods
    protected fun setDialogAnimation(animationStyle: Int) {
        dialog?.window?.attributes?.windowAnimations = animationStyle
    }

    // Dismissal callback
    protected fun setOnDismissListener(listener: () -> Unit) {
        dialog?.setOnDismissListener {
            listener()
        }
    }

    // Loading state handling
    protected fun showLoading() {
        // Override in child dialog fragments if needed
    }

    protected fun hideLoading() {
        // Override in child dialog fragments if needed
    }

    // Lifecycle logging for debugging
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.d("Lifecycle", "${javaClass.simpleName} onCreate")
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

    // Utility method for safe dismissal
    protected fun dismissSafely() {
        try {
            if (isAdded && !isDetached) {
                dismiss()
            }
        } catch (e: Exception) {
            LogUtils.e("BaseDialogFragment", "Error dismissing dialog", e)
        }
    }

    companion object {
        const val DEFAULT_WIDTH_PERCENTAGE = 90
    }
}