package com.example.macrotracker

import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// onAddClick now takes both the FoodItem and a Double multiplier.
class FoodAdapter(
    private val foodList: List<FoodItem>,
    private val onAddClick: (FoodItem, Double) -> Unit
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

        // Display nutritional information
        holder.caloriesText.text = "Calories: ${food.calories}"
        holder.proteinText.text = "Protein: ${food.protein}g"
        holder.carbsText.text = "Carbs: ${food.carbs}g"
        holder.fatsText.text = "Fats: ${food.fats}g"

        // When the Add button is clicked, show a dialog for measurement type and value.
        holder.addButton.setOnClickListener {
            val context = holder.itemView.context
            // Step 1: Let the user choose the measurement type.
            val options = arrayOf("Servings", "Grams")
            var selectedOption = 0 // default to Servings
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Choose Measurement")
            builder.setSingleChoiceItems(options, 0) { _, which ->
                selectedOption = which
            }
            builder.setPositiveButton("Next") { dialog, _ ->
                dialog.dismiss()
                // Step 2: Prompt for the numeric value.
                val valueInput = EditText(context)
                valueInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                val valueBuilder = AlertDialog.Builder(context)
                valueBuilder.setTitle("Enter value (${options[selectedOption]})")
                valueBuilder.setView(valueInput)
                valueBuilder.setPositiveButton("OK") { valueDialog, _ ->
                    val valueStr = valueInput.text.toString()
                    // If Servings is selected, use the value directly.
                    // If Grams is selected, assume the food's values represent 100g; multiplier = entered grams / 100.
                    val multiplier = when (selectedOption) {
                        0 -> valueStr.toDoubleOrNull() ?: 1.0
                        1 -> (valueStr.toDoubleOrNull() ?: 100.0) / 100.0
                        else -> 1.0
                    }
                    // Invoke the callback; duplicates are allowed.
                    onAddClick(food, multiplier)
                    valueDialog.dismiss()
                }
                valueBuilder.setNegativeButton("Cancel") { valueDialog, _ ->
                    valueDialog.cancel()
                }
                valueBuilder.show()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.show()
        }
    }

    override fun getItemCount(): Int = foodList.size
}
