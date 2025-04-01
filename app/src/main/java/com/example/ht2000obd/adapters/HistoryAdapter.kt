package com.example.ht2000obd.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ht2000obd.databinding.ItemHistoryBinding
import com.example.ht2000obd.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (HistoryEntry) -> Unit,
    private val onDeleteClick: (HistoryEntry) -> Unit
) : ListAdapter<HistoryEntry, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        private val binding: ItemHistoryBinding,
        private val onItemClick: (HistoryEntry) -> Unit,
        private val onDeleteClick: (HistoryEntry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

        fun bind(entry: HistoryEntry) {
            binding.apply {
                // Set timestamp
                textTimestamp.text = dateFormat.format(Date(entry.timestamp))

                // Set primary metrics
                textRpm.text = "${entry.rpm}"
                textSpeed.text = "${entry.speed} km/h"
                textTemp.text = "${entry.coolantTemp}°C"

                // Set trouble codes
                if (entry.troubleCodes.isEmpty()) {
                    textTroubleCodes.text = "No trouble codes"
                } else {
                    textTroubleCodes.text = entry.troubleCodes.joinToString("\n")
                }

                // Set click listeners
                root.setOnClickListener {
                    onItemClick(entry)
                }

                buttonDelete.setOnClickListener {
                    onDeleteClick(entry)
                }
            }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryEntry>() {
        override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val TAG = "HistoryAdapter"
    }
}