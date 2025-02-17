package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.inputmethod.EditorInfo
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

        db = FirebaseFirestore.getInstance()

        // Link EditTexts for user inputs
        val caloriesInput = findViewById<EditText>(R.id.caloriesInput)
        val proteinInput = findViewById<EditText>(R.id.proteinInput)
        val carbsInput = findViewById<EditText>(R.id.carbsInput)
        val fatsInput = findViewById<EditText>(R.id.fatsInput)

        // Auto-calculate default macros when calories change (if no macro field is currently focused)
        caloriesInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: Editable?) { }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!proteinInput.isFocused() && !carbsInput.isFocused() && !fatsInput.isFocused()) {
                    val calStr = s.toString()
                    if (calStr.isNotEmpty()) {
                        val totalCals = calStr.toIntOrNull() ?: 0
                        // Default ratios: Protein 30%, Fat 25%, Carbs 45%
                        val proteinCals = (totalCals * 0.30).toInt()
                        val fatCals = (totalCals * 0.25).toInt()
                        val carbsCals = (totalCals * 0.45).toInt()
                        proteinInput.setText((proteinCals / 4).toString())
                        carbsInput.setText((carbsCals / 4).toString())
                        fatsInput.setText((fatCals / 9).toString())
                    } else {
                        proteinInput.setText("")
                        carbsInput.setText("")
                        fatsInput.setText("")
                    }
                }
            }
        })

        // Recalculate after user confirms protein entry
        proteinInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                recalcAfterProtein(caloriesInput, proteinInput, carbsInput, fatsInput)
                true
            } else false
        }

        // Recalculate after user confirms carbs entry
        carbsInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                recalcAfterCarbs(caloriesInput, proteinInput, carbsInput, fatsInput)
                true
            } else false
        }

        // Recalculate after user confirms fats entry
        fatsInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                recalcAfterFats(caloriesInput, proteinInput, carbsInput, fatsInput)
                true
            } else false
        }

        // Save button
        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val calStr = caloriesInput.text.toString()
            val pStr = proteinInput.text.toString()
            val cStr = carbsInput.text.toString()
            val fStr = fatsInput.text.toString()

            if (calStr.isBlank() || pStr.isBlank() || cStr.isBlank() || fStr.isBlank()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cal = calStr.toInt()
            val p = pStr.toInt()
            val c = cStr.toInt()
            val f = fStr.toInt()

            val macros = Macros(cal, p, c, f)
            db.collection("userMacros").document("macros")
                .set(macros)
                .addOnSuccessListener {
                    Toast.makeText(this, "Macros updated", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, landingpage::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update macros", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        }

        // Back button
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            startActivity(Intent(this, landingpage::class.java))
            finish()
        }
    }

    private fun recalcAfterProtein(
        caloriesInput: EditText,
        proteinInput: EditText,
        carbsInput: EditText,
        fatsInput: EditText
    ) {
        val totalCals = caloriesInput.text.toString().toIntOrNull() ?: return
        val proteinGrams = proteinInput.text.toString().toIntOrNull() ?: return
        val proteinCals = proteinGrams * 4
        if (proteinCals > totalCals) {
            Toast.makeText(this, "Protein exceeds total calories", Toast.LENGTH_SHORT).show()
            return
        }
        val remCals = totalCals - proteinCals
        // Distribute remaining calories between carbs (45%) and fat (25%)
        val ratioSum = 45 + 25
        val newCarbsCals = (remCals * (45.0 / ratioSum)).toInt()
        val newFatCals = (remCals * (25.0 / ratioSum)).toInt()
        carbsInput.setText((newCarbsCals / 4).toString())
        fatsInput.setText((newFatCals / 9).toString())
    }

    private fun recalcAfterCarbs(
        caloriesInput: EditText,
        proteinInput: EditText,
        carbsInput: EditText,
        fatsInput: EditText
    ) {
        val totalCals = caloriesInput.text.toString().toIntOrNull() ?: return
        val carbsGrams = carbsInput.text.toString().toIntOrNull() ?: return
        val carbsCals = carbsGrams * 4
        if (carbsCals > totalCals) {
            Toast.makeText(this, "Carbs exceed total calories", Toast.LENGTH_SHORT).show()
            return
        }
        val remCals = totalCals - carbsCals
        // Distribute remaining calories between protein (30%) and fat (25%)
        val ratioSum = 30 + 25
        val newProteinCals = (remCals * (30.0 / ratioSum)).toInt()
        val newFatCals = (remCals * (25.0 / ratioSum)).toInt()
        proteinInput.setText((newProteinCals / 4).toString())
        fatsInput.setText((newFatCals / 9).toString())
    }

    private fun recalcAfterFats(
        caloriesInput: EditText,
        proteinInput: EditText,
        carbsInput: EditText,
        fatsInput: EditText
    ) {
        val totalCals = caloriesInput.text.toString().toIntOrNull() ?: return
        val fatsGrams = fatsInput.text.toString().toIntOrNull() ?: return
        val fatsCals = fatsGrams * 9
        if (fatsCals > totalCals) {
            Toast.makeText(this, "Fat exceeds total calories", Toast.LENGTH_SHORT).show()
            return
        }
        val remCals = totalCals - fatsCals
        // Distribute remaining calories between protein (30%) and carbs (45%)
        val ratioSum = 30 + 45
        val newProteinCals = (remCals * (30.0 / ratioSum)).toInt()
        val newCarbsCals = (remCals * (45.0 / ratioSum)).toInt()
        proteinInput.setText((newProteinCals / 4).toString())
        carbsInput.setText((newCarbsCals / 4).toString())
    }
}
