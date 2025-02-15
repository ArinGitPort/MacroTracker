package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityDailylogsBinding
import com.google.firebase.firestore.FirebaseFirestore

class dailylogs : AppCompatActivity() {
    private lateinit var binding: ActivityDailylogsBinding
    private lateinit var dailyLogsAdapter: DailyLogsAdapter
    private val loggedFoods = mutableListOf<FoodItem>()

    // Firestore instance
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailylogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RecyclerView with adapter
        dailyLogsAdapter = DailyLogsAdapter(loggedFoods) { foodItem ->
            removeFoodItem(foodItem)
        }
        binding.dailyFoodRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@dailylogs)
            adapter = dailyLogsAdapter
        }

        // Back Button Navigation
        binding.backButton.setOnClickListener {
            val intent = Intent(this, landingpage::class.java)
            startActivity(intent)
            finish()
        }

        // Fetch Data from Firestore and update macros
        fetchDailyLogs()

        // Receive new food data from Landing Page
        handleIncomingFoodData()
    }

    /**
     * Fetches logged food data from Firestore and updates RecyclerView & Macros UI.
     */
    private fun fetchDailyLogs() {
        db.collection("daily_logs")
            .get()
            .addOnSuccessListener { documents ->
                loggedFoods.clear()
                var totalCalories = 0
                var totalProtein = 0
                var totalCarbs = 0
                var totalFats = 0

                for (document in documents) {
                    try {
                        val foodItem = document.toObject(FoodItem::class.java)
                        loggedFoods.add(foodItem)

                        // Sum up macros
                        totalCalories += foodItem.calories
                        totalProtein += foodItem.protein
                        totalCarbs += foodItem.carbs
                        totalFats += foodItem.fats
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document: ${document.id}", e)
                    }
                }

                // Update RecyclerView
                dailyLogsAdapter.notifyDataSetChanged()

                // Update Macro TextViews in both sections
                updateMacroTextViews(totalCalories, totalProtein, totalCarbs, totalFats)

                if (loggedFoods.isEmpty()) {
                    Toast.makeText(this, "No logs found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching data", exception)
                Toast.makeText(this, "Failed to load logs", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Updates the TextViews in both the "Remaining Macros" and "Daily Macro Breakdown" sections.
     */
    private fun updateMacroTextViews(calories: Int, protein: Int, carbs: Int, fats: Int) {
        // Update Remaining Macros
        binding.totalCaloriesCount.text = "$calories kcal"
        binding.proteinRemaining.text = "Protein: ${protein}g"
        binding.carbsRemaining.text = "Carbs: ${carbs}g"
        binding.fatRemaining.text = "Fat: ${fats}g"

        // Update Daily Macro Breakdown
        binding.proteinValue.text = "${protein}g"
        binding.carbsValue.text = "${carbs}g"
        binding.fatValue.text = "${fats}g"
    }

    /**
     * Checks the intent for incoming food data and adds it to Firestore.
     */
    private fun handleIncomingFoodData() {
        val foodName = intent.getStringExtra("foodName") ?: return
        val calories = intent.getIntExtra("calories", 0)
        val protein = intent.getIntExtra("protein", 0)
        val carbs = intent.getIntExtra("carbs", 0)
        val fats = intent.getIntExtra("fats", 0)

        val foodItem = FoodItem(foodName, calories, protein, carbs, fats)

        // Save to Firestore
        db.collection("daily_logs").add(foodItem)
            .addOnSuccessListener {
                Toast.makeText(this, "$foodName added to logs", Toast.LENGTH_SHORT).show()
                fetchDailyLogs() // Refresh data
            }
            .addOnFailureListener { e ->
                Log.e("DailyLogs", "Failed to add log", e)
                Toast.makeText(this, "Failed to add log", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Removes a food item from Firestore and updates RecyclerView.
     */
    private fun removeFoodItem(foodItem: FoodItem) {
        db.collection("daily_logs")
            .whereEqualTo("name", foodItem.name)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("daily_logs").document(document.id).delete()
                        .addOnSuccessListener {
                            loggedFoods.remove(foodItem)
                            dailyLogsAdapter.notifyDataSetChanged()
                            fetchDailyLogs() // Refresh data after deletion
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to remove item", Toast.LENGTH_SHORT).show()
                            Log.e("DailyLogs", "Error removing food", e)
                        }
                }
            }
    }
}
