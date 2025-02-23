package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class cookpal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cookpal)

        // Apply edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Animate the advertisement logo (assumed to have ID "cookpalLogo")
        val logoImageView = findViewById<ImageView>(R.id.cookpalLogo)
        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        logoImageView.startAnimation(scaleAnimation)

        // Animate the back button with fade in
        val backButton = findViewById<ImageView>(R.id.backButton)
        val fadeAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        backButton.startAnimation(fadeAnimation)

        // Implement back button: when tapped, navigate to userprofile activity.
        backButton.setOnClickListener {
            startActivity(Intent(this, userprofile::class.java))
            finish()
        }
    }
}
