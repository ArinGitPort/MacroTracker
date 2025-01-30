package com.example.macrotracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class landingpage : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_landingpage)

		// Initialize RecyclerView
		val recyclerView = findViewById<RecyclerView>(R.id.mainFoodRecyclerView)

		// Set LayoutManager
		recyclerView.layoutManager = LinearLayoutManager(this)

		// Set Adapter
		val foodItems = listOf("Apple", "Banana", "Carrot", "Dates", "Eggs", "Fish", "Grapes")
		val adapter = FoodAdapter(foodItems)
		recyclerView.adapter = adapter
	}



}
