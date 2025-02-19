package com.example.macrotracker

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

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

        // Open measurement selection dialog
        holder.addButton.setOnClickListener {
            showMeasurementDialog(holder.itemView, food)
        }
    }

    override fun getItemCount(): Int = foodList.size

    /**
     * Show a custom dialog for selecting measurement type and entering value.
     */
    private fun showMeasurementDialog(view: View, food: FoodItem) {
        val context = view.context
        val dialogView = LayoutInflater.from(context).inflate(R.layout.add_serving_dialog, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroup)
        val servingRadio = dialogView.findViewById<RadioButton>(R.id.radioServing)
        val gramsRadio = dialogView.findViewById<RadioButton>(R.id.radioGrams)
        val valueInput = dialogView.findViewById<EditText>(R.id.inputValue)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)

        valueInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        servingRadio.isChecked = true // Default selection

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // Cancel Button
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Confirm Button
        confirmButton.setOnClickListener {
            val valueStr = valueInput.text.toString()
            val multiplier = when {
                servingRadio.isChecked -> valueStr.toDoubleOrNull() ?: 1.0
                gramsRadio.isChecked -> {
                    val grams = valueStr.toDoubleOrNull() ?: 100.0
                    grams / 100.0
                }
                else -> 1.0
            }
            val unit = if (servingRadio.isChecked) "serving" else "g"

            onAddClick(food, multiplier, unit)
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
