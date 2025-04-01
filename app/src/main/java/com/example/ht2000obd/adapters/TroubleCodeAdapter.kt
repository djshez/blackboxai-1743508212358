package com.example.ht2000obd.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ht2000obd.databinding.ItemTroubleCodeBinding

class TroubleCodeAdapter(
    private val onCodeClick: (String) -> Unit
) : ListAdapter<String, TroubleCodeAdapter.TroubleCodeViewHolder>(TroubleCodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TroubleCodeViewHolder {
        val binding = ItemTroubleCodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TroubleCodeViewHolder(binding, onCodeClick)
    }

    override fun onBindViewHolder(holder: TroubleCodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TroubleCodeViewHolder(
        private val binding: ItemTroubleCodeBinding,
        private val onCodeClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(code: String) {
            binding.apply {
                textCode.text = code
                textDescription.text = getBasicDescription(code)
                
                // Set click listeners
                buttonInfo.setOnClickListener {
                    onCodeClick(code)
                }
                
                root.setOnClickListener {
                    onCodeClick(code)
                }
            }
        }

        private fun getBasicDescription(code: String): String {
            // Basic description based on code type
            return when (code.first()) {
                'P' -> "Powertrain - Related to engine, transmission, and associated systems"
                'C' -> "Chassis - Related to brakes, steering, suspension, and other chassis systems"
                'B' -> "Body - Related to airbags, seatbelts, and other body systems"
                'U' -> "Network - Related to computer systems and network communications"
                else -> "Unknown system"
            }
        }
    }

    private class TroubleCodeDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}