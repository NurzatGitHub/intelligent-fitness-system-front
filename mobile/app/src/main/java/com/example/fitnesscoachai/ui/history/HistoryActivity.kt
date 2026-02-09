package com.example.fitnesscoachai.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.summary.SummaryActivity
import java.text.SimpleDateFormat
import java.util.*

data class WorkoutHistoryItem(
    val exercise: String,
    val reps: Int,
    val duration: Int,
    val date: Long
)

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: WorkoutHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistory = findViewById(R.id.rvHistory)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = WorkoutHistoryAdapter { item ->
            // Navigate to workout details (could be SummaryActivity with read-only mode)
            val intent = Intent(this, SummaryActivity::class.java)
            intent.putExtra("exercise_name", item.exercise)
            intent.putExtra("duration", item.duration)
            intent.putExtra("reps", item.reps)
            startActivity(intent)
        }

        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter

        loadWorkoutHistory()
    }

    private fun loadWorkoutHistory() {
        val prefs = getSharedPreferences("workout_history", MODE_PRIVATE)
        val historyCount = prefs.getInt("history_count", 0)

        val historyItems = mutableListOf<WorkoutHistoryItem>()

        for (i in 0 until historyCount) {
            val exercise = prefs.getString("exercise_$i", null)
            val duration = prefs.getInt("duration_$i", 0)
            val reps = prefs.getInt("reps_$i", 0)
            val date = prefs.getLong("date_$i", 0)

            if (exercise != null && date > 0) {
                historyItems.add(WorkoutHistoryItem(exercise, reps, duration, date))
            }
        }

        // Sort by date (newest first)
        historyItems.sortByDescending { it.date }

        if (historyItems.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            adapter.submitList(historyItems)
        }
    }
}

class WorkoutHistoryAdapter(
    private val onItemClick: (WorkoutHistoryItem) -> Unit
) : RecyclerView.Adapter<WorkoutHistoryAdapter.ViewHolder>() {

    private var items = listOf<WorkoutHistoryItem>()

    fun submitList(newItems: List<WorkoutHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvExercise: TextView = itemView.findViewById(R.id.tvExercise)
        private val tvReps: TextView = itemView.findViewById(R.id.tvReps)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)

        fun bind(item: WorkoutHistoryItem) {
            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(item.date))

            tvExercise.text = item.exercise
            tvReps.text = "Reps: ${item.reps}"

            // Format duration
            val minutes = item.duration / 60
            val seconds = item.duration % 60
            tvDuration.text = String.format("%02d:%02d", minutes, seconds)
        }
    }
}
