package com.example.macrotracker

import com.google.firebase.Timestamp

data class FoodItem(
    val name: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    var servingSize: Double = 1.0,
    var unit: String = "serving",
    val timestamp: Timestamp? = null
)
