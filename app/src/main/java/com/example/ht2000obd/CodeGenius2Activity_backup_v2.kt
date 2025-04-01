package com.example.ht2000obd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ht2000obd.databinding.ActivityCodeGenius2Binding

class CodeGenius2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityCodeGenius2Binding
    private lateinit var aiHelper: AIHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCodeGenius2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AIHelper
        aiHelper = AIHelper()

        // Initialize UI components and features for CodeGenius2
        setupUI()
    }

    private fun setupUI() {
        // Set up the user interface for CodeGenius2
        // This will include buttons, text fields, and other components

        binding.startCodingButton.setOnClickListener {
            // Example usage of AIHelper to get coding suggestions
            val suggestions = aiHelper.getCodingSuggestions("Sample code snippet")
            // Display suggestions to the user using SuggestionDialog
            val suggestionDialog = SuggestionDialog(this, suggestions)
            suggestionDialog.show()
        }

        binding.manageProjectsButton.setOnClickListener {
            // Handle project management functionality
        }

        binding.aiAssistanceButton.setOnClickListener {
            // Handle AI assistance functionality
        }
    }
}