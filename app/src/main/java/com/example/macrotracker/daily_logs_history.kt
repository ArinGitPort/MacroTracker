package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityDailyLogsHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class daily_logs_history : AppCompatActivity() {
    private lateinit var binding: ActivityDailyLogsHistoryBinding
    private val db = FirebaseFirestore.getInstance()
    private val historyLogs = mutableListOf<FoodItem>()
    private lateinit var adapter: dailylogshistoryAdapter
    private val auth = FirebaseAuth.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDailyLogsHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get current user ID once
        userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up RecyclerView
        binding.dailyLogsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = dailylogshistoryAdapter(historyLogs)
        binding.dailyLogsRecyclerView.adapter = adapter

        // Fetch history logs from Firestore from the user's subcollection
        fetchHistoryLogs()

        // Back button navigates to UserProfile (or adjust as needed)
        binding.backButton.setOnClickListener {
            startActivity(Intent(this, userprofile::class.java))
            finish()
        }
    }

    private fun fetchHistoryLogs() {
        val uid = userId ?: return
        db.collection("users").document(uid)
            .collection("daily_logs_history")
            .orderBy("resetDate")
            .get()
            .addOnSuccessListener { documents ->
                historyLogs.clear()
                for (doc in documents) {
                    val food = doc.toObject(FoodItem::class.java)
                    historyLogs.add(food)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching history logs", Toast.LENGTH_SHORT).show()
            }
    }
}
