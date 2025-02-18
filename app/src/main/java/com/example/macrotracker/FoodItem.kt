package com.example.macrotracker

data class FoodItem(
    val name: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0,
    var servingSize: Double = 1.0,  // Default is 1 serving
    var unit: String = "serving"  // Can be "g" or "serving"
)
