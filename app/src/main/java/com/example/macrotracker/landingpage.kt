package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityLandingpageBinding
import com.google.firebase.firestore.FirebaseFirestore

class landingpage : AppCompatActivity() {
	private lateinit var binding: ActivityLandingpageBinding
	private val db = FirebaseFirestore.getInstance() // Firestore instance

	// Full list and mutable filtered list of food items
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
		val foodAdapter = FoodAdapter(filteredFoodItems) { selectedFood, multiplier ->
			// Multiply nutritional values by multiplier before adding to Firestore
			val modifiedFood = FoodItem(
				selectedFood.name,
				(selectedFood.calories * multiplier).toInt(),
				(selectedFood.protein * multiplier).toInt(),
				(selectedFood.carbs * multiplier).toInt(),
				(selectedFood.fats * multiplier).toInt()
			)
			addFoodToFirestore(modifiedFood)
		}
		binding.mainFoodRecyclerView.adapter = foodAdapter

		// Set up search bar to filter food items
		binding.searchBarInput.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) { }
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
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
				foodAdapter.notifyDataSetChanged()
			}
		})

		// Navigation Icons Click Listeners
		binding.macrosIconImage.setOnClickListener {
			startActivity(Intent(this, editmacros::class.java))
		}
		binding.dailyLogsIconImage.setOnClickListener {
			startActivity(Intent(this, dailylogs::class.java))
		}

		// Reset Button Listener
		binding.resetButton.setOnClickListener {
			AlertDialog.Builder(this)
				.setTitle("Reset Data")
				.setMessage("This will reset all daily logs and macros. Do you want to continue?")
				.setPositiveButton("Yes") { dialog, _ ->
					resetAllData()
					dialog.dismiss()
				}
				.setNegativeButton("No") { dialog, _ ->
					dialog.dismiss()
				}
				.show()
		}
	}

	/**
	 * Resets daily logs and macro goals in Firestore.
	 */
	private fun resetAllData() {
		// 1. Delete all documents in "daily_logs"
		db.collection("daily_logs").get()
			.addOnSuccessListener { documents ->
				val batch = db.batch()
				for (doc in documents) {
					batch.delete(doc.reference)
				}
				batch.commit().addOnSuccessListener {
					Toast.makeText(this, "Daily logs reset", Toast.LENGTH_SHORT).show()
					fetchAndComputeRemainingMacros() // Update UI after reset
				}.addOnFailureListener {
					Toast.makeText(this, "Failed to reset daily logs", Toast.LENGTH_SHORT).show()
				}
			}
			.addOnFailureListener {
				Toast.makeText(this, "Error resetting daily logs", Toast.LENGTH_SHORT).show()
			}

		// 2. Reset macros in "userMacros" (here, setting to 0 for all values, adjust as needed)
		val defaultMacros = Macros(0, 0, 0, 0)
		db.collection("userMacros").document("macros")
			.set(defaultMacros)
			.addOnSuccessListener {
				Toast.makeText(this, "Macros reset", Toast.LENGTH_SHORT).show()
				fetchAndComputeRemainingMacros()
			}
			.addOnFailureListener {
				Toast.makeText(this, "Failed to reset macros", Toast.LENGTH_SHORT).show()
			}
	}

	/**
	 * Stores selected food in Firestore.
	 * If a document with the same food name exists, it combines the nutritional values.
	 */
	private fun addFoodToFirestore(food: FoodItem) {
		val foodRef = db.collection("daily_logs")
		foodRef.whereEqualTo("name", food.name)
			.get()
			.addOnSuccessListener { documents ->
				if (documents.isEmpty) {
					// No existing entry: add new document.
					foodRef.add(food)
						.addOnSuccessListener {
							Toast.makeText(this, "${food.name} added to logs", Toast.LENGTH_SHORT).show()
							fetchAndComputeRemainingMacros()
						}
						.addOnFailureListener {
							Toast.makeText(this, "Failed to add food", Toast.LENGTH_SHORT).show()
						}
				} else {
					// Combine nutritional values from existing entries.
					var combinedCalories = food.calories
					var combinedProtein = food.protein
					var combinedCarbs = food.carbs
					var combinedFats = food.fats
					val docsList = documents.documents
					for (doc in docsList) {
						val existingFood = doc.toObject(FoodItem::class.java)
						if (existingFood != null) {
							combinedCalories += existingFood.calories
							combinedProtein += existingFood.protein
							combinedCarbs += existingFood.carbs
							combinedFats += existingFood.fats
						}
					}
					val combinedFood = FoodItem(
						name = food.name,
						calories = combinedCalories,
						protein = combinedProtein,
						carbs = combinedCarbs,
						fats = combinedFats
					)
					// Update the first document with the combined values.
					val firstDocId = docsList[0].id
					foodRef.document(firstDocId).set(combinedFood)
						.addOnSuccessListener {
							// Delete any extra duplicates.
							if (docsList.size > 1) {
								for (i in 1 until docsList.size) {
									foodRef.document(docsList[i].id).delete()
								}
							}
							Toast.makeText(this, "${food.name} updated in logs", Toast.LENGTH_SHORT).show()
							fetchAndComputeRemainingMacros()
						}
						.addOnFailureListener {
							Toast.makeText(this, "Failed to update food", Toast.LENGTH_SHORT).show()
						}
				}
			}
			.addOnFailureListener {
				Toast.makeText(this, "Error checking food logs", Toast.LENGTH_SHORT).show()
			}
	}

	/**
	 * Fetches macro goals and daily logs from Firestore, computes remaining macros,
	 * and updates the corresponding TextViews.
	 */
	private fun fetchAndComputeRemainingMacros() {
		db.collection("userMacros").document("macros").get()
			.addOnSuccessListener { macroDoc ->
				if (macroDoc.exists()) {
					val goalMacros = macroDoc.toObject(Macros::class.java)
					if (goalMacros != null) {
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

								val remainingCalories = goalMacros.calories - sumCalories
								val remainingProtein = goalMacros.protein - sumProtein
								val remainingCarbs = goalMacros.carbs - sumCarbs
								val remainingFats = goalMacros.fats - sumFats

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
