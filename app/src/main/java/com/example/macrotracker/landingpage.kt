package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityLandingpageBinding

class landingpage : AppCompatActivity() {
	private lateinit var binding: ActivityLandingpageBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// ViewBinding
		binding = ActivityLandingpageBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Sample food list
		val foodItems = listOf(
			FoodItem("Apple", 95, 0, 25, 0),
			FoodItem("Banana", 105, 1, 27, 0),
			FoodItem("Carrot", 41, 1, 10, 0),
			FoodItem("Eggs", 68, 6, 1, 5),
			FoodItem("Fish", 140, 20, 0, 6),
			FoodItem("Grapes", 62, 0, 16, 0)
		)

		// Set up RecyclerView
		binding.mainFoodRecyclerView.layoutManager = LinearLayoutManager(this)
		binding.mainFoodRecyclerView.adapter = FoodAdapter(foodItems) { selectedFood ->
			// Send selected food item to Daily Logs
			val intent = Intent(this, dailylogs::class.java).apply {
				putExtra("foodName", selectedFood.name)
				putExtra("calories", selectedFood.calories)
				putExtra("protein", selectedFood.protein)
				putExtra("carbs", selectedFood.carbs)
				putExtra("fats", selectedFood.fats)
			}
			startActivity(intent)
		}

		// Set Click Listeners (Preserving all previous ones)
		binding.macrosIconImage.setOnClickListener {
			val intent = Intent(this, editmacros::class.java)
			startActivity(intent)
		}



		binding.dailyLogsIconImage.setOnClickListener {
			val intent = Intent(this, dailylogs::class.java)
			startActivity(intent)
		}
	}
}
