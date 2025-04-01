package com.example.ht2000obd.base

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.example.ht2000obd.utils.LogUtils

/**
 * Interface for resource provider
 */
interface ResourceProvider {
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg args: Any): String
    fun getStringArray(@ArrayRes resId: Int): Array<String>
    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg args: Any): String
    @ColorInt fun getColor(@ColorRes resId: Int): Int
    fun getColorStateList(@ColorRes resId: Int): ColorStateList?
    fun getDrawable(@DrawableRes resId: Int): Drawable?
    fun getDimension(@DimenRes resId: Int): Float
    fun getDimensionPixelSize(@DimenRes resId: Int): Int
    fun getFloat(@DimenRes resId: Int): Float
    fun getInteger(@DimenRes resId: Int): Int
    fun getBoolean(resId: Int): Boolean
}

/**
 * Base implementation of resource provider
 */
class BaseResourceProvider(private val context: Context) : ResourceProvider {

    override fun getString(@StringRes resId: Int): String {
        return try {
            context.getString(resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "String resource not found: $resId", e)
            ""
        }
    }

    override fun getString(@StringRes resId: Int, vararg args: Any): String {
        return try {
            context.getString(resId, *args)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "String resource not found: $resId", e)
            ""
        }
    }

    override fun getStringArray(@ArrayRes resId: Int): Array<String> {
        return try {
            context.resources.getStringArray(resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "String array resource not found: $resId", e)
            emptyArray()
        }
    }

    override fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg args: Any): String {
        return try {
            context.resources.getQuantityString(resId, quantity, *args)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Quantity string resource not found: $resId", e)
            ""
        }
    }

    @ColorInt
    override fun getColor(@ColorRes resId: Int): Int {
        return try {
            ContextCompat.getColor(context, resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Color resource not found: $resId", e)
            0
        }
    }

    override fun getColorStateList(@ColorRes resId: Int): ColorStateList? {
        return try {
            ContextCompat.getColorStateList(context, resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Color state list resource not found: $resId", e)
            null
        }
    }

    override fun getDrawable(@DrawableRes resId: Int): Drawable? {
        return try {
            ContextCompat.getDrawable(context, resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Drawable resource not found: $resId", e)
            null
        }
    }

    override fun getDimension(@DimenRes resId: Int): Float {
        return try {
            context.resources.getDimension(resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Dimension resource not found: $resId", e)
            0f
        }
    }

    override fun getDimensionPixelSize(@DimenRes resId: Int): Int {
        return try {
            context.resources.getDimensionPixelSize(resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Dimension pixel size resource not found: $resId", e)
            0
        }
    }

    override fun getFloat(@DimenRes resId: Int): Float {
        return try {
            val value = TypedValue()
            context.resources.getValue(resId, value, true)
            value.float
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Float resource not found: $resId", e)
            0f
        }
    }

    override fun getInteger(@DimenRes resId: Int): Int {
        return try {
            context.resources.getInteger(resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Integer resource not found: $resId", e)
            0
        }
    }

    override fun getBoolean(resId: Int): Boolean {
        return try {
            context.resources.getBoolean(resId)
        } catch (e: Resources.NotFoundException) {
            LogUtils.e("Resources", "Boolean resource not found: $resId", e)
            false
        }
    }

    /**
     * Extension functions for resource access
     */
    companion object {
        fun Context.dpToPx(dp: Float): Float {
            return dp * resources.displayMetrics.density
        }

        fun Context.pxToDp(px: Float): Float {
            return px / resources.displayMetrics.density
        }

        fun Context.spToPx(sp: Float): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                resources.displayMetrics
            )
        }

        fun Context.pxToSp(px: Float): Float {
            return px / resources.displayMetrics.scaledDensity
        }

        fun Context.getThemeColor(attr: Int): Int {
            val typedValue = TypedValue()
            theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }

        fun Context.getThemeDrawable(attr: Int): Drawable? {
            val typedValue = TypedValue()
            theme.resolveAttribute(attr, typedValue, true)
            return ContextCompat.getDrawable(this, typedValue.resourceId)
        }

        fun Context.getThemeDimension(attr: Int): Float {
            val typedValue = TypedValue()
            theme.resolveAttribute(attr, typedValue, true)
            return TypedValue.complexToDimension(typedValue.data, resources.displayMetrics)
        }
    }
}