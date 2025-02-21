package com.example.macrotracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class dailylogshistoryAdapter(private var foodList: List<FoodItem>) :
    RecyclerView.Adapter<dailylogshistoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodName: TextView = itemView.findViewById(R.id.foodName)
        val logDateTextView: TextView = itemView.findViewById(R.id.logDateTextView)
        val calories: TextView = itemView.findViewById(R.id.calories)
        val protein: TextView = itemView.findViewById(R.id.protein)
        val carbs: TextView = itemView.findViewById(R.id.carbs)
        val fats: TextView = itemView.findViewById(R.id.fats)
        val serving: TextView = itemView.findViewById(R.id.serving)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Make sure your adapter XML is named "activity_dailylogshistory_adapter.xml"
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_dailylogshistory_adapter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val foodItem = foodList[position]
        holder.foodName.text = foodItem.name
        holder.calories.text = "Calories: ${foodItem.calories} kcal"
        holder.protein.text = "Protein: ${foodItem.protein}g"
        holder.carbs.text = "Carbs: ${foodItem.carbs}g"
        holder.fats.text = "Fats: ${foodItem.fats}g"
        holder.serving.text = "Serving: ${foodItem.servingSize} ${foodItem.unit}"
        holder.logDateTextView.text = formatTimestamp(foodItem.timestamp)
    }

    override fun getItemCount(): Int = foodList.size

    fun updateList(newList: List<FoodItem>) {
        foodList = newList
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            val date = timestamp.toDate()
            val sdf = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
            "Logged on: " + sdf.format(date)
        } else {
            "No Date"
        }
    }
}
