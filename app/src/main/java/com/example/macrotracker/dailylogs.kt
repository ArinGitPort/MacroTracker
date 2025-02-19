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
        dailyLogsAdapter = DailyLogsAdapter(
            loggedFoods,
            onRemoveClick = { foodItem -> removeFoodItem(foodItem) },
            onEditClick = { fetchDailyLogs(); fetchAndComputeRemainingMacros() } // Refresh UI after editing
        )

        binding.dailyFoodRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@dailylogs)
            adapter = dailyLogsAdapter
        }


        // Back Button Navigation
        binding.backButton.setOnClickListener {
            startActivity(Intent(this, landingpage::class.java))
            finish()
        }

        // Fetch data for both sections
        fetchDailyLogs() // Updates daily macro breakdown
        fetchAndComputeRemainingMacros() // Updates remaining macros

        // Receive new food data from Landing Page
        handleIncomingFoodData()
    }

    /**
     * Fetches logged food data from Firestore and updates the Daily Macro Breakdown UI.
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

                // Update only the Daily Macro Breakdown section
                updateDailyMacroBreakdown(totalCalories, totalProtein, totalCarbs, totalFats)

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
     * Fetches goal macros and updates the Remaining Macros UI.
     */
    private fun fetchAndComputeRemainingMacros() {
        db.collection("userMacros").document("macros").get()
            .addOnSuccessListener { macroDoc ->
                if (macroDoc.exists()) {
                    val goalMacros = macroDoc.toObject(Macros::class.java)
                    if (goalMacros != null) {
                        db.collection("daily_logs").get()
                            .addOnSuccessListener { logs ->
                                var sumProtein = 0
                                var sumCarbs = 0
                                var sumFats = 0

                                for (doc in logs) {
                                    try {
                                        val item = doc.toObject(FoodItem::class.java)
                                        sumProtein += item.protein
                                        sumCarbs += item.carbs
                                        sumFats += item.fats
                                    } catch (e: Exception) {
                                        Log.e("DailyLogs", "Error converting document: ${doc.id}", e)
                                    }
                                }

                                // Compute remaining macros
                                val remainingProtein = goalMacros.protein - sumProtein
                                val remainingCarbs = goalMacros.carbs - sumCarbs
                                val remainingFats = goalMacros.fats - sumFats

                                // Update only the Remaining Macros section
                                updateRemainingMacros(remainingProtein, remainingCarbs, remainingFats)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to fetch remaining macros", Toast.LENGTH_SHORT).show()
                                Log.e("DailyLogs", "Error fetching remaining macros", e)
                            }
                    }
                } else {
                    Toast.makeText(this, "Macro goals not set", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching macro goals", Toast.LENGTH_SHORT).show()
                Log.e("DailyLogs", "Error fetching macros", e)
            }
    }

    /**
     * Updates only the "Remaining Macros" section.
     */
    private fun updateRemainingMacros(protein: Int, carbs: Int, fats: Int) {
        binding.proteinRemaining.text = "Protein: ${protein}g"
        binding.carbsRemaining.text = "Carbs: ${carbs}g"
        binding.fatRemaining.text = "Fat: ${fats}g"
    }

    /**
     * Updates only the "Daily Macro Breakdown" section.
     */
    private fun updateDailyMacroBreakdown(calories: Int, protein: Int, carbs: Int, fats: Int) {
        binding.totalCaloriesCount.text = "$calories kcal"
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
                fetchDailyLogs() // Update Daily Macro Breakdown
                fetchAndComputeRemainingMacros() // Update Remaining Macros
            }
            .addOnFailureListener { e ->
                Log.e("DailyLogs", "Failed to add log", e)
                Toast.makeText(this, "Failed to add log", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Removes a food item from Firestore and updates UI.
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
                            fetchDailyLogs() // Update Daily Macro Breakdown
                            fetchAndComputeRemainingMacros() // Update Remaining Macros
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to remove item", Toast.LENGTH_SHORT).show()
                            Log.e("DailyLogs", "Error removing food", e)
                        }
                }
            }
    }
}
