package com.example.ht2000obd.utils

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.ht2000obd.R
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationUtils {
    /**
     * Default navigation animation options
     */
    private val defaultNavOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.nav_default_enter_anim)
        .setExitAnim(R.anim.nav_default_exit_anim)
        .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
        .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
        .build()

    /**
     * Navigate using NavDirections with safe handling
     */
    fun Fragment.navigateSafely(directions: NavDirections) {
        try {
            findNavController().navigate(directions, defaultNavOptions)
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Failed to navigate using directions", e)
        }
    }

    /**
     * Navigate using destination ID with safe handling
     */
    fun Fragment.navigateSafely(
        @IdRes destinationId: Int,
        args: Bundle? = null,
        navOptions: NavOptions = defaultNavOptions
    ) {
        try {
            findNavController().navigate(destinationId, args, navOptions)
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Failed to navigate to destination: $destinationId", e)
        }
    }

    /**
     * Pop back stack with safe handling
     */
    fun Fragment.popBackStack(): Boolean {
        return try {
            findNavController().popBackStack()
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Failed to pop back stack", e)
            false
        }
    }

    /**
     * Pop back stack to a specific destination
     */
    fun Fragment.popBackStackTo(@IdRes destinationId: Int, inclusive: Boolean = false): Boolean {
        return try {
            findNavController().popBackStack(destinationId, inclusive)
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Failed to pop back stack to destination: $destinationId", e)
            false
        }
    }

    /**
     * Setup bottom navigation with NavController
     */
    fun setupBottomNavigation(
        bottomNav: BottomNavigationView,
        navController: NavController
    ) {
        bottomNav.setOnItemSelectedListener { item ->
            try {
                navController.navigate(item.itemId, null, defaultNavOptions)
                true
            } catch (e: Exception) {
                LogUtils.e("Navigation", "Failed to navigate to menu item: ${item.itemId}", e)
                false
            }
        }
    }

    /**
     * Create slide animation nav options
     */
    fun createSlideNavOptions(): NavOptions {
        return NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
    }

    /**
     * Create fade animation nav options
     */
    fun createFadeNavOptions(): NavOptions {
        return NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.fade_out)
            .setPopEnterAnim(R.anim.fade_in)
            .setPopExitAnim(R.anim.fade_out)
            .build()
    }

    /**
     * Extension function to check if a destination is on top of the back stack
     */
    fun NavController.isDestinationOnTop(@IdRes destinationId: Int): Boolean {
        return try {
            currentDestination?.id == destinationId
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Failed to check current destination", e)
            false
        }
    }

    /**
     * Extension function to safely get current destination ID
     */
    val NavController.currentDestinationId: Int?
        get() = try {
            currentDestination?.id
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Failed to get current destination ID", e)
            null
        }

    /**
     * Extension function to check if back stack is empty
     */
    fun NavController.isBackStackEmpty(): Boolean {
        return try {
            previousBackStackEntry == null
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Failed to check back stack", e)
            true
        }
    }

    /**
     * Extension function to clear back stack up to a destination
     */
    fun NavController.clearBackStackUpTo(@IdRes destinationId: Int) {
        try {
            popBackStack(destinationId, false)
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Failed to clear back stack", e)
        }
    }

    /**
     * Constants for navigation
     */
    object NavConstants {
        const val KEY_DEVICE_ADDRESS = "device_address"
        const val KEY_TROUBLE_CODE = "trouble_code"
        const val KEY_HISTORY_ENTRY_ID = "history_entry_id"
        const val KEY_EXPORT_TYPE = "export_type"
    }
}