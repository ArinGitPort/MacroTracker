package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NutritionSettingsActivity : AppCompatActivity() {

    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var goalWeightInput: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var exerciseFrequencySpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutrition_settings)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get UI elements from XML
        heightInput = findViewById(R.id.heightInput)
        weightInput = findViewById(R.id.weightInput)
        goalWeightInput = findViewById(R.id.goalWeightInput)
        genderSpinner = findViewById(R.id.genderSpinner)
        exerciseFrequencySpinner = findViewById(R.id.exerciseFrequencySpinner)
        saveButton = findViewById(R.id.saveNutritionSettingsButton)
        backButton = findViewById(R.id.backButton)

        // Get the current user's ID
        val userId = auth.currentUser?.uid
        if (userId != null) {
            fetchUserNutritionData(userId)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        // When Save is clicked, update the user's nutrition settings in Firestore
        saveButton.setOnClickListener {
            if (userId != null) {
                saveUserNutritionData(userId)
            }
        }

        // Back button navigates back to the UserProfileActivity
        backButton.setOnClickListener {
            startActivity(Intent(this, userprofile::class.java))
            finish()
        }
    }

    // Fetch existing nutrition data and fill the fields.
    private fun fetchUserNutritionData(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Populate the fields with saved data
                    heightInput.setText(document.getString("height"))
                    weightInput.setText(document.getString("weight"))
                    goalWeightInput.setText(document.getString("goalWeight"))

                    // Set the spinner for gender
                    val gender = document.getString("gender")
                    val genderOptions = resources.getStringArray(R.array.gender_options)
                    val genderIndex = genderOptions.indexOf(gender)
                    if (genderIndex >= 0) {
                        genderSpinner.setSelection(genderIndex)
                    }

                    // Set the spinner for exercise frequency
                    val exercise = document.getString("exerciseFrequency")
                    val exerciseOptions = resources.getStringArray(R.array.activity_levels)
                    val exerciseIndex = exerciseOptions.indexOf(exercise)
                    if (exerciseIndex >= 0) {
                        exerciseFrequencySpinner.setSelection(exerciseIndex)
                    }

                    // Optionally, compute macros after fetching data.
                    computeUserMacros(userId)
                } else {
                    Log.d("NutritionSettings", "No user data found.")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
                Log.e("NutritionSettings", "Error fetching data", e)
            }
    }

    // Save the nutrition settings to Firestore.
    private fun saveUserNutritionData(userId: String) {
        val nutritionData = hashMapOf(
            "height" to heightInput.text.toString(),
            "weight" to weightInput.text.toString(),
            "goalWeight" to goalWeightInput.text.toString(),
            "gender" to genderSpinner.selectedItem.toString(),
            "exerciseFrequency" to exerciseFrequencySpinner.selectedItem.toString()
        )

        db.collection("users").document(userId)
            .set(nutritionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Nutrition settings saved!", Toast.LENGTH_SHORT).show()
                computeUserMacros(userId) // Recompute macros after saving
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save settings", Toast.LENGTH_SHORT).show()
                Log.e("NutritionSettings", "Error saving data", e)
            }
    }

    // Example function to compute macros (calories, protein, carbs, fats) using basic formulas.
    private fun computeUserMacros(userId: String) {
        val height = heightInput.text.toString().toDoubleOrNull() ?: 0.0
        val weight = weightInput.text.toString().toDoubleOrNull() ?: 0.0
        val age = 25 // Replace with an input or retrieve from user profile if available

        val gender = genderSpinner.selectedItem.toString()
        val exerciseLevel = exerciseFrequencySpinner.selectedItem.toString()

        // Calculate Basal Metabolic Rate (BMR) using the Mifflin-St Jeor Equation
        val bmr = if (gender.equals("Male", ignoreCase = true)) {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }

        // Determine activity factor based on exercise frequency
        val activityFactor = when (exerciseLevel) {
            "Sedentary (Little to no exercise)" -> 1.2
            "Lightly Active (1-3 days/week)" -> 1.375
            "Moderately Active (3-5 days/week)" -> 1.55
            "Very Active (6-7 days/week)" -> 1.725
            "Super Active (Athlete level)" -> 1.9
            else -> 1.2
        }

        // Compute daily calorie needs
        val dailyCalories = (bmr * activityFactor).toInt()

        // Example macros: protein (g), carbs (g), fats (g)
        val protein = (weight * 2.2).toInt()         // Rough estimate: 2.2 grams per kg
        val carbs = (dailyCalories * 0.5 / 4).toInt()  // 50% of calories from carbs (4 cal per gram)
        val fats = (dailyCalories * 0.3 / 9).toInt()   // 30% of calories from fat (9 cal per gram)

        // Save computed macros to Firestore under a separate collection "userMacros"
        val updatedMacros = hashMapOf(
            "calories" to dailyCalories,
            "protein" to protein,
            "carbs" to carbs,
            "fats" to fats
        )

        db.collection("userMacros").document(userId)
            .set(updatedMacros)
            .addOnSuccessListener {
                Log.d("NutritionSettings", "Macros updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("NutritionSettings", "Error updating macros", e)
            }
    }
}
