package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class registerpage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registerpage)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerpageLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val registerButton = findViewById<Button>(R.id.registerButton)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        // Apply Fade In Animation to the Register Button
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1000
            startOffset = 300
            fillAfter = true
        }
        registerButton.startAnimation(fadeIn)

        // Register Button Click
        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(username, email, password)
        }

        // Back Button Click
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this, loginpage::class.java))
            finish()
        }
    }

    /**
     * Register the user in Firebase Authentication and store their data in Firestore.
     */
    private fun registerUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    saveUserData(userId, username, email)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Save user data to Firestore under "users/{userID}"
     */
    private fun saveUserData(userId: String, username: String, email: String) {
        val userData = hashMapOf(
            "username" to username,
            "email" to email
        )

        val userRef = db.collection("users").document(userId)

        userRef.set(userData)
            .addOnSuccessListener {
                setupUserCollections(userId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Setup empty subcollections for daily logs and user macros.
     */
    private fun setupUserCollections(userId: String) {
        val userRef = db.collection("users").document(userId)

        // Initialize empty macros
        val defaultMacros = hashMapOf(
            "calories" to 2000,
            "protein" to 150,
            "carbs" to 250,
            "fats" to 50
        )

        userRef.collection("userMacros").document("macros")
            .set(defaultMacros)
            .addOnSuccessListener {
                Toast.makeText(this, "User registered successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, loginpage::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error initializing macros: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
