package com.example.macrotracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.macrotracker.databinding.ActivityNutritionSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class NutritionSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNutritionSettingsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Default username value – if the username field is still this, we assume it’s not been updated.
    private val defaultUsername = "User"
    // SharedPreferences flag key to ensure we only prompt once.
    private val PREFS_KEY = "app_prefs"
    private val USERNAME_PROMPTED_KEY = "usernamePrompted"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNutritionSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupUnitSuffixes()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            fetchUserNutritionData(userId)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }

        binding.saveNutritionSettingsButton.setOnClickListener {
            if (userId != null) {
                saveUserNutritionData(userId)
            }
        }

        binding.backButton.setOnClickListener {
            startActivity(Intent(this, userprofile::class.java))
            finish()
        }
    }

    private fun setupUnitSuffixes() {
        binding.heightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.heightInput.text.toString()
                if (text.isNotEmpty() && !text.contains(" cm")) {
                    binding.heightInput.setText("$text cm")
                }
            } else {
                binding.heightInput.setText(binding.heightInput.text.toString().replace(" cm", ""))
            }
        }
        binding.weightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.weightInput.text.toString()
                if (text.isNotEmpty() && !text.contains(" kg")) {
                    binding.weightInput.setText("$text kg")
                }
            } else {
                binding.weightInput.setText(binding.weightInput.text.toString().replace(" kg", ""))
            }
        }
        binding.goalWeightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.goalWeightInput.text.toString()
                if (text.isNotEmpty() && !text.contains(" kg")) {
                    binding.goalWeightInput.setText("$text kg")
                }
            } else {
                binding.goalWeightInput.setText(binding.goalWeightInput.text.toString().replace(" kg", ""))
            }
        }
        binding.ageInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.ageInput.text.toString()
                if (text.isNotEmpty() && !text.contains(" years old")) {
                    binding.ageInput.setText("$text years old")
                }
            } else {
                binding.ageInput.setText(binding.ageInput.text.toString().replace(" years old", ""))
            }
        }
    }

    private fun fetchUserNutritionData(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.heightInput.setText(document.getString("height"))
                    binding.weightInput.setText(document.getString("weight"))
                    binding.goalWeightInput.setText(document.getString("goalWeight"))
                    binding.ageInput.setText(document.getString("age"))
                    val gender = document.getString("gender")
                    val genderOptions = resources.getStringArray(R.array.gender_options)
                    val genderIndex = genderOptions.indexOf(gender)
                    if (genderIndex >= 0) {
                        binding.genderSpinner.setSelection(genderIndex)
                    }
                    val exercise = document.getString("exerciseFrequency")
                    val exerciseOptions = resources.getStringArray(R.array.activity_levels)
                    val exerciseIndex = exerciseOptions.indexOf(exercise)
                    if (exerciseIndex >= 0) {
                        binding.exerciseFrequencySpinner.setSelection(exerciseIndex)
                    }
                } else {
                    Log.d("NutritionSettings", "No user data found. Likely first login.")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
                Log.e("NutritionSettings", "Error fetching data", e)
            }
    }

    private fun saveUserNutritionData(userId: String) {
        // Remove unit suffixes before saving
        val heightValue = binding.heightInput.text.toString().replace(" cm", "")
        val weightValue = binding.weightInput.text.toString().replace(" kg", "")
        val goalWeightValue = binding.goalWeightInput.text.toString().replace(" kg", "")
        val ageValue = binding.ageInput.text.toString().replace(" years old", "")

        val nutritionData = hashMapOf(
            "height" to heightValue,
            "weight" to weightValue,
            "goalWeight" to goalWeightValue,
            "age" to ageValue,
            "gender" to binding.genderSpinner.selectedItem.toString(),
            "exerciseFrequency" to binding.exerciseFrequencySpinner.selectedItem.toString()
        )

        // Save (merge) the nutrition settings.
        db.collection("users").document(userId)
            .set(nutritionData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Nutrition settings saved!", Toast.LENGTH_SHORT).show()
                computeUserMacros(userId)
                // After saving, check if the username is still default or not updated.
                checkAndPromptEditUsername(userId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save settings", Toast.LENGTH_SHORT).show()
                Log.e("NutritionSettings", "Error saving data", e)
            }
    }

    private fun computeUserMacros(userId: String) {
        val height = binding.heightInput.text.toString().replace(" cm", "").toDoubleOrNull() ?: 0.0
        val weight = binding.weightInput.text.toString().replace(" kg", "").toDoubleOrNull() ?: 0.0
        val age = binding.ageInput.text.toString().replace(" years old", "").toIntOrNull() ?: 25

        val gender = binding.genderSpinner.selectedItem.toString()
        val exerciseLevel = binding.exerciseFrequencySpinner.selectedItem.toString()

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
        val computedProtein = (dailyCalories * 0.32 / 4).toInt()
        val computedCarbs = (dailyCalories * 0.42 / 4).toInt()
        val computedFats = (dailyCalories * 0.26 / 9).toInt()
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

    /**
     * Checks the user's current username from Firestore.
     * If it is empty, equals the default value, or equals the user's UID, then prompt for username.
     * This prompt is only shown once using a SharedPreferences flag.
     */
    private fun checkAndPromptEditUsername(userId: String) {
        val prefs = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        val alreadyPrompted = prefs.getBoolean(USERNAME_PROMPTED_KEY, false)
        if (!alreadyPrompted) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username")
                        if (username.isNullOrEmpty() || username == defaultUsername || username == auth.currentUser?.uid) {
                            showEditUsernameDialog()
                            prefs.edit().putBoolean(USERNAME_PROMPTED_KEY, true).apply()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("NutritionSettings", "Error checking username", e)
                }
        }
    }

    /**
     * Displays a dialog to allow the user to edit their username.
     * The dialog layout is defined in edit_username_dialog.xml.
     */
    private fun showEditUsernameDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_username_dialog, null)
        val usernameInput = dialogView.findViewById<EditText>(R.id.editUsernameInput)
        usernameInput.hint = "Enter your username"

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Set Username")
            .setView(dialogView)
            .create()

        val updateButton = dialogView.findViewById<Button>(R.id.updateButton)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        updateButton.setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            if (newUsername.isNotEmpty()) {
                updateUsername(newUsername)
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
     * Updates the username in Firestore and sets the "usernameUpdated" flag to true.
     */
    private fun updateUsername(newUsername: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(mapOf("username" to newUsername, "usernameUpdated" to true))
            .addOnSuccessListener {
                Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show()
            }
    }
}
