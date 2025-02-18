package com.example.macrotracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DailyLogsAdapter(
    private val loggedFoods: MutableList<FoodItem>,
    private val onRemoveClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<DailyLogsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodName: TextView = view.findViewById(R.id.foodName)
        val calories: TextView = view.findViewById(R.id.calories)
        val protein: TextView = view.findViewById(R.id.protein)
        val carbs: TextView = view.findViewById(R.id.carbs)
        val fats: TextView = view.findViewById(R.id.fats)
        val serving: TextView = view.findViewById(R.id.serving)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dailylog, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = loggedFoods[position]
        holder.foodName.text = food.name
        holder.calories.text = "Calories: ${food.calories} kcal"
        holder.protein.text = "Protein: ${food.protein}g"
        holder.carbs.text = "Carbs: ${food.carbs}g"
        holder.fats.text = "Fats: ${food.fats}g"
        // Display the serving information (e.g., "Serving: 1 serving" or "Serving: 200 g")
        holder.serving.text = "Serving: ${food.servingSize} ${food.unit}"

        // Handle delete button click
        holder.deleteButton.setOnClickListener {
            onRemoveClick(food)
        }
    }

    override fun getItemCount(): Int = loggedFoods.size
}
