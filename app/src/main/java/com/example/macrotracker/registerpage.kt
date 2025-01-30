package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class registerpage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registerpage)

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerpageLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val registerButton = findViewById<Button>(R.id.registerButton)

        // Animation - Fade in effect
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1000  // Animation duration (milliseconds)
            startOffset = 300  // Delay before animation starts
            fillAfter = true  // Keep final state after animation
        }
        registerButton.startAnimation(fadeIn)

        // Navigate back to Login Page
        registerButton.setOnClickListener {
            val intent = Intent(this, loginpage::class.java)
            startActivity(intent)
            finish() // Closes register activity so the user can't go back using the back button
        }

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, loginpage::class.java)
            startActivity(intent)
            finish() // Closes register page so it doesn't stack
        }
    }
}
