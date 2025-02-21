package com.example.macrotracker

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class FoodAdapter(
    private var foodList: List<FoodItem>,
    private val onAddClick: (FoodItem, Double, String) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

        // Show the measurement selection dialog when add button is clicked.
        holder.addButton.setOnClickListener {
            showMeasurementDialog(holder.itemView, food)
        }
    }

    override fun getItemCount(): Int = foodList.size

    /**
     * Updates the list shown by the adapter.
     */
    fun updateList(newList: List<FoodItem>) {
        foodList = newList
        notifyDataSetChanged()
    }

    /**
     * Displays a custom dialog for selecting the measurement type and entering a value.
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

        // Set up input to allow decimal values.
        valueInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        // Default selection is "serving"
        servingRadio.isChecked = true

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {
            val valueStr = valueInput.text.toString()
            // Use 1.0 as default if no input provided.
            val multiplier = when {
                valueStr.isBlank() -> 1.0
                servingRadio.isChecked -> valueStr.toDoubleOrNull() ?: 1.0
                gramsRadio.isChecked -> {
                    // Assume that 100 grams equals 1 serving.
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
