package com.example.ht2000obd.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.ht2000obd.utils.LogUtils
import com.example.ht2000obd.utils.UIUtils.setThrottledClickListener

abstract class BaseAdapter<T : Any, VB : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseAdapter.BaseViewHolder<VB>>(diffCallback) {

    // Abstract methods to be implemented by child adapters
    abstract fun createBinding(parent: ViewGroup): VB
    abstract fun bindItem(binding: VB, item: T, position: Int)

    // Optional click listener
    private var itemClickListener: ((T) -> Unit)? = null

    // Optional long click listener
    private var itemLongClickListener: ((T) -> Boolean)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = createBinding(parent)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        try {
            val item = getItem(position)
            bindItem(holder.binding, item, position)

            // Set click listeners if provided
            itemClickListener?.let { listener ->
                holder.binding.root.setThrottledClickListener {
                    listener(item)
                }
            }

            itemLongClickListener?.let { listener ->
                holder.binding.root.setOnLongClickListener {
                    listener(item)
                }
            }
        } catch (e: Exception) {
            LogUtils.e("BaseAdapter", "Error binding view holder at position $position", e)
        }
    }

    // Method to set click listener
    fun setOnItemClickListener(listener: (T) -> Unit) {
        itemClickListener = listener
    }

    // Method to set long click listener
    fun setOnItemLongClickListener(listener: (T) -> Boolean) {
        itemLongClickListener = listener
    }

    // ViewHolder class
    class BaseViewHolder<VB : ViewBinding>(
        val binding: VB
    ) : RecyclerView.ViewHolder(binding.root)

    // Utility methods for list operations
    fun updateItems(newItems: List<T>) {
        submitList(newItems)
    }

    fun addItem(item: T) {
        val currentList = currentList.toMutableList()
        currentList.add(item)
        submitList(currentList)
    }

    fun removeItem(item: T) {
        val currentList = currentList.toMutableList()
        currentList.remove(item)
        submitList(currentList)
    }

    fun removeItemAt(position: Int) {
        if (position in 0 until itemCount) {
            val currentList = currentList.toMutableList()
            currentList.removeAt(position)
            submitList(currentList)
        }
    }

    fun clearItems() {
        submitList(emptyList())
    }

    // Companion object for creating DiffUtil.ItemCallback
    companion object {
        fun <T : Any> createDiffCallback(
            areItemsTheSame: (T, T) -> Boolean,
            areContentsTheSame: (T, T) -> Boolean
        ): DiffUtil.ItemCallback<T> {
            return object : DiffUtil.ItemCallback<T>() {
                override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
                    return areItemsTheSame(oldItem, newItem)
                }

                override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
                    return areContentsTheSame(oldItem, newItem)
                }
            }
        }
    }

    // Extension function to create ViewBinding
    protected fun ViewGroup.inflateBinding(
        inflater: (LayoutInflater, ViewGroup, Boolean) -> VB
    ): VB {
        return inflater(
            LayoutInflater.from(context),
            this,
            false
        )
    }

    // Extension function to safely get item at position
    protected fun getItemSafely(position: Int): T? {
        return if (position in 0 until itemCount) {
            getItem(position)
        } else {
            null
        }
    }

    // Extension function to check if position is valid
    protected fun isValidPosition(position: Int): Boolean {
        return position in 0 until itemCount
    }

    // Extension function to get the last position
    protected val lastPosition: Int
        get() = itemCount - 1

    // Extension function to check if adapter is empty
    val isEmpty: Boolean
        get() = itemCount == 0
}