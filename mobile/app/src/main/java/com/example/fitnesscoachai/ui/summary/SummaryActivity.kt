package com.example.fitnesscoachai.ui.summary

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.MainActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.history.HistoryActivity
import com.google.android.material.button.MaterialButton
import java.util.concurrent.TimeUnit

class SummaryActivity : AppCompatActivity() {

    private lateinit var tvExerciseName: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvTotalReps: TextView
    private lateinit var tvCommonMistakes: TextView
    private lateinit var tvOverallPerformance: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnGoToHistory: MaterialButton
    private lateinit var btnBackToHome: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val exerciseName = intent.getStringExtra("exercise_name") ?: "Exercise"
        val duration = intent.getIntExtra("duration", 0)
        val reps = intent.getIntExtra("reps", 0)

        initializeViews()
        populateData(exerciseName, duration, reps)
        setupListeners()
    }

    private fun initializeViews() {
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvDuration = findViewById(R.id.tvDuration)
        tvTotalReps = findViewById(R.id.tvTotalReps)
        tvCommonMistakes = findViewById(R.id.tvCommonMistakes)
        tvOverallPerformance = findViewById(R.id.tvOverallPerformance)
        btnSave = findViewById(R.id.btnSave)
        btnGoToHistory = findViewById(R.id.btnGoToHistory)
        btnBackToHome = findViewById(R.id.btnBackToHome)
    }

    private fun populateData(exerciseName: String, duration: Int, reps: Int) {
        tvExerciseName.text = exerciseName
        tvTotalReps.text = reps.toString()

        // Format duration
        val minutes = TimeUnit.SECONDS.toMinutes(duration.toLong())
        val seconds = duration % 60
        tvDuration.text = String.format("%02d:%02d", minutes, seconds)

        // Common mistakes (placeholder - would come from workout data)
        tvCommonMistakes.text = "Keep your back straight\nMaintain proper form"

        // Overall performance (placeholder logic)
        val performance = when {
            reps > 20 -> "Excellent"
            reps > 10 -> "Good"
            reps > 5 -> "Average"
            else -> "Keep practicing"
        }
        tvOverallPerformance.text = performance
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            // TODO: Save workout to database
            Toast.makeText(this, "Workout saved", Toast.LENGTH_SHORT).show()
            
            // Save to SharedPreferences for history (temporary)
            val prefs = getSharedPreferences("workout_history", MODE_PRIVATE)
            val historyCount = prefs.getInt("history_count", 0)
            prefs.edit()
                .putString("exercise_$historyCount", tvExerciseName.text.toString())
                .putInt("duration_$historyCount", getDurationInSeconds())
                .putInt("reps_$historyCount", tvTotalReps.text.toString().toIntOrNull() ?: 0)
                .putLong("date_$historyCount", System.currentTimeMillis())
                .putInt("history_count", historyCount + 1)
                .apply()
        }

        btnGoToHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }

        btnBackToHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun getDurationInSeconds(): Int {
        val durationText = tvDuration.text.toString()
        val parts = durationText.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts[1].toIntOrNull() ?: 0
            return minutes * 60 + seconds
        }
        return 0
    }
}
