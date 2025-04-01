package com.example.ht2000obd

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView

class SuggestionDialog(context: Context, private val suggestions: List<String>) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_suggestion)

        val suggestionsTextView: TextView = findViewById(R.id.suggestionsTextView)
        val closeButton: Button = findViewById(R.id.closeButton)

        suggestionsTextView.text = suggestions.joinToString("\n")
        
        closeButton.setOnClickListener {
            dismiss()
        }
    }
}