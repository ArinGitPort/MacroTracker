package com.example.macrotracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FoodAdapter(
    private val foodList: List<FoodItem>,
    private val onAddClick: (FoodItem) -> Unit // Click Listener for Add Button
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodName: TextView = itemView.findViewById(R.id.foodName)
        val addButton: Button = itemView.findViewById(R.id.addButton) // Reference to the Add button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]
        holder.foodName.text = food.name

        // Handle add button click
        holder.addButton.setOnClickListener {
            onAddClick(food) // Calls the function to send data to Daily Logs
        }
    }

    override fun getItemCount(): Int = foodList.size
}
