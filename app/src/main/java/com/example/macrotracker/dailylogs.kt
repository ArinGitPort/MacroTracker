package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityDailylogsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Locale

class dailylogs : AppCompatActivity() {
    private lateinit var binding: ActivityDailylogsBinding
    private lateinit var dailyLogsAdapter: DailyLogsAdapter
    private val loggedFoods = mutableListOf<FoodItem>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailylogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up RecyclerView with adapter using findViewById or binding (if available)
        val foodCollectionRef: CollectionReference = db.collection("users")
            .document(userId!!).collection("daily_logs")
        dailyLogsAdapter = DailyLogsAdapter(
            foodCollectionRef,
            loggedFoods,
            onRemoveClick = { foodItem -> removeFoodItem(foodItem) },
            onEditClick = {
                fetchDailyLogs()
                fetchAndComputeRemainingMacros()
            }
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
        fetchDailyLogs()           // Updates daily macro breakdown
        fetchAndComputeRemainingMacros()   // Updates remaining macros

        // Receive new food data from Landing Page
        handleIncomingFoodData()
    }

    /**
     * Fetches logged food data from the user's daily_logs subcollection and updates the UI.
     */
    private fun fetchDailyLogs() {
        val uid = userId ?: return
        db.collection("users").document(uid).collection("daily_logs")
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

                dailyLogsAdapter.notifyDataSetChanged()
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
     * Fetches the user's macro goals from users/{uid}/userMacros/macros and computes remaining macros.
     */
    private fun fetchAndComputeRemainingMacros() {
        val uid = userId ?: return
        db.collection("users").document(uid)
            .collection("userMacros").document("macros")
            .get()
            .addOnSuccessListener { macroDoc ->
                if (macroDoc.exists()) {
                    val goalMacros = macroDoc.toObject(Macros::class.java)
                    if (goalMacros != null) {
                        db.collection("users").document(uid)
                            .collection("daily_logs")
                            .get()
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

                                val remainingProtein = goalMacros.protein - sumProtein
                                val remainingCarbs = goalMacros.carbs - sumCarbs
                                val remainingFats = goalMacros.fats - sumFats

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
     * Updates the "Remaining Macros" section UI.
     */
    private fun updateRemainingMacros(protein: Int, carbs: Int, fats: Int) {
        binding.proteinRemaining.text = "Protein: ${protein}g"
        binding.carbsRemaining.text = "Carbs: ${carbs}g"
        binding.fatRemaining.text = "Fat: ${fats}g"
    }

    /**
     * Updates the "Daily Macro Breakdown" section UI.
     */
    private fun updateDailyMacroBreakdown(calories: Int, protein: Int, carbs: Int, fats: Int) {
        binding.totalCaloriesCount.text = "$calories kcal"
        binding.proteinValue.text = "${protein}g"
        binding.carbsValue.text = "${carbs}g"
        binding.fatValue.text = "${fats}g"
    }

    /**
     * Checks for incoming food data via intent and adds it to the user's daily_logs subcollection.
     */
    private fun handleIncomingFoodData() {
        val foodName = intent.getStringExtra("foodName") ?: return
        val calories = intent.getIntExtra("calories", 0)
        val protein = intent.getIntExtra("protein", 0)
        val carbs = intent.getIntExtra("carbs", 0)
        val fats = intent.getIntExtra("fats", 0)

        val foodItem = FoodItem(foodName, calories, protein, carbs, fats)
        val uid = userId ?: return

        db.collection("users").document(uid).collection("daily_logs").add(foodItem)
            .addOnSuccessListener {
                Toast.makeText(this, "$foodName added to logs", Toast.LENGTH_SHORT).show()
                fetchDailyLogs()
                fetchAndComputeRemainingMacros()
            }
            .addOnFailureListener { e ->
                Log.e("DailyLogs", "Failed to add log", e)
                Toast.makeText(this, "Failed to add log", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Removes a food item from the user's daily_logs subcollection and updates the UI.
     */
    private fun removeFoodItem(foodItem: FoodItem) {
        val uid = userId ?: return
        db.collection("users").document(uid).collection("daily_logs")
            .whereEqualTo("name", foodItem.name)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("users").document(uid).collection("daily_logs").document(document.id).delete()
                        .addOnSuccessListener {
                            loggedFoods.remove(foodItem)
                            dailyLogsAdapter.notifyDataSetChanged()
                            fetchDailyLogs()
                            fetchAndComputeRemainingMacros()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to remove item", Toast.LENGTH_SHORT).show()
                            Log.e("DailyLogs", "Error removing food", e)
                        }
                }
            }
    }
}
