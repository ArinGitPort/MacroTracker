package com.example.macrotracker

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.macrotracker.databinding.ActivityAdminLogsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class admin_logs : AppCompatActivity() {

    private lateinit var binding: ActivityAdminLogsBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: com.google.firebase.auth.FirebaseAuth

    // Full list of feedback items fetched from Firestore
    private val fullFeedbackList = mutableListOf<FeedbackItem>()
    // List that will be filtered based on search keyword and date range
    private var filteredFeedbackList = mutableListOf<FeedbackItem>()
    private lateinit var feedbackAdapter: FeedbackAdapter

    // Filtering variables
    private var keywordFilter: String = ""
    private var startDateFilter: Date? = null
    private var endDateFilter: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize FirebaseAuth
        auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        // Setup RecyclerView for feedback
        feedbackAdapter = FeedbackAdapter(filteredFeedbackList)
        binding.feedbackRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.feedbackRecyclerView.adapter = feedbackAdapter

        // Setup Logout Button functionality
        val logoutButton = findViewById<MaterialButton>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, loginpage::class.java))
            finish()
        }

        // Setup Clear Button functionality
        val clearButton = findViewById<MaterialButton>(R.id.clearButton)
        clearButton.setOnClickListener {
            showClearFeedbackConfirmationDialog()
        }

        // Setup search bar filter (EditText with ID "searchBar" in your layout)
        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                keywordFilter = s.toString().trim()
                filterFeedbackList()
            }
        })

        // Setup date filter button (MaterialButton with ID "dateFilterButton" in your layout)
        val dateFilterButton = findViewById<MaterialButton>(R.id.dateFilterButton)
        dateFilterButton.setOnClickListener {
            showDateRangePicker()
        }

        // Fetch feedback data from Firestore
        fetchFeedbacks()
    }

    private fun fetchFeedbacks() {
        db.collection("feedbox")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                fullFeedbackList.clear()
                for (doc in documents) {
                    val item = doc.toObject(FeedbackItem::class.java).copy(docId = doc.id)
                    fullFeedbackList.add(item)
                }
                // Initially, display the full list.
                filteredFeedbackList.clear()
                filteredFeedbackList.addAll(fullFeedbackList)
                feedbackAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching feedback: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Combines keyword and date range filters to update the feedback list.
     */
    private fun filterFeedbackList() {
        val filtered = fullFeedbackList.filter { item ->
            // Check keyword filter: username or feedback should contain the keyword.
            val matchesKeyword = if (keywordFilter.isNotBlank()) {
                item.username.lowercase(Locale.getDefault()).contains(keywordFilter.lowercase(Locale.getDefault())) ||
                        item.feedback.lowercase(Locale.getDefault()).contains(keywordFilter.lowercase(Locale.getDefault()))
            } else {
                true
            }
            // Check date filter: if both start and end dates are set, the item's date must be within the range.
            val matchesDate = if (startDateFilter != null && endDateFilter != null && item.timestamp != null) {
                val date = item.timestamp.toDate()
                !date.before(startDateFilter) && !date.after(endDateFilter)
            } else {
                true
            }
            matchesKeyword && matchesDate
        }
        filteredFeedbackList.clear()
        filteredFeedbackList.addAll(filtered)
        feedbackAdapter.notifyDataSetChanged()
    }

    /**
     * Shows a DatePickerDialog to select a date range for filtering feedback.
     * After selecting the start date, it then prompts for an end date.
     */
    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Set start date at beginning of day
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                startDateFilter = calendar.time

                // Now prompt for end date
                DatePickerDialog(
                    this,
                    { _, endYear, endMonth, endDayOfMonth ->
                        calendar.set(endYear, endMonth, endDayOfMonth, 23, 59, 59)
                        endDateFilter = calendar.time
                        // Apply filter after both dates are selected
                        filterFeedbackList()
                        Toast.makeText(
                            this,
                            "Filtering from ${formatDate(startDateFilter!!)} to ${formatDate(endDateFilter!!)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
        return sdf.format(date)
    }

    /**
     * Displays a confirmation dialog to clear all feedback from the "feedbox" collection.
     */
    private fun showClearFeedbackConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Clear Feedback")
        builder.setMessage("Are you sure you want to clear all feedback?")
        builder.setPositiveButton("Clear") { dialog, _ ->
            clearFeedbacks()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    /**
     * Clears all documents from the "feedbox" collection.
     */
    private fun clearFeedbacks() {
        db.collection("feedbox")
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (doc in documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Feedback cleared", Toast.LENGTH_SHORT).show()
                    fetchFeedbacks()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to clear feedback: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error clearing feedback: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Data model for feedback items
    data class FeedbackItem(
        val username: String = "",
        val feedback: String = "",
        val timestamp: Timestamp? = null,
        val docId: String = ""
    )

    // Adapter for feedback items
    inner class FeedbackAdapter(private var feedbacks: List<FeedbackItem>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

        inner class FeedbackViewHolder(itemView: android.view.View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val usernameText: TextView = itemView.findViewById(R.id.feedbackUsername)
            val feedbackText: TextView = itemView.findViewById(R.id.feedbackText)
            val timeText: TextView = itemView.findViewById(R.id.feedbackTime)
            val removeButton: Button = itemView.findViewById(R.id.removeFeedbackButton)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FeedbackViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feedback, parent, false)
            return FeedbackViewHolder(view)
        }

        override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
            val item = feedbacks[position]
            holder.usernameText.text = item.username
            holder.feedbackText.text = item.feedback
            holder.timeText.text = formatTimestamp(item.timestamp)
            // Remove button functionality: delete the feedback from Firestore.
            holder.removeButton.setOnClickListener {
                db.collection("feedbox").document(item.docId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@admin_logs, "Feedback removed", Toast.LENGTH_SHORT).show()
                        fullFeedbackList.remove(item)
                        filterFeedbackList()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@admin_logs, "Failed to remove feedback: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        override fun getItemCount(): Int = feedbacks.size

        fun updateList(newList: List<FeedbackItem>) {
            feedbacks = newList
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
}
