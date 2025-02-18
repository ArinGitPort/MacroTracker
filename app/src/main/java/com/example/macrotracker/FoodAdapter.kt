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

// onAddClick now takes FoodItem, multiplier (Double), and unit (String)
class FoodAdapter(
    private val foodList: List<FoodItem>,
    private val onAddClick: (FoodItem, Double, String) -> Unit
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]
        holder.foodName.text = food.name
        holder.caloriesText.text = "Calories: ${food.calories}"
        holder.proteinText.text = "Protein: ${food.protein}g"
        holder.carbsText.text = "Carbs: ${food.carbs}g"
        holder.fatsText.text = "Fats: ${food.fats}g"

        // When addButton is clicked, prompt for measurement type and value.
        holder.addButton.setOnClickListener {
            val context = holder.itemView.context
            val options = arrayOf("Servings", "Grams")
            var selectedOption = 0 // default to Servings
            AlertDialog.Builder(context)
                .setTitle("Choose Measurement")
                .setSingleChoiceItems(options, 0) { _, which ->
                    selectedOption = which
                }
                .setPositiveButton("Next") { dialog, _ ->
                    dialog.dismiss()
                    // Prompt for the value
                    val valueInput = EditText(context).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    }
                    AlertDialog.Builder(context)
                        .setTitle("Enter value (${options[selectedOption]})")
                        .setView(valueInput)
                        .setPositiveButton("OK") { valueDialog, _ ->
                            val valueStr = valueInput.text.toString()
                            // If "Servings", multiplier = entered value.
                            // If "Grams", assume default serving is 100g, so multiplier = entered grams / 100.
                            val multiplier = when (selectedOption) {
                                0 -> valueStr.toDoubleOrNull() ?: 1.0
                                1 -> {
                                    val grams = valueStr.toDoubleOrNull() ?: 100.0
                                    grams / 100.0
                                }
                                else -> 1.0
                            }
                            val unit = if (selectedOption == 0) "serving" else "g"
                            onAddClick(food, multiplier, unit)
                            valueDialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { valueDialog, _ ->
                            valueDialog.cancel()
                        }
                        .show()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        }
    }

    override fun getItemCount(): Int = foodList.size
}
