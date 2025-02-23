package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class registerpage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registerpage)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerpageLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val registerButton = findViewById<Button>(R.id.registerButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val backButton = findViewById<ImageView>(R.id.backButton)

        // Apply Fade In Animation to the Register Button
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1000
            startOffset = 300
            fillAfter = true
        }
        registerButton.startAnimation(fadeIn)

        // Password Toggle Functionality: Toggle visibility on touch of drawable end.
        var passwordVisible = false
        passwordInput.setOnTouchListener { _, event ->
            // DRAWABLE_END is at index 2 in the compound drawables array
            val DRAWABLE_END = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordInput.right - passwordInput.compoundDrawables[DRAWABLE_END].bounds.width())) {
                    passwordVisible = !passwordVisible
                    if (passwordVisible) {
                        // Show password and update icon
                        passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_24, 0)
                    } else {
                        // Hide password and update icon
                        passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                        passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_off_24, 0)
                    }
                    // Keep cursor at the end of the text
                    passwordInput.setSelection(passwordInput.text.length)
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Register Button Click
        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate email and password inputs.
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Reject emails that appear to be dummy accounts (customize as needed)
            if (email.contains("dummy", ignoreCase = true)) {
                Toast.makeText(this, "Please use a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use a default username ("User") which can be updated later; also set "usernameUpdated" flag to false.
            registerUser("User", email, password)
        }

        // Back Button Click
        backButton.setOnClickListener {
            startActivity(Intent(this, loginpage::class.java))
            finish()
        }
    }

    /**
     * Registers the user with Firebase Authentication and stores their data in Firestore.
     * Also sends a verification email after registration.
     */
    private fun registerUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    saveUserData(userId, username, email)
                    // Send email verification
                    auth.currentUser?.sendEmailVerification()
                        ?.addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Registration successful! Please check your email to verify your account.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        ?.addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to send verification email: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    // Sign the user out until they verify their email.
                    auth.signOut()
                    startActivity(Intent(this, loginpage::class.java))
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Saves user data to Firestore under "users/{userID}" using merge so as not to overwrite subcollections.
     * Also sets the "usernameUpdated" flag to false.
     */
    private fun saveUserData(userId: String, username: String, email: String) {
        val userData = hashMapOf(
            "username" to username,
            "email" to email,
            "usernameUpdated" to false
        )

        val userRef = db.collection("users").document(userId)
        userRef.set(userData, SetOptions.merge())
            .addOnSuccessListener {
                setupUserCollections(userId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Sets up empty subcollections for daily logs and user macros.
     */
    private fun setupUserCollections(userId: String) {
        val userRef = db.collection("users").document(userId)

        val defaultMacros = hashMapOf(
            "calories" to 2000,
            "protein" to 150,
            "carbs" to 250,
            "fats" to 50
        )

        userRef.collection("userMacros").document("macros")
            .set(defaultMacros)
            .addOnSuccessListener {
                // No additional action required.
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error initializing macros: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
