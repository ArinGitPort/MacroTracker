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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.nio.charset.Charset
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class landingpage : AppCompatActivity() {
	private lateinit var binding: ActivityLandingpageBinding
	private val db = FirebaseFirestore.getInstance()
	private lateinit var auth: FirebaseAuth
	private var userId: String? = null

	// Instead of hardcoded list, these lists will be populated from food.json
	private var originalFoodItems = mutableListOf<FoodItem>()
	private var filteredFoodItems = mutableListOf<FoodItem>()
	private lateinit var foodAdapter: FoodAdapter

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

		// Load food data from JSON file in assets (ensure file is named "food.json")
		loadFoodDataFromJson()

		// Set up RecyclerView for food selection
		binding.mainFoodRecyclerView.layoutManager = LinearLayoutManager(this)
		foodAdapter = FoodAdapter(filteredFoodItems) { selectedFood, multiplier, unit ->
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
			override fun afterTextChanged(s: Editable?) {}
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				val query = s.toString().trim().lowercase(Locale.getDefault())
				val filtered = originalFoodItems.filter {
					it.name.lowercase(Locale.getDefault()).contains(query)
				}
				updateFilteredList(filtered)
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

	/**
	 * Loads food data from the "food.json" file in the assets folder.
	 */
	private fun loadFoodDataFromJson() {
		try {
			val inputStream = assets.open("food.json")
			val size = inputStream.available()
			val buffer = ByteArray(size)
			inputStream.read(buffer)
			inputStream.close()
			val json = String(buffer, Charset.defaultCharset())

			// Parse JSON into a List of FoodItem objects
			val listType = object : TypeToken<List<FoodItem>>() {}.type
			originalFoodItems = Gson().fromJson(json, listType)
			filteredFoodItems.clear()
			filteredFoodItems.addAll(originalFoodItems)
			if (::foodAdapter.isInitialized) {
				foodAdapter.notifyDataSetChanged()
			}
		} catch (e: IOException) {
			Toast.makeText(this, "Error loading food data", Toast.LENGTH_SHORT).show()
			Log.e("LandingPage", "Error loading food.json", e)
		}
	}

	/**
	 * Updates the displayed list of foods.
	 */
	private fun updateFilteredList(filteredList: List<FoodItem>) {
		filteredFoodItems.clear()
		filteredFoodItems.addAll(filteredList)
		foodAdapter.notifyDataSetChanged()
	}

	/**
	 * Adds a food item to Firestore under the user's daily_logs collection.
	 */
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
					val newFood = food.copy(timestamp = com.google.firebase.Timestamp.now())
					foodRef.add(newFood)
						.addOnSuccessListener {
							Toast.makeText(this, "${food.name} added to logs", Toast.LENGTH_SHORT).show()
						}
						.addOnFailureListener {
							Toast.makeText(this, "Failed to add food", Toast.LENGTH_SHORT).show()
						}
				}
			}
			.addOnFailureListener {
				Toast.makeText(this, "Error checking food logs", Toast.LENGTH_SHORT).show()
			}
	}

	/**
	 * Fetches daily logs and computes remaining macros against user's goals.
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

	/**
	 * Checks the time and shows a daily reset dialog if needed.
	 */
	private fun checkAndPromptDailyReset() {
		val calendar = Calendar.getInstance()
		val hour = calendar.get(Calendar.HOUR_OF_DAY)
		if (hour == 0) {
			showDailyResetDialog()
		}
	}

	/**
	 * Displays a dialog prompting the user to reset daily logs.
	 */
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

	/**
	 * Archives current daily logs to "daily_logs_history" and deletes them from "daily_logs".
	 * Uses Asia/Manila time.
	 */
	private fun resetAllData() {
		val uid = userId ?: return
		val userRef = db.collection("users").document(uid)
		val dailyLogsRef = userRef.collection("daily_logs")
		val historyRef = userRef.collection("daily_logs_history")

		// Get current Manila time
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
}
