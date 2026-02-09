package com.example.fitnesscoachai.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.exercise.ExerciseSelectActivity
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Azamat") ?: "Azamat"

        // Set user name
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        tvUserName.text = "$userName 👋"

        // Set sub-greeting
        val tvSubGreeting = view.findViewById<TextView>(R.id.tvSubGreeting)
        tvSubGreeting.text = "Your AI trainer is ready"

        // Load and display today's activity
        loadTodaysActivity(view)

        // Start first training button
        val btnStartFirstTraining = view.findViewById<MaterialButton>(R.id.btnStartFirstTraining)
        btnStartFirstTraining.setOnClickListener {
            startActivity(Intent(requireContext(), ExerciseSelectActivity::class.java))
        }

        // Load weekly progress
        loadWeeklyProgress(view)

        // Load overall status
        loadOverallStatus(view)
    }

    private fun loadTodaysActivity(view: View) {
        val prefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = prefs.getInt("history_count", 0)
        val llTodayActivity = view.findViewById<LinearLayout>(R.id.llTodayActivity)
        val tvNoActivity = view.findViewById<TextView>(R.id.tvNoActivity)
        val btnStartFirstTraining = view.findViewById<MaterialButton>(R.id.btnStartFirstTraining)

        // Check if there's activity today
        val today = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        var hasTodayActivity = false

        for (i in 0 until historyCount) {
            val date = prefs.getLong("date_$i", 0)
            if (date > 0 && (today - date) < oneDayInMillis) {
                hasTodayActivity = true
                break
            }
        }

        if (hasTodayActivity) {
            tvNoActivity.visibility = View.GONE
            btnStartFirstTraining.visibility = View.GONE

            // Add activity items (simplified - in real app would load from database)
            // For demo, showing sample data
            addActivityItem(llTodayActivity, "Squat", 15, "Good")
            addActivityItem(llTodayActivity, "Push-up", 10, "Needs improvement")
        } else {
            tvNoActivity.visibility = View.VISIBLE
            btnStartFirstTraining.visibility = View.VISIBLE
        }
    }

    private fun addActivityItem(parent: LinearLayout, exercise: String, reps: Int, form: String) {
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val exerciseText = TextView(requireContext()).apply {
            text = exercise
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val detailsText = TextView(requireContext()).apply {
            text = "Reps: $reps | Form: $form"
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }

        itemLayout.addView(exerciseText)
        itemLayout.addView(detailsText)
        parent.addView(itemLayout)
    }

    private fun loadWeeklyProgress(view: View) {
        val prefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = prefs.getInt("history_count", 0)
        
        // Calculate weekly workouts (simplified - count last 7 days)
        val weekInMillis = 7 * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        var weeklyWorkouts = 0
        
        for (i in 0 until historyCount) {
            val date = prefs.getLong("date_$i", 0)
            if (date > 0 && (now - date) < weekInMillis) {
                weeklyWorkouts++
            }
        }

        val tvWeeklyProgressSub = view.findViewById<TextView>(R.id.tvWeeklyProgressSub)
        val tvWeeklyProgressPercent = view.findViewById<TextView>(R.id.tvWeeklyProgressPercent)

        if (weeklyWorkouts > 0) {
            tvWeeklyProgressSub.text = "$weeklyWorkouts workouts completed"
            // Calculate percentage (assuming goal is 5 workouts per week)
            val percentage = (weeklyWorkouts * 100 / 5).coerceAtMost(100)
            tvWeeklyProgressPercent.text = "$percentage%"
        } else {
            tvWeeklyProgressSub.text = "Training consistency"
            tvWeeklyProgressPercent.text = "0%"
        }
    }

    private fun loadOverallStatus(view: View) {
        val prefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = prefs.getInt("history_count", 0)

        val tvTotalWorkouts = view.findViewById<TextView>(R.id.tvTotalWorkouts)
        tvTotalWorkouts.text = historyCount.toString()

        // Calculate average form score (simplified - in real app would calculate from actual scores)
        val tvAverageFormScore = view.findViewById<TextView>(R.id.tvAverageFormScore)
        val averageScore = if (historyCount > 0) {
            // Placeholder calculation
            78
        } else {
            0
        }
        tvAverageFormScore.text = "$averageScore%"
    }
}
