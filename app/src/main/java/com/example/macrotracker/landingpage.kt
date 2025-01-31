package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
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

		// Find ImageViews
		val macros = findViewById<ImageView>(R.id.macrosIconImage)
		val barcode = findViewById<ImageView>(R.id.barcodeScannerIconImage)
		val dailyLogs = findViewById<ImageView>(R.id.dailyLogsIconImage)

		// Set Click Listeners
		macros.setOnClickListener {
			val intent = Intent(this, editmacros::class.java)
			startActivity(intent)
		}



		dailyLogs.setOnClickListener {
			val intent = Intent(this, dailylogs::class.java)
			startActivity(intent)
		}
	}
}
