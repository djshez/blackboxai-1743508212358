package com.example.ht2000obd.base

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.ht2000obd.utils.CoroutineUtils
import com.example.ht2000obd.utils.LogUtils
import com.example.ht2000obd.utils.UIUtils.showSnackbar
import com.example.ht2000obd.utils.UIUtils.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    // Abstract method to be implemented by child activities
    abstract fun getViewBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            _binding = getViewBinding()
            setContentView(binding.root)
            
            initViews()
            setupObservers()
            setupListeners()
        } catch (e: Exception) {
            LogUtils.e("BaseActivity", "Error in onCreate", e)
            showError("Failed to initialize activity")
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    // Template methods to be overridden by child activities
    protected open fun initViews() {}
    protected open fun setupObservers() {}
    protected open fun setupListeners() {}

    // Utility methods for showing messages
    protected fun showError(message: String) {
        binding.root.showSnackbar(message)
    }

    protected fun showMessage(message: String) {
        showToast(message)
    }

    // Dialog utility methods
    protected fun showDialog(
        title: String,
        message: String,
        positiveButton: String = "OK",
        negativeButton: String? = "Cancel",
        onPositive: () -> Unit = {},
        onNegative: () -> Unit = {}
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton) { dialog, _ ->
                dialog.dismiss()
                onPositive()
            }
            .apply {
                negativeButton?.let { btnText ->
                    setNegativeButton(btnText) { dialog, _ ->
                        dialog.dismiss()
                        onNegative()
                    }
                }
            }
            .show()
    }

    // Permission handling
    protected fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    protected fun requestPermission(
        permission: String,
        requestCode: Int,
        rationaleMessage: String? = null
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            rationaleMessage?.let { message ->
                showDialog(
                    title = "Permission Required",
                    message = message,
                    positiveButton = "Grant",
                    onPositive = {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(permission),
                            requestCode
                        )
                    }
                )
            } ?: run {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                requestCode
            )
        }
    }

    // Coroutine utility methods
    protected fun launchInScope(
        block: suspend CoroutineScope.() -> Unit
    ) = lifecycleScope.launch(
        CoroutineUtils.createExceptionHandler("BaseActivity")
    ) {
        block()
    }

    protected fun launchWhenStarted(
        block: suspend CoroutineScope.() -> Unit
    ) = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            block()
        }
    }

    // Handle back button in toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Loading state handling
    protected fun showLoading() {
        // Override in child activities if needed
    }

    protected fun hideLoading() {
        // Override in child activities if needed
    }

    // Lifecycle logging for debugging
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

    // Utility method for handling back press with custom action
    protected fun handleBackPress(action: () -> Unit) {
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    action()
                }
            }
        )
    }

    // Utility method for safe fragment transactions
    protected fun replaceFragment(
        containerId: Int,
        fragment: androidx.fragment.app.Fragment,
        addToBackStack: Boolean = true
    ) {
        try {
            supportFragmentManager.beginTransaction().apply {
                replace(containerId, fragment)
                if (addToBackStack) {
                    addToBackStack(null)
                }
                commit()
            }
        } catch (e: Exception) {
            LogUtils.e("BaseActivity", "Error replacing fragment", e)
        }
    }
}