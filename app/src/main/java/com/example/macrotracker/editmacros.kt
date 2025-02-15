package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class editmacros : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editmacros)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editmacrosLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Link EditTexts for user inputs
        val caloriesInput = findViewById<EditText>(R.id.caloriesInput)
        val proteinInput = findViewById<EditText>(R.id.proteinInput)
        val carbsInput = findViewById<EditText>(R.id.carbsInput)
        val fatsInput = findViewById<EditText>(R.id.fatsInput)

        // Save Button: update macros in Firestore
        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            // Get text from inputs
            val caloriesStr = caloriesInput.text.toString()
            val proteinStr = proteinInput.text.toString()
            val carbsStr = carbsInput.text.toString()
            val fatsStr = fatsInput.text.toString()

            // Validate that none of the fields are empty
            if (caloriesStr.isBlank() || proteinStr.isBlank() || carbsStr.isBlank() || fatsStr.isBlank()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse inputs to integers
            val calories = caloriesStr.toInt()
            val protein = proteinStr.toInt()
            val carbs = carbsStr.toInt()
            val fats = fatsStr.toInt()

            // Create a Macros object
            val macros = Macros(calories, protein, carbs, fats)

            // Save the macros to Firestore (collection "userMacros", document "macros")
            db.collection("userMacros").document("macros")
                .set(macros)
                .addOnSuccessListener {
                    Toast.makeText(this, "Macros updated", Toast.LENGTH_SHORT).show()
                    // Navigate back to landing page (which should fetch the updated macros)
                    val intent = Intent(this, landingpage::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update macros", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        }

        // Back Button: navigate back without saving changes
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, landingpage::class.java)
            startActivity(intent)
            finish()
        }
    }
}
