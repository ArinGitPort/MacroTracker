package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
    // Ensure that the view with id "usernameTextView" in your layout is an EditText
    private lateinit var usernameEditText: EditText

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userprofile)

        // Initialize UI elements
        dailyLogsHistoryBox = findViewById(R.id.dailylogsHistorygridBox1)
        nutritionSettingsBox = findViewById(R.id.nutritionSettinggridBox2)
        supportUsBox = findViewById(R.id.buyPremiumgridBox3)
        shareButton = findViewById(R.id.shareButton)
        logoutButton = findViewById(R.id.logoutButton)
        backButton = findViewById(R.id.backButton)
        usernameEditText = findViewById(R.id.usernameTextView)

        // Fetch and display username from Firestore
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Retrieve the "username" field. If it's empty or equals the UID, use a default value.
                        val username = document.getString("username")
                        if (username.isNullOrEmpty() || username == uid) {
                            usernameEditText.setText("User")
                        } else {
                            usernameEditText.setText(username)
                        }
                    } else {
                        usernameEditText.setText("New User")
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error fetching username", Toast.LENGTH_SHORT).show()
                }
        }

        // When the user taps the username field, show the edit username dialog.
        usernameEditText.setOnClickListener {
            showEditUsernameDialog()
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
            finish()
        }
    }

    /**
     * Displays a dialog to allow the user to edit their username.
     * The dialog layout is defined in edit_username_dialog.xml.
     */
    private fun showEditUsernameDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_username_dialog, null)
        val usernameInput = dialogView.findViewById<EditText>(R.id.editUsernameInput)
        // Pre-populate with the current username.
        usernameInput.setText(usernameEditText.text.toString())

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Edit Username")
            .setView(dialogView)
            .create()

        val updateButton = dialogView.findViewById<Button>(R.id.updateButton)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        updateButton.setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            if (newUsername.isNotEmpty()) {
                updateUsername(newUsername)
                usernameEditText.setText(newUsername)
            } else {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            }
            alertDialog.dismiss()
        }

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    /**
     * Updates the username in Firestore.
     */
    private fun updateUsername(newUsername: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("username", newUsername)
            .addOnSuccessListener {
                Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Displays the feedbox dialog using the layout 'feedbox_dialog.xml'.
     * When the user taps "Send", the feedback message is sent to Firestore.
     */
    private fun showFeedboxDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.feedbox_dialog, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val feedbackInput = dialogView.findViewById<EditText>(R.id.feedbackInput)
        val sendButton = dialogView.findViewById<Button>(R.id.sendButton)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        sendButton.setOnClickListener {
            val feedback = feedbackInput.text.toString().trim()
            if (feedback.isNotEmpty()) {
                // Use the username from Firestore if available; here we simply use the uid as fallback.
                val currentUsername = usernameEditText.text.toString().ifEmpty { "Anonymous" }
                val feedbackData = hashMapOf(
                    "username" to currentUsername,
                    "feedback" to feedback,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                db.collection("feedbox").add(feedbackData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Feedback sent", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send feedback: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show()
            }
        }

        closeButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    /**
     * Shows a confirmation dialog for logout using a custom layout (logout_dialog.xml).
     */
    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.logout_dialog, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val logoutBtn = dialogView.findViewById<Button>(R.id.confirmButton)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelButton)

        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, loginpage::class.java)
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
