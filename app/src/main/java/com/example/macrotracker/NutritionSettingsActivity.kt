package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NutritionSettingsActivity : AppCompatActivity() {

    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var goalWeightInput: EditText
    private lateinit var ageInput: EditText
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
        ageInput = findViewById(R.id.ageInput)
        genderSpinner = findViewById(R.id.genderSpinner)
        exerciseFrequencySpinner = findViewById(R.id.exerciseFrequencySpinner)
        saveButton = findViewById(R.id.saveNutritionSettingsButton)
        backButton = findViewById(R.id.backButton)

        // Set up onFocus listeners for unit suffixes.
        setupUnitSuffixes()

        // Get the current user's ID
        val userId = auth.currentUser?.uid
        if (userId != null) {
            fetchUserNutritionData(userId)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        // Save settings when Save is clicked.
        // This will compute and update the macros.
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
     * Set focus change listeners on input fields so that when they lose focus,
     * a unit suffix is appended, and when they gain focus, the suffix is removed.
     */
    private fun setupUnitSuffixes() {
        // For heightInput, append " cm"
        heightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = heightInput.text.toString()
                if (text.isNotEmpty() && !text.contains(" cm")) {
                    heightInput.setText("$text cm")
                }
            } else {
                val text = heightInput.text.toString().replace(" cm", "")
                heightInput.setText(text)
            }
        }
        // For weightInput, append " kg"
        weightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = weightInput.text.toString()
                if (text.isNotEmpty() && !text.contains(" kg")) {
                    weightInput.setText("$text kg")
                }
            } else {
                val text = weightInput.text.toString().replace(" kg", "")
                weightInput.setText(text)
            }
        }
        // For goalWeightInput, append " kg"
        goalWeightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = goalWeightInput.text.toString()
                if (text.isNotEmpty() && !text.contains(" kg")) {
                    goalWeightInput.setText("$text kg")
                }
            } else {
                val text = goalWeightInput.text.toString().replace(" kg", "")
                goalWeightInput.setText(text)
            }
        }
        // For ageInput, append " years old"
        ageInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = ageInput.text.toString()
                if (text.isNotEmpty() && !text.contains(" years old")) {
                    ageInput.setText("$text years old")
                }
            } else {
                val text = ageInput.text.toString().replace(" years old", "")
                ageInput.setText(text)
            }
        }
    }

    /**
     * Fetches existing nutrition settings for the given user and populates the fields.
     * This method loads the saved data (including age) so the user can see their current settings.
     */
    private fun fetchUserNutritionData(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    heightInput.setText(document.getString("height"))
                    weightInput.setText(document.getString("weight"))
                    goalWeightInput.setText(document.getString("goalWeight"))
                    ageInput.setText(document.getString("age"))
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
                    // Do not auto compute macros here if user manually set macros elsewhere.
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
     * Before saving, it removes the unit suffixes.
     */
    private fun saveUserNutritionData(userId: String) {
        // Remove unit suffixes before saving
        val heightValue = heightInput.text.toString().replace(" cm", "")
        val weightValue = weightInput.text.toString().replace(" kg", "")
        val goalWeightValue = goalWeightInput.text.toString().replace(" kg", "")
        val ageValue = ageInput.text.toString().replace(" years old", "")

        val nutritionData = hashMapOf(
            "height" to heightValue,
            "weight" to weightValue,
            "goalWeight" to goalWeightValue,
            "age" to ageValue,
            "gender" to genderSpinner.selectedItem.toString(),
            "exerciseFrequency" to exerciseFrequencySpinner.selectedItem.toString()
        )

        db.collection("users").document(userId)
            .set(nutritionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Nutrition settings saved!", Toast.LENGTH_SHORT).show()
                // Compute macros only when the user clicks Save.
                computeUserMacros(userId)
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
     * This uses the user's age input.
     */
    private fun computeUserMacros(userId: String) {
        val height = heightInput.text.toString().replace(" cm", "").toDoubleOrNull() ?: 0.0
        val weight = weightInput.text.toString().replace(" kg", "").toDoubleOrNull() ?: 0.0
        val age = ageInput.text.toString().replace(" years old", "").toIntOrNull() ?: 25

        val gender = genderSpinner.selectedItem.toString()
        val exerciseLevel = exerciseFrequencySpinner.selectedItem.toString()

        // Calculate BMR using the Mifflin-St Jeor Equation.
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

        // Macro calculation using target percentages:
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
