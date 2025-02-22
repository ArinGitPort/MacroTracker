package com.example.macrotracker

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class support_us_activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_support_us)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back Button Functionality
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Closes the current activity and returns to the previous one
        }

        // Load PayMongo Logo using Glide
        val paymongoLogo = findViewById<ImageView>(R.id.paymongoLogo)
        Glide.with(this)
            .load(R.drawable.paymongo_logo) // Replace with actual image URL or drawable
            .placeholder(R.drawable.paymongo_logo) // Optional: Placeholder while loading
            .error(R.drawable.paymongo_logo) // Optional: Image if loading fails
            .into(paymongoLogo)
    }
}
