package com.example.macrotracker

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

// onAddClick now takes both the FoodItem and the quantity.
class FoodAdapter(
    private val foodList: List<FoodItem>,
    private val onAddClick: (FoodItem, Int) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodName: TextView = itemView.findViewById(R.id.foodName)
        val addButton: Button = itemView.findViewById(R.id.addButton)
        val caloriesText: TextView = itemView.findViewById(R.id.caloriesText)
        val proteinText: TextView = itemView.findViewById(R.id.proteinText)
        val carbsText: TextView = itemView.findViewById(R.id.carbsText)
        val fatsText: TextView = itemView.findViewById(R.id.fatsText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]
        holder.foodName.text = food.name

        // Update macros display for this food item.
        holder.caloriesText.text = "Calories: ${food.calories}"
        holder.proteinText.text = "Protein: ${food.protein}g"
        holder.carbsText.text = "Carbs: ${food.carbs}g"
        holder.fatsText.text = "Fats: ${food.fats}g"

        // When addButton is clicked, show an AlertDialog to ask for quantity.
        holder.addButton.setOnClickListener {
            val context = holder.itemView.context
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Enter Quantity")

            // Create an EditText for quantity input.
            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                val quantityStr = input.text.toString()
                // Default to 1 if quantity is not provided or invalid.
                val quantity = quantityStr.toIntOrNull() ?: 1
                onAddClick(food, quantity)
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.show()
        }
    }

    override fun getItemCount(): Int = foodList.size
}
