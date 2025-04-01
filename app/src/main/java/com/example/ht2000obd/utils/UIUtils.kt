package com.example.ht2000obd.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.ht2000obd.R

object UIUtils {
    // Extension function to show toast
    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    fun Context.showToast(@StringRes messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, messageRes, duration).show()
    }

    // Extension function to show snackbar
    fun View.showSnackbar(
        message: String,
        duration: Int = Snackbar.LENGTH_LONG,
        action: String? = null,
        actionCallback: (() -> Unit)? = null
    ) {
        Snackbar.make(this, message, duration).apply {
            action?.let { actionText ->
                setAction(actionText) { actionCallback?.invoke() }
            }
            show()
        }
    }

    // Extension function to show alert dialog
    fun Context.showAlertDialog(
        title: String,
        message: String,
        positiveButton: String = getString(R.string.dialog_ok),
        negativeButton: String? = getString(R.string.dialog_cancel),
        positiveCallback: (() -> Unit)? = null,
        negativeCallback: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton) { dialog, _ ->
                dialog.dismiss()
                positiveCallback?.invoke()
            }
            .apply {
                negativeButton?.let { btnText ->
                    setNegativeButton(btnText) { dialog, _ ->
                        dialog.dismiss()
                        negativeCallback?.invoke()
                    }
                }
            }
            .show()
    }

    // Extension function to hide keyboard
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    // Extension function to show keyboard
    fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    // Extension function to get color from resources
    fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(this, colorRes)
    }

    // Extension function to get drawable from resources
    fun Context.getDrawableCompat(@DrawableRes drawableRes: Int): Drawable? {
        return ContextCompat.getDrawable(this, drawableRes)
    }

    // Extension function to check if device is in dark mode
    fun Context.isDarkMode(): Boolean {
        return resources.configuration.uiMode and 
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    // Extension function to set view visibility
    fun View.setVisible(visible: Boolean) {
        visibility = if (visible) View.VISIBLE else View.GONE
    }

    // Extension function to toggle view visibility
    fun View.toggleVisibility() {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    // Extension function for Fragment to get color from resources
    fun Fragment.getColorCompat(@ColorRes colorRes: Int): Int {
        return requireContext().getColorCompat(colorRes)
    }

    // Extension function for Fragment to get drawable from resources
    fun Fragment.getDrawableCompat(@DrawableRes drawableRes: Int): Drawable? {
        return requireContext().getDrawableCompat(drawableRes)
    }

    // Extension function to load drawable into ImageView
    fun ImageView.setDrawableRes(@DrawableRes drawableRes: Int) {
        setImageDrawable(context.getDrawableCompat(drawableRes))
    }

    // Extension function to enable/disable view with alpha change
    fun View.setEnabledWithAlpha(enabled: Boolean) {
        isEnabled = enabled
        alpha = if (enabled) 1.0f else 0.5f
    }

    // Function to format error messages for display
    fun formatErrorForDisplay(error: Throwable): String {
        return when (error) {
            is com.example.ht2000obd.exceptions.OBDException -> error.message ?: "Unknown error"
            else -> error.localizedMessage ?: "An unexpected error occurred"
        }
    }

    // Constants for UI operations
    object Constants {
        const val ANIMATION_DURATION_SHORT = 150L
        const val ANIMATION_DURATION_MEDIUM = 300L
        const val ANIMATION_DURATION_LONG = 500L
        
        const val ALPHA_DISABLED = 0.5f
        const val ALPHA_ENABLED = 1.0f
        
        const val CLICK_THROTTLE_DURATION = 300L
    }

    // Extension function to prevent double clicks
    private var lastClickTime: Long = 0
    fun View.setThrottledClickListener(throttleTime: Long = Constants.CLICK_THROTTLE_DURATION, action: () -> Unit) {
        setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= throttleTime) {
                lastClickTime = currentTime
                action.invoke()
            }
        }
    }
}