package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.macrotracker.databinding.ActivityAdminLogsBinding

class admin_logs : AppCompatActivity() {
    private lateinit var binding: ActivityAdminLogsBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: com.google.firebase.auth.FirebaseAuth

    // List for feedback items
    private val feedbackList = mutableListOf<FeedbackItem>()
    private lateinit var feedbackAdapter: FeedbackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Initialize FirebaseAuth
        auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup RecyclerView for Feedback
        feedbackAdapter = FeedbackAdapter(feedbackList)
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

        // Fetch data from Firestore
        fetchFeedbacks()
    }

    private fun fetchFeedbacks() {
        db.collection("feedbox")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                feedbackList.clear()
                for (doc in documents) {
                    val item = doc.toObject(FeedbackItem::class.java)
                    feedbackList.add(item)
                }
                feedbackAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching feedback: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    // Data model for feedback
    data class FeedbackItem(
        val username: String = "",
        val feedback: String = "",
        val timestamp: Timestamp? = null
    )

    // Adapter for feedback items
    inner class FeedbackAdapter(private val feedbacks: List<FeedbackItem>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

        inner class FeedbackViewHolder(itemView: android.view.View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val usernameText: TextView = itemView.findViewById(R.id.feedbackUsername)
            val feedbackText: TextView = itemView.findViewById(R.id.feedbackText)
            val timeText: TextView = itemView.findViewById(R.id.feedbackTime)
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
        }

        override fun getItemCount(): Int = feedbacks.size

        private fun formatTimestamp(timestamp: Timestamp?): String {
            return if (timestamp != null) {
                val date = timestamp.toDate()
                val sdf = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
                "Logged on: " + sdf.format(date)
            } else {
                "No Date"
            }
        }
    }
}
