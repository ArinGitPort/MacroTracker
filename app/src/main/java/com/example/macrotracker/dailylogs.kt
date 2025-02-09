package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityDailylogsBinding

class dailylogs : AppCompatActivity() {
    private lateinit var binding: ActivityDailylogsBinding
    private lateinit var dailyLogsAdapter: DailyLogsAdapter
    private val loggedFoods = mutableListOf<FoodItem>() // Stores logged food items

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding Setup
        binding = ActivityDailylogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView Setup with onRemoveClick handler
        dailyLogsAdapter = DailyLogsAdapter(loggedFoods) { foodItem ->
            removeFoodItem(foodItem)
        }
        binding.dailyFoodRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@dailylogs)
            adapter = dailyLogsAdapter
        }

        // Handle Back Button Navigation
        binding.backButton.setOnClickListener {
            val intent = Intent(this, landingpage::class.java)
            startActivity(intent)
            finish() // Closes this activity to prevent stacking
        }

        // Receive data from Landing Page
        handleIncomingFoodData()
    }

    private fun handleIncomingFoodData() {
        val foodName = intent.getStringExtra("foodName")
        val calories = intent.getIntExtra("calories", 0)
        val protein = intent.getIntExtra("protein", 0)
        val carbs = intent.getIntExtra("carbs", 0)
        val fats = intent.getIntExtra("fats", 0)

        if (!foodName.isNullOrEmpty()) {
            val foodItem = FoodItem(foodName, calories, protein, carbs, fats)
            loggedFoods.add(foodItem) // Add new food item to the list
            dailyLogsAdapter.notifyDataSetChanged() // Update RecyclerView
        }
    }

    private fun removeFoodItem(foodItem: FoodItem) {
        val position = loggedFoods.indexOf(foodItem)
        if (position != -1) {
            loggedFoods.removeAt(position) // Remove item from list
            dailyLogsAdapter.notifyItemRemoved(position) // Notify RecyclerView
        }
    }
}
