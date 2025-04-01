package com.example.ht2000obd

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ht2000obd.databinding.ActivityAIAssistanceBinding

class AIAssistanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAIAssistanceBinding
    private lateinit var aiHelper: AIHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAIAssistanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AIHelper
        aiHelper = AIHelper()

        // Initialize UI components and features for AI assistance
        setupUI()
    }

    private fun setupUI() {
        binding.getSuggestionsButton.setOnClickListener {
            val codeInput = binding.codeInput.text.toString()
            val suggestions = aiHelper.getCodingSuggestions(codeInput)
            // Display suggestions to the user (this can be done via a dialog or a new activity)
            val suggestionDialog = SuggestionDialog(this, suggestions)
            suggestionDialog.show()
        }
    }
}