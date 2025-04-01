package com.example.ht2000obd

import android.os.Bundle
import android.content.Intent
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
        binding.startCodingButton.setOnClickListener {
            val suggestions = aiHelper.getCodingSuggestions("Sample code snippet")
            val suggestionDialog = SuggestionDialog(this, suggestions)
            suggestionDialog.show()
        }

        binding.manageProjectsButton.setOnClickListener {
            val intent = Intent(this, ManageProjectsActivity::class.java)
            startActivity(intent)
        }

        binding.aiAssistanceButton.setOnClickListener {
            val intent = Intent(this, AIAssistanceActivity::class.java)
            startActivity(intent)
        }
    }
}