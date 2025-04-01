package com.example.ht2000obd.base

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import com.example.ht2000obd.R
import com.example.ht2000obd.utils.LogUtils

/**
 * Interface for navigation commands
 */
sealed class NavigationCommand {
    data class To(
        val directions: NavDirections,
        val options: NavOptions? = null
    ) : NavigationCommand()

    data class ToRes(
        @IdRes val destinationId: Int,
        val args: Bundle? = null,
        val options: NavOptions? = null
    ) : NavigationCommand()

    data class BackTo(
        @IdRes val destinationId: Int,
        val inclusive: Boolean = false
    ) : NavigationCommand()

    object Back : NavigationCommand()
    object ToRoot : NavigationCommand()
}

/**
 * Interface for navigation handler
 */
interface NavigationHandler {
    fun navigate(command: NavigationCommand)
}

/**
 * Base implementation of navigation handler
 */
class BaseNavigationHandler(
    private val navController: NavController
) : NavigationHandler {

    override fun navigate(command: NavigationCommand) {
        try {
            when (command) {
                is NavigationCommand.To -> {
                    navController.navigate(command.directions, command.options)
                }
                is NavigationCommand.ToRes -> {
                    navController.navigate(
                        command.destinationId,
                        command.args,
                        command.options
                    )
                }
                is NavigationCommand.BackTo -> {
                    navController.popBackStack(
                        command.destinationId,
                        command.inclusive
                    )
                }
                is NavigationCommand.Back -> {
                    navController.popBackStack()
                }
                is NavigationCommand.ToRoot -> {
                    navController.popBackStack(
                        navController.graph.startDestinationId,
                        false
                    )
                }
            }
        } catch (e: Exception) {
            LogUtils.e("Navigation", "Error navigating", e)
        }
    }
}

/**
 * Navigation options builder
 */
object NavigationOptionsBuilder {
    fun defaultNavOptions(): NavOptions {
        return NavOptions.Builder()
            .setEnterAnim(R.anim.nav_default_enter_anim)
            .setExitAnim(R.anim.nav_default_exit_anim)
            .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
            .build()
    }

    fun slideNavOptions(): NavOptions {
        return NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
    }

    fun fadeNavOptions(): NavOptions {
        return NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.fade_out)
            .setPopEnterAnim(R.anim.fade_in)
            .setPopExitAnim(R.anim.fade_out)
            .build()
    }

    fun builder(): NavOptions.Builder {
        return NavOptions.Builder()
    }
}

/**
 * Extension functions for navigation
 */
fun NavController.navigateSafely(
    directions: NavDirections,
    options: NavOptions? = null
) {
    try {
        navigate(directions, options)
    } catch (e: Exception) {
        LogUtils.e("Navigation", "Error navigating", e)
    }
}

fun NavController.navigateSafely(
    @IdRes resId: Int,
    args: Bundle? = null,
    options: NavOptions? = null
) {
    try {
        navigate(resId, args, options)
    } catch (e: Exception) {
        LogUtils.e("Navigation", "Error navigating", e)
    }
}

fun NavController.popBackStackSafely(
    @IdRes destinationId: Int,
    inclusive: Boolean = false
): Boolean {
    return try {
        popBackStack(destinationId, inclusive)
    } catch (e: Exception) {
        LogUtils.e("Navigation", "Error popping back stack", e)
        false
    }
}

/**
 * Navigation route constants
 */
object NavigationRoutes {
    // Main navigation
    const val DASHBOARD = "dashboard"
    const val CODE_SCAN = "code_scan"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    // Dashboard sub-navigation
    const val DASHBOARD_DETAIL = "dashboard_detail"
    const val DASHBOARD_SETTINGS = "dashboard_settings"

    // Code scan sub-navigation
    const val CODE_SCAN_RESULT = "code_scan_result"
    const val CODE_SCAN_HISTORY = "code_scan_history"

    // History sub-navigation
    const val HISTORY_DETAIL = "history_detail"
    const val HISTORY_FILTER = "history_filter"

    // Settings sub-navigation
    const val SETTINGS_PROFILE = "settings_profile"
    const val SETTINGS_BLUETOOTH = "settings_bluetooth"
    const val SETTINGS_ABOUT = "settings_about"
}

/**
 * Navigation argument keys
 */
object NavigationArgs {
    const val ITEM_ID = "item_id"
    const val ITEM_TYPE = "item_type"
    const val ITEM_DATA = "item_data"
    const val SHOW_BACK = "show_back"
    const val TITLE = "title"
    const val MESSAGE = "message"
    const val RESULT = "result"
}