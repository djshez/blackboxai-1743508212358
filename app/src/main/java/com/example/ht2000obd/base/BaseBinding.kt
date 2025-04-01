package com.example.ht2000obd.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Interface for view binding delegate
 */
interface ViewBindingDelegate<T : ViewBinding> : ReadOnlyProperty<Any, T>

/**
 * Base implementation of view binding delegate
 */
class BaseViewBindingDelegate<T : ViewBinding>(
    private val bindingClass: Class<T>,
    private val inflaterMethod: (LayoutInflater) -> T
) : ViewBindingDelegate<T> {

    private var binding: T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return binding ?: createBinding(thisRef).also { binding = it }
    }

    private fun createBinding(thisRef: Any): T {
        return try {
            val inflateMethod = bindingClass.getMethod(
                "inflate",
                LayoutInflater::class.java
            )
            val layoutInflater = when (thisRef) {
                is android.app.Activity -> thisRef.layoutInflater
                is androidx.fragment.app.Fragment -> thisRef.layoutInflater
                else -> throw IllegalArgumentException("Unsupported binding delegate")
            }
            inflateMethod.invoke(null, layoutInflater) as T
        } catch (e: Exception) {
            throw IllegalStateException("Error creating view binding", e)
        }
    }

    fun clear() {
        binding = null
    }
}

/**
 * Base view holder with view binding
 */
abstract class BaseBindingViewHolder<T : ViewBinding>(
    protected val binding: T
) : RecyclerView.ViewHolder(binding.root)

/**
 * Extension function to create view binding for activities
 */
inline fun <reified T : ViewBinding> androidx.activity.ComponentActivity.viewBinding(
    noinline inflater: (LayoutInflater) -> T
): ViewBindingDelegate<T> {
    return BaseViewBindingDelegate(T::class.java, inflater)
}

/**
 * Extension function to create view binding for fragments
 */
inline fun <reified T : ViewBinding> androidx.fragment.app.Fragment.viewBinding(
    noinline inflater: (LayoutInflater) -> T
): ViewBindingDelegate<T> {
    return BaseViewBindingDelegate(T::class.java, inflater)
}

/**
 * Extension function to create view binding for recycler view items
 */
inline fun <reified T : ViewBinding> ViewGroup.inflateBinding(
    layoutInflater: LayoutInflater = LayoutInflater.from(context),
    attachToParent: Boolean = false
): T {
    val inflateMethod = T::class.java.getMethod(
        "inflate",
        LayoutInflater::class.java,
        ViewGroup::class.java,
        Boolean::class.java
    )
    return inflateMethod.invoke(null, layoutInflater, this, attachToParent) as T
}

/**
 * Base interface for binding data to views
 */
interface ViewBinder<T> {
    fun bind(data: T)
}

/**
 * Base implementation of view binder
 */
abstract class BaseViewBinder<T>(protected val view: View) : ViewBinder<T>

/**
 * Extension function to bind data to view
 */
fun <T> View.bindData(data: T, binder: BaseViewBinder<T>) {
    binder.bind(data)
}

/**
 * Extension function to bind data to recycler view
 */
fun <T> RecyclerView.ViewHolder.bindData(data: T, binder: (T) -> Unit) {
    binder(data)
}

/**
 * Extension function to bind click listener
 */
fun View.onClick(listener: () -> Unit) {
    setOnClickListener { listener() }
}

/**
 * Extension function to bind long click listener
 */
fun View.onLongClick(listener: () -> Boolean) {
    setOnLongClickListener { listener() }
}

/**
 * Extension function to bind visibility
 */
fun View.setVisibility(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * Extension function to bind enabled state
 */
fun View.setEnabled(enabled: Boolean) {
    isEnabled = enabled
    alpha = if (enabled) 1.0f else 0.5f
}

/**
 * Extension function to bind selected state
 */
fun View.setSelected(selected: Boolean) {
    isSelected = selected
}

/**
 * Extension function to bind text
 */
fun android.widget.TextView.setText(text: CharSequence?) {
    this.text = text
}

/**
 * Extension function to bind image resource
 */
fun android.widget.ImageView.setImageResource(resourceId: Int) {
    setImageResource(resourceId)
}

/**
 * Extension function to bind background resource
 */
fun View.setBackgroundResource(resourceId: Int) {
    setBackgroundResource(resourceId)
}

/**
 * Extension function to bind padding
 */
fun View.setPadding(padding: Int) {
    setPadding(padding, padding, padding, padding)
}

/**
 * Extension function to bind margin
 */
fun View.setMargin(margin: Int) {
    val params = layoutParams as? ViewGroup.MarginLayoutParams
    params?.setMargins(margin, margin, margin, margin)
    layoutParams = params
}