package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class userprofile : AppCompatActivity() {

    private lateinit var dailyLogsHistoryBox: LinearLayout
    private lateinit var nutritionSettingsBox: LinearLayout
    private lateinit var supportUsBox: LinearLayout
    private lateinit var shareButton: Button
    private lateinit var logoutButton: Button
    private lateinit var backButton: ImageView
    private lateinit var usernameTextView: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userprofile) // Ensure your XML filename matches

        // Initialize UI elements
        dailyLogsHistoryBox = findViewById(R.id.dailylogsHistorygridBox1)
        nutritionSettingsBox = findViewById(R.id.nutritionSettinggridBox2)
        supportUsBox = findViewById(R.id.buyPremiumgridBox3)
        shareButton = findViewById(R.id.shareButton)
        logoutButton = findViewById(R.id.logoutButton)
        backButton = findViewById(R.id.backButton)
        usernameTextView = findViewById(R.id.usernameTextView)

        // Fetch and display username from Firestore
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") ?: "User"
                        usernameTextView.text = username
                    } else {
                        usernameTextView.text = "New User"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching username", Toast.LENGTH_SHORT).show()
                    Log.e("UserProfile", "Error fetching username", e)
                }
        }

        // Navigation for Daily Logs History
        dailyLogsHistoryBox.setOnClickListener {
            startActivity(Intent(this, daily_logs_history::class.java))
        }

        // Navigation for Nutrition Settings
        nutritionSettingsBox.setOnClickListener {
            startActivity(Intent(this, NutritionSettingsActivity::class.java))
        }

        // Navigation for Support Us / Buy Premium
        supportUsBox.setOnClickListener {
            startActivity(Intent(this, support_us_activity::class.java))
        }

        // Share Button: Open the feedbox dialog
        shareButton.setOnClickListener {
            showFeedboxDialog()
        }

        // Logout button with confirmation dialog
        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Back button: Return to landing page
        backButton.setOnClickListener {
            startActivity(Intent(this, landingpage::class.java))
            finish() // Close current activity
        }
    }

    /**
     * Displays the feedbox dialog using the layout 'feedbox_dialog.xml'
     */
    private fun showFeedboxDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.feedbox_dialog, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Example: if there's a close button in your feedbox dialog
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)
        closeButton?.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    /**
     * Shows a confirmation dialog for logout.
     */
    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.logout_dialog, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Assume your logout_user_dialog.xml has two buttons with these IDs:
        // - logoutButton (for confirming logout)
        // - cancelButton (for cancelling)
        val logoutBtn = dialogView.findViewById<Button>(R.id.confirmButton)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelButton)

        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            alertDialog.dismiss()
        }

        cancelBtn.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

}
