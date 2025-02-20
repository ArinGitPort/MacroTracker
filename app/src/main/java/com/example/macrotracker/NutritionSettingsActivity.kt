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
    private lateinit var todaysWeightInput: EditText
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

        // Get UI elements
        heightInput = findViewById(R.id.heightInput)
        weightInput = findViewById(R.id.weightInput)
        todaysWeightInput = findViewById(R.id.todaysWeightInput)
        goalWeightInput = findViewById(R.id.goalWeightInput)
        genderSpinner = findViewById(R.id.genderSpinner)
        exerciseFrequencySpinner = findViewById(R.id.exerciseFrequencySpinner)
        saveButton = findViewById(R.id.saveNutritionSettingsButton)
        backButton = findViewById(R.id.backButton)

        val userId = auth.currentUser?.uid

        if (userId != null) {
            fetchUserNutritionData(userId) // Fetch stored user data and display it
        }

        // Save data to Firebase Firestore
        saveButton.setOnClickListener {
            if (userId != null) {
                saveUserNutritionData(userId)
            }
        }

        // Navigate back
        backButton.setOnClickListener {
            startActivity(Intent(this, landingpage::class.java))
            finish()
        }
    }

    private fun fetchUserNutritionData(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Load saved data into input fields
                    heightInput.setText(document.getString("height"))
                    weightInput.setText(document.getString("weight"))
                    todaysWeightInput.setText(document.getString("todaysWeight"))
                    goalWeightInput.setText(document.getString("goalWeight"))

                    val gender = document.getString("gender")
                    val genderIndex = resources.getStringArray(R.array.gender_options).indexOf(gender)
                    if (genderIndex != -1) genderSpinner.setSelection(genderIndex)

                    val exerciseFrequency = document.getString("exerciseFrequency")
                    val exerciseIndex = resources.getStringArray(R.array.activity_levels).indexOf(exerciseFrequency)
                    if (exerciseIndex != -1) exerciseFrequencySpinner.setSelection(exerciseIndex)

                    computeUserMacros(userId) // Compute macros after fetching data
                } else {
                    Log.d("NutritionSettings", "No existing user data found.")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
                Log.e("NutritionSettings", "Error fetching data", e)
            }
    }

    private fun saveUserNutritionData(userId: String) {
        val nutritionData = hashMapOf(
            "height" to heightInput.text.toString(),
            "weight" to weightInput.text.toString(),
            "todaysWeight" to todaysWeightInput.text.toString(),
            "goalWeight" to goalWeightInput.text.toString(),
            "gender" to genderSpinner.selectedItem.toString(),
            "exerciseFrequency" to exerciseFrequencySpinner.selectedItem.toString()
        )

        db.collection("users").document(userId)
            .set(nutritionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Nutrition settings saved!", Toast.LENGTH_SHORT).show()
                computeUserMacros(userId) // Recalculate macros after saving
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                Log.e("NutritionSettings", "Error saving data", e)
            }
    }

    private fun computeUserMacros(userId: String) {
        db.collection("userMacros").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val macros = document.toObject(Macros::class.java)

                    if (macros != null) {
                        val height = heightInput.text.toString().toDoubleOrNull() ?: 0.0
                        val weight = weightInput.text.toString().toDoubleOrNull() ?: 0.0
                        val age = 25 // Example age, consider adding this input

                        val gender = genderSpinner.selectedItem.toString()
                        val exerciseLevel = exerciseFrequencySpinner.selectedItem.toString()

                        // Calculate BMR using Mifflin-St Jeor Equation
                        val bmr = if (gender == "Male") {
                            10 * weight + 6.25 * height - 5 * age + 5
                        } else {
                            10 * weight + 6.25 * height - 5 * age - 161
                        }

                        // Adjust based on exercise frequency
                        val activityFactor = when (exerciseLevel) {
                            "Sedentary (Little to no exercise)" -> 1.2
                            "Lightly Active (1-3 days/week)" -> 1.375
                            "Moderately Active (3-5 days/week)" -> 1.55
                            "Very Active (6-7 days/week)" -> 1.725
                            "Super Active (Athlete level)" -> 1.9
                            else -> 1.2
                        }

                        val dailyCalories = (bmr * activityFactor).toInt()
                        val protein = (weight * 2.2).toInt()  // Example: 2.2g protein per kg
                        val carbs = (dailyCalories * 0.5 / 4).toInt() // 50% carbs
                        val fats = (dailyCalories * 0.3 / 9).toInt() // 30% fats

                        // Save computed macros to Firestore
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
                } else {
                    Log.d("NutritionSettings", "No macro data found for user.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("NutritionSettings", "Error fetching macros", e)
            }
    }
}
