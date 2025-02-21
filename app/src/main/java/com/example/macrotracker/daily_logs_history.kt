package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macrotracker.databinding.ActivityDailyLogsHistoryBinding
import com.google.firebase.firestore.FirebaseFirestore

class daily_logs_history : AppCompatActivity() {
    private lateinit var binding: ActivityDailyLogsHistoryBinding
    private val db = FirebaseFirestore.getInstance()
    private val historyLogs = mutableListOf<FoodItem>()
    private lateinit var adapter: dailylogshistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge if needed
        enableEdgeToEdge()
        binding = ActivityDailyLogsHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RecyclerView
        binding.dailyLogsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = dailylogshistoryAdapter(historyLogs)
        binding.dailyLogsRecyclerView.adapter = adapter

        // Fetch history logs from Firestore
        fetchHistoryLogs()

        // Back button functionality: navigate to landingpage
        binding.backButton.setOnClickListener {
            startActivity(Intent(this, userprofile::class.java))
            finish()
        }
    }

    private fun fetchHistoryLogs() {
        db.collection("daily_logs_history")
            .orderBy("resetDate") // Adjust sorting as needed
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
