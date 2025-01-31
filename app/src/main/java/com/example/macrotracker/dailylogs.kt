package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class dailylogs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dailylogs)

        // Ensure View is properly set before finding elements
        val backButton = findViewById<ImageView>(R.id.backButton)

        backButton.setOnClickListener {
            val intent = Intent(this, landingpage::class.java)
            startActivity(intent)
            finish() // Optional: Closes this activity so user can't go back
        }
    }
}
