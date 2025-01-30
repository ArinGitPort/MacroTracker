package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the next button
        val nextButton = findViewById<Button>(R.id.nextButton)

        // Apply fade-in pop-out animation with a delay
        nextButton.alpha = 0f // Start invisible
        nextButton.postDelayed({
            nextButton.animate()
                .alpha(1f)  // Fully visible
                .scaleX(1.1f)  // Slight pop-out effect
                .scaleY(1.1f)
                .setDuration(2000) // Animation duration (800ms)
                .withEndAction {
                    // Return to normal size after pop-out
                    nextButton.animate().scaleX(1f).scaleY(1f).setDuration(200)
                }
                .start()
        }, 500) // Delay of 500ms before animation starts

        // Set up button click listener
        nextButton.setOnClickListener {
            val intent = Intent(this, loginpage::class.java)
            startActivity(intent)
        }
    }
}
