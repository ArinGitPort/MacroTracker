package com.example.macrotracker

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DailyLogsAdapter(
    private val loggedFoods: MutableList<FoodItem>,
    private val onRemoveClick: (FoodItem) -> Unit,  // Called when food is removed
    private val onEditClick: () -> Unit             // Called when food is edited
) : RecyclerView.Adapter<DailyLogsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodName: TextView = view.findViewById(R.id.foodName)
        val calories: TextView = view.findViewById(R.id.calories)
        val protein: TextView = view.findViewById(R.id.protein)
        val carbs: TextView = view.findViewById(R.id.carbs)
        val fats: TextView = view.findViewById(R.id.fats)
        val serving: TextView = view.findViewById(R.id.serving)
        val timestamp: TextView = view.findViewById(R.id.timestamp) // Timestamp field
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
        val editButton: Button = view.findViewById(R.id.editButton)
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
        holder.serving.text = "Serving: ${food.servingSize} ${food.unit}"

        // Display timestamp (if available)
        if (food.timestamp != null) {
            holder.timestamp.text = "Last Updated: ${formatTimestamp(food.timestamp!!)}"
        } else {
            holder.timestamp.text = "Last Updated: N/A"
        }

        // Handle delete button click
        holder.deleteButton.setOnClickListener {
            onRemoveClick(food)
        }

        // Handle edit button click
        holder.editButton.setOnClickListener {
            showEditDialog(holder.itemView.context, food)
        }
    }

    override fun getItemCount(): Int = loggedFoods.size

    /**
     * Show a dialog to edit serving size and update Firestore.
     */
    private fun showEditDialog(context: Context, food: FoodItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.edit_serving_dialog, null)
        val servingInput = dialogView.findViewById<EditText>(R.id.servingInput)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val updateButton = dialogView.findViewById<Button>(R.id.updateButton)

        servingInput.setText(food.servingSize.toString())

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        updateButton.setOnClickListener {
            val newServingSize = servingInput.text.toString().toDoubleOrNull()
            if (newServingSize != null && newServingSize > 0) {
                updateFoodServing(context, food, newServingSize)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    /**
     * Updates the serving size in Firestore and timestamps it.
     */
    private fun updateFoodServing(context: Context, food: FoodItem, newServingSize: Double) {
        val db = FirebaseFirestore.getInstance()
        db.collection("daily_logs")
            .whereEqualTo("name", food.name)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val docRef = db.collection("daily_logs").document(document.id)

                    // Calculate new macros based on serving size change
                    val scaleFactor = newServingSize / food.servingSize
                    val updatedFood = mapOf(
                        "calories" to (food.calories * scaleFactor).toInt(),
                        "protein" to (food.protein * scaleFactor).toInt(),
                        "carbs" to (food.carbs * scaleFactor).toInt(),
                        "fats" to (food.fats * scaleFactor).toInt(),
                        "servingSize" to newServingSize,
                        "unit" to food.unit,
                        "timestamp" to Timestamp.now() // Store updated timestamp
                    )

                    docRef.update(updatedFood)
                        .addOnSuccessListener {
                            onEditClick()
                            Toast.makeText(context, "Serving updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error finding food item", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Formats the Firestore timestamp to a readable date string.
     */
    private fun formatTimestamp(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

        sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")

        return sdf.format(date)
    }

}
