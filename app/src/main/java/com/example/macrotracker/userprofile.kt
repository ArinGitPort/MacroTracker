package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class userprofile : AppCompatActivity() {

    private lateinit var dailyLogsHistoryBox: LinearLayout
    private lateinit var nutritionSettingsBox: LinearLayout
    private lateinit var supportUsBox: LinearLayout
    private lateinit var logoutButton: Button
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userprofile) // Ensure your XML filename matches

        // Initialize UI elements
        dailyLogsHistoryBox = findViewById(R.id.dailylogsHistorygridBox1)
        nutritionSettingsBox = findViewById(R.id.nutritionSettinggridBox2)
        supportUsBox = findViewById(R.id.buyPremiumgridBox3)
        logoutButton = findViewById(R.id.logoutButton)
        backButton = findViewById(R.id.backButton)

        // Navigate to Daily Logs History Activity
        dailyLogsHistoryBox.setOnClickListener {
            val intent = Intent(this, daily_logs_history::class.java)
            startActivity(intent)
        }

        // Navigate to Nutrition Settings Activity
        nutritionSettingsBox.setOnClickListener {
            val intent = Intent(this, NutritionSettingsActivity::class.java)
            startActivity(intent)
        }

        // Navigate to Support Us / Buy Premium Activity
        supportUsBox.setOnClickListener {
            val intent = Intent(this, support_us_activity::class.java)
            startActivity(intent)
        }

        // Logout functionality - Logs out user and redirects to MainActivity
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Firebase logout
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears back stack
            startActivity(intent)
            finish() // Close current activity
        }

        // Back button to go back to Landing Page
        backButton.setOnClickListener {
            val intent = Intent(this, landingpage::class.java)
            startActivity(intent)
            finish() // Close current activity
        }
    }
}
