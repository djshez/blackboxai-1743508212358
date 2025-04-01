package com.example.ht2000obd

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ht2000obd.databinding.ActivityManageProjectsBinding

class ManageProjectsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageProjectsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI components and features for managing projects
        setupUI()
    }

    private fun setupUI() {
        // Set up the user interface for managing projects
        binding.createProjectButton.setOnClickListener {
            createNewProject()
        }
    }

    private fun createNewProject() {
        // Placeholder for creating a new project
        Toast.makeText(this, "New project created!", Toast.LENGTH_SHORT).show()
    }
}