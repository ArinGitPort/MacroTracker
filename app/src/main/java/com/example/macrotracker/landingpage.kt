package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityLandingpageBinding
import com.google.firebase.firestore.FirebaseFirestore

class landingpage : AppCompatActivity() {
	private lateinit var binding: ActivityLandingpageBinding
	private val db = FirebaseFirestore.getInstance() // Firestore instance

	// Keep a full list and a mutable filtered list
	private val originalFoodItems = listOf(
		FoodItem("Apple", 95, 0, 25, 0),
		FoodItem("Banana", 105, 1, 27, 0),
		FoodItem("Carrot", 41, 1, 10, 0),
		FoodItem("Eggs", 68, 6, 1, 5),
		FoodItem("Fish", 140, 20, 0, 6),
		FoodItem("Grapes", 62, 0, 16, 0)
	)
	private val filteredFoodItems = originalFoodItems.toMutableList()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// ViewBinding Setup
		binding = ActivityLandingpageBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Update remaining macros based on goal macros and daily logs
		fetchAndComputeRemainingMacros()

		// Set up RecyclerView for food selection with the filtered list
		binding.mainFoodRecyclerView.layoutManager = LinearLayoutManager(this)
		val adapter = FoodAdapter(filteredFoodItems) { selectedFood, quantity ->
			// Multiply nutritional values by quantity
			val modifiedFood = FoodItem(
				selectedFood.name,
				selectedFood.calories * quantity,
				selectedFood.protein * quantity,
				selectedFood.carbs * quantity,
				selectedFood.fats * quantity
			)
			addFoodToFirestore(modifiedFood)
		}
		binding.mainFoodRecyclerView.adapter = adapter

		// Set up search bar to filter food items
		binding.searchBarInput.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) {
				// No action needed here
			}
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
				// No action needed here
			}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				val query = s.toString().trim().toLowerCase()
				filteredFoodItems.clear()
				if (query.isEmpty()) {
					filteredFoodItems.addAll(originalFoodItems)
				} else {
					val filtered = originalFoodItems.filter {
						it.name.toLowerCase().contains(query)
					}
					filteredFoodItems.addAll(filtered)
				}
				adapter.notifyDataSetChanged()
			}
		})

		// Navigation Icons Click Listeners
		binding.macrosIconImage.setOnClickListener {
			startActivity(Intent(this, editmacros::class.java))
		}

		binding.dailyLogsIconImage.setOnClickListener {
			startActivity(Intent(this, dailylogs::class.java))
		}
	}

	/**
	 * Stores selected food in Firestore (if not already logged) and refreshes macro calculations.
	 */
	private fun addFoodToFirestore(food: FoodItem) {
		val foodRef = db.collection("daily_logs")
		// Check if food is already logged to prevent duplicates
		foodRef.whereEqualTo("name", food.name)
			.get()
			.addOnSuccessListener { documents ->
				if (documents.isEmpty) {
					foodRef.add(food)
						.addOnSuccessListener {
							Toast.makeText(this, "${food.name} added to logs", Toast.LENGTH_SHORT).show()
							// Refresh macros after adding food
							fetchAndComputeRemainingMacros()
						}
						.addOnFailureListener {
							Toast.makeText(this, "Failed to add food", Toast.LENGTH_SHORT).show()
						}
				} else {
					Toast.makeText(this, "${food.name} is already logged", Toast.LENGTH_SHORT).show()
				}
			}
			.addOnFailureListener {
				Toast.makeText(this, "Error checking food logs", Toast.LENGTH_SHORT).show()
			}
	}

	/**
	 * Fetches the macro goals and daily logs from Firestore, computes the remaining macros,
	 * and updates the TextViews (calorieCount, proteinCount, carbsCount, fatCount).
	 */
	private fun fetchAndComputeRemainingMacros() {
		// Fetch the macro goals from Firestore
		db.collection("userMacros").document("macros").get()
			.addOnSuccessListener { macroDoc ->
				if (macroDoc.exists()) {
					val goalMacros = macroDoc.toObject(Macros::class.java)
					if (goalMacros != null) {
						// Now fetch daily logs and sum their nutritional values
						db.collection("daily_logs").get()
							.addOnSuccessListener { logs ->
								var sumCalories = 0
								var sumProtein = 0
								var sumCarbs = 0
								var sumFats = 0

								for (doc in logs) {
									try {
										val item = doc.toObject(FoodItem::class.java)
										sumCalories += item.calories
										sumProtein += item.protein
										sumCarbs += item.carbs
										sumFats += item.fats
									} catch (e: Exception) {
										Log.e("LandingPage", "Error converting document: ${doc.id}", e)
									}
								}

								// Compute remaining macros (goal minus consumed)
								val remainingCalories = goalMacros.calories - sumCalories
								val remainingProtein = goalMacros.protein - sumProtein
								val remainingCarbs = goalMacros.carbs - sumCarbs
								val remainingFats = goalMacros.fats - sumFats

								// Update TextViews on the landing page
								binding.calorieCount.text = remainingCalories.toString()
								binding.proteinCount.text = remainingProtein.toString()
								binding.carbsCount.text = remainingCarbs.toString()
								binding.fatCount.text = remainingFats.toString()
							}
							.addOnFailureListener { e ->
								Toast.makeText(this, "Failed to fetch daily logs", Toast.LENGTH_SHORT).show()
								Log.e("LandingPage", "Error fetching daily logs", e)
							}
					}
				} else {
					Toast.makeText(this, "Macro goals not set", Toast.LENGTH_SHORT).show()
				}
			}
			.addOnFailureListener { e ->
				Toast.makeText(this, "Error fetching macro goals", Toast.LENGTH_SHORT).show()
				Log.e("LandingPage", "Error fetching macros", e)
			}
	}
}
