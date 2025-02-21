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

        // Initialize Firebase
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

        // Save settings when Save is clicked
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

    /**
     * Fetches existing nutrition settings for the given user and populates the fields.
     */
    private fun fetchUserNutritionData(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    heightInput.setText(document.getString("height"))
                    weightInput.setText(document.getString("weight"))
                    goalWeightInput.setText(document.getString("goalWeight"))

                    val gender = document.getString("gender")
                    val genderOptions = resources.getStringArray(R.array.gender_options)
                    val genderIndex = genderOptions.indexOf(gender)
                    if (genderIndex >= 0) {
                        genderSpinner.setSelection(genderIndex)
                    }

                    val exercise = document.getString("exerciseFrequency")
                    val exerciseOptions = resources.getStringArray(R.array.activity_levels)
                    val exerciseIndex = exerciseOptions.indexOf(exercise)
                    if (exerciseIndex >= 0) {
                        exerciseFrequencySpinner.setSelection(exerciseIndex)
                    }

                    // Optionally, compute macros automatically after fetching settings.
                    computeUserMacros(userId)
                } else {
                    Log.d("NutritionSettings", "No user data found. Likely first login.")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
                Log.e("NutritionSettings", "Error fetching data", e)
            }
    }

    /**
     * Saves the nutrition settings under users/{userId} and then computes macros.
     */
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

    /**
     * Computes the user's macro goals (calories, protein, carbs, fats) using the Mifflin-St Jeor Equation
     * and target percentages (approx. 32% protein, 42% carbs, 26% fat), then saves the result under
     * users/{userId}/userMacros/macros.
     */
    private fun computeUserMacros(userId: String) {
        val height = heightInput.text.toString().toDoubleOrNull() ?: 0.0
        val weight = weightInput.text.toString().toDoubleOrNull() ?: 0.0
        val age = 25 // Replace with a user input if available

        val gender = genderSpinner.selectedItem.toString()
        val exerciseLevel = exerciseFrequencySpinner.selectedItem.toString()

        // Calculate BMR using the Mifflin-St Jeor Equation
        val bmr = if (gender.equals("Male", ignoreCase = true)) {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }

        val activityFactor = when (exerciseLevel) {
            "Sedentary (Little to no exercise)" -> 1.2
            "Lightly Active (1-3 days/week)" -> 1.375
            "Moderately Active (3-5 days/week)" -> 1.55
            "Very Active (6-7 days/week)" -> 1.725
            "Super Active (Athlete level)" -> 1.9
            else -> 1.2
        }

        val dailyCalories = (bmr * activityFactor).toInt()

        // Updated macro calculation using target percentages:
        val computedProtein = (dailyCalories * 0.32 / 4).toInt()
        val computedCarbs = (dailyCalories * 0.42 / 4).toInt()
        val computedFats = (dailyCalories * 0.26 / 9).toInt()

        // Recalculate total calories from macros to ensure consistency
        val totalCalories = computedProtein * 4 + computedCarbs * 4 + computedFats * 9

        val updatedMacros = hashMapOf(
            "calories" to totalCalories,
            "protein" to computedProtein,
            "carbs" to computedCarbs,
            "fats" to computedFats
        )

        db.collection("users").document(userId)
            .collection("userMacros").document("macros")
            .set(updatedMacros)
            .addOnSuccessListener {
                Log.d("NutritionSettings", "Macros updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("NutritionSettings", "Error updating macros", e)
            }
    }
}
