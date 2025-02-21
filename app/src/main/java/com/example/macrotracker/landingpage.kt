package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityLandingpageBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.util.TimeZone


class landingpage : AppCompatActivity() {
	private lateinit var binding: ActivityLandingpageBinding
	private val db = FirebaseFirestore.getInstance()
	private lateinit var auth: FirebaseAuth
	private var userId: String? = null

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
		binding = ActivityLandingpageBinding.inflate(layoutInflater)
		setContentView(binding.root)

		auth = FirebaseAuth.getInstance()
		userId = auth.currentUser?.uid
		if (userId == null) {
			// If no user is logged in, navigate to login page
			startActivity(Intent(this, loginpage::class.java))
			finish()
			return
		}

		// Check if user profile (nutrition settings) exists. If not, redirect.
		db.collection("users").document(userId!!).get()
			.addOnSuccessListener { document ->
				// Assume if "height" or "weight" is missing, the user hasn't set up their nutrition settings.
				if (!document.exists() ||
					document.getString("height").isNullOrEmpty() ||
					document.getString("weight").isNullOrEmpty()
				) {
					startActivity(Intent(this, NutritionSettingsActivity::class.java))
					finish()
					return@addOnSuccessListener
				}
				// If settings exist, continue with normal flow.
				fetchAndComputeRemainingMacros()
			}
			.addOnFailureListener { e ->
				Toast.makeText(this, "Error checking user profile", Toast.LENGTH_SHORT).show()
				Log.e("LandingPage", "Error checking user profile", e)
			}

		// Check for forced reset or prompt at midnight
		if (intent.getBooleanExtra("forceReset", false)) {
			showDailyResetDialog()
		} else {
			checkAndPromptDailyReset()
		}

		// Set up RecyclerView for food selection
		binding.mainFoodRecyclerView.layoutManager = LinearLayoutManager(this)
		val foodAdapter = FoodAdapter(filteredFoodItems) { selectedFood, multiplier, unit ->
			val modifiedFood = FoodItem(
				name = selectedFood.name,
				calories = (selectedFood.calories * multiplier).toInt(),
				protein = (selectedFood.protein * multiplier).toInt(),
				carbs = (selectedFood.carbs * multiplier).toInt(),
				fats = (selectedFood.fats * multiplier).toInt(),
				servingSize = multiplier,
				unit = unit
			)
			addFoodToFirestore(modifiedFood)
		}
		binding.mainFoodRecyclerView.adapter = foodAdapter

		// Filter food list using search bar with proper locale
		binding.searchBarInput.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) { }
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				val query = s.toString().trim().toLowerCase(Locale.getDefault())
				val filtered = originalFoodItems.filter {
					it.name.toLowerCase(Locale.getDefault()).contains(query)
				}
				foodAdapter.updateList(filtered)
			}
		})

		// Navigation Icons
		binding.macrosIconImage.setOnClickListener {
			startActivity(Intent(this, editmacros::class.java))
		}
		binding.barcodeScannerIconImage.setOnClickListener {
			startActivity(Intent(this, userprofile::class.java))
		}
		binding.dailyLogsIconImage.setOnClickListener {
			startActivity(Intent(this, dailylogs::class.java))
		}

		// Reset button
		binding.resetButton.setOnClickListener {
			showDailyResetDialog()
		}
	}

	private fun checkAndPromptDailyReset() {
		val calendar = Calendar.getInstance()
		val hour = calendar.get(Calendar.HOUR_OF_DAY)
		if (hour == 0) {
			showDailyResetDialog()
		}
	}

	private fun showDailyResetDialog() {
		val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_day, null)
		val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
			.setView(dialogView)
			.create()
		alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

		val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
		val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

		confirmButton.setOnClickListener {
			resetAllData()
			alertDialog.dismiss()
		}
		cancelButton.setOnClickListener {
			alertDialog.dismiss()
		}
		alertDialog.show()
	}

	// Reset daily logs: Archive current logs to "daily_logs_history" then delete them from "daily_logs"
	private fun resetAllData() {
		val uid = userId ?: return
		val userRef = db.collection("users").document(uid)
		val dailyLogsRef = userRef.collection("daily_logs")
		val historyRef = userRef.collection("daily_logs_history")

		// Get current time and add the Asia/Manila offset (UTC+8, no DST)
		val offsetMillis = TimeZone.getTimeZone("Asia/Manila").rawOffset.toLong()
		val manilaTime = System.currentTimeMillis() + offsetMillis
		val resetTimestamp = Timestamp(Date(manilaTime))

		dailyLogsRef.get()
			.addOnSuccessListener { documents ->
				val batch = db.batch()
				for (doc in documents) {
					val foodData = doc.data.toMutableMap()
					foodData["resetDate"] = resetTimestamp
					val historyDocRef = historyRef.document()
					batch.set(historyDocRef, foodData)
					batch.delete(doc.reference)
				}
				batch.commit().addOnSuccessListener {
					Toast.makeText(this, "Daily logs reset & archived", Toast.LENGTH_SHORT).show()
					startActivity(Intent(this, daily_logs_history::class.java))
					finish()
				}.addOnFailureListener {
					Toast.makeText(this, "Failed to reset logs", Toast.LENGTH_SHORT).show()
				}
			}
			.addOnFailureListener {
				Toast.makeText(this, "Error resetting daily logs", Toast.LENGTH_SHORT).show()
			}
	}


	// Add food to daily logs in the user's subcollection
	private fun addFoodToFirestore(food: FoodItem) {
		val uid = userId ?: run {
			Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
			return
		}
		val foodRef = db.collection("users").document(uid).collection("daily_logs")
		foodRef.whereEqualTo("name", food.name)
			.get()
			.addOnSuccessListener { documents ->
				if (documents.isEmpty) {
					val newFood = food.copy(timestamp = Timestamp.now())
					foodRef.add(newFood)
						.addOnSuccessListener {
							Toast.makeText(this, "${food.name} added to logs", Toast.LENGTH_SHORT).show()
							fetchAndComputeRemainingMacros()
						}
						.addOnFailureListener {
							Toast.makeText(this, "Failed to add food", Toast.LENGTH_SHORT).show()
						}
				} else {
					val firstDocId = documents.documents[0].id
					val docRef = foodRef.document(firstDocId)
					docRef.get().addOnSuccessListener { doc ->
						val existingFood = doc.toObject(FoodItem::class.java)
						if (existingFood != null) {
							val updatedFood = mapOf(
								"calories" to (existingFood.calories + food.calories),
								"protein" to (existingFood.protein + food.protein),
								"carbs" to (existingFood.carbs + food.carbs),
								"fats" to (existingFood.fats + food.fats),
								"servingSize" to (existingFood.servingSize + food.servingSize),
								"unit" to food.unit,
								"timestamp" to Timestamp.now()
							)
							docRef.update(updatedFood)
								.addOnSuccessListener {
									Toast.makeText(this, "${food.name} updated in logs", Toast.LENGTH_SHORT).show()
									fetchAndComputeRemainingMacros()
								}
								.addOnFailureListener {
									Toast.makeText(this, "Failed to update food", Toast.LENGTH_SHORT).show()
								}
						}
					}
				}
			}
			.addOnFailureListener {
				Toast.makeText(this, "Error checking food logs", Toast.LENGTH_SHORT).show()
			}
	}

	// Fetch daily logs and compute remaining macros against user's goals
	private fun fetchAndComputeRemainingMacros() {
		val uid = userId ?: return
		// Fetch macro goals from users/{uid}/userMacros/macros
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
