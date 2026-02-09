package com.example.fitnesscoachai.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.auth.AuthActivity
import com.example.fitnesscoachai.ui.history.HistoryActivity
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile(view)
        loadQuickStats(view)
        loadFitnessProfile(view)
        loadAchievements(view)
        loadTrainingInsights(view)
        loadRecentActivity(view)
        setupSettings(view)
    }

    private fun loadUserProfile(view: View) {
        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Azamat") ?: "Azamat"

        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        tvUserName.text = userName

        val profilePrefs = requireContext().getSharedPreferences("user_profile", android.content.Context.MODE_PRIVATE)
        val fitnessLevel = profilePrefs.getString("fitness_level", "Beginner") ?: "Beginner"
        val trainingGoal = profilePrefs.getString("training_goal", null)

        val tvUserSubtitle = view.findViewById<TextView>(R.id.tvUserSubtitle)
        tvUserSubtitle.text = if (trainingGoal != null) {
            "Training Goal: $trainingGoal"
        } else {
            "Fitness Level: $fitnessLevel"
        }
    }

    private fun loadQuickStats(view: View) {
        val workoutPrefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = workoutPrefs.getInt("history_count", 0)

        val tvTotalWorkouts = view.findViewById<TextView>(R.id.tvTotalWorkouts)
        tvTotalWorkouts.text = historyCount.toString()

        // Calculate total reps
        var totalReps = 0
        for (i in 0 until historyCount) {
            val reps = workoutPrefs.getInt("reps_$i", 0)
            totalReps += reps
        }

        val tvTotalReps = view.findViewById<TextView>(R.id.tvTotalReps)
        tvTotalReps.text = totalReps.toString()

        // Calculate average form score (simplified)
        val tvAvgFormScore = view.findViewById<TextView>(R.id.tvAvgFormScore)
        val avgScore = if (historyCount > 0) {
            // Placeholder calculation - in real app would use actual form scores
            78
        } else {
            0
        }
        tvAvgFormScore.text = "$avgScore%"
    }

    private fun loadFitnessProfile(view: View) {
        val prefs = requireContext().getSharedPreferences("user_profile", android.content.Context.MODE_PRIVATE)

        val tvAge = view.findViewById<TextView>(R.id.tvAge)
        val age = prefs.getInt("age", 25)
        tvAge.text = age.toString()

        val tvWeight = view.findViewById<TextView>(R.id.tvWeight)
        val weight = prefs.getFloat("weight", 75f)
        tvWeight.text = "${weight.toInt()} kg"

        val tvHeight = view.findViewById<TextView>(R.id.tvHeight)
        val height = prefs.getFloat("height", 180f)
        tvHeight.text = "${height.toInt()} cm"

        val tvTrainingFrequency = view.findViewById<TextView>(R.id.tvTrainingFrequency)
        val frequency = prefs.getString("training_frequency", "3-4 times per week") ?: "3-4 times per week"
        tvTrainingFrequency.text = frequency

        val tvLimitations = view.findViewById<TextView>(R.id.tvLimitations)
        val limitations = prefs.getString("injuries", "No Limitations") ?: "No Limitations"
        tvLimitations.text = limitations

        // Edit Profile button (placeholder - would navigate to edit screen)
        val btnEditProfile = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditProfile)
        btnEditProfile.setOnClickListener {
            // TODO: Navigate to edit profile screen
        }
    }

    private fun loadAchievements(view: View) {
        val llAchievements = view.findViewById<LinearLayout>(R.id.llAchievements)
        val workoutPrefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = workoutPrefs.getInt("history_count", 0)

        val achievements = mutableListOf<String>()

        if (historyCount >= 1) {
            achievements.add("First workout completed")
        }
        if (historyCount >= 5) {
            achievements.add("5 workouts done")
        }

        // Calculate consistency streak (simplified)
        val streak = calculateStreak(workoutPrefs, historyCount)
        if (streak > 0) {
            achievements.add("Consistency streak: $streak days")
        }

        if (achievements.isEmpty()) {
            val noAchievements = TextView(requireContext()).apply {
                text = "Complete your first workout to unlock achievements!"
                textSize = 14f
                setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            llAchievements.addView(noAchievements)
        } else {
            achievements.forEach { achievement ->
                val achievementView = TextView(requireContext()).apply {
                    text = "✓ $achievement"
                    textSize = 14f
                    setPadding(0, 8, 0, 8)
                }
                llAchievements.addView(achievementView)
            }
        }
    }

    private fun calculateStreak(prefs: android.content.SharedPreferences, historyCount: Int): Int {
        if (historyCount == 0) return 0

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dates = mutableListOf<Long>()
        for (i in 0 until historyCount) {
            val date = prefs.getLong("date_$i", 0)
            if (date > 0) {
                dates.add(date)
            }
        }

        dates.sortDescending()

        var streak = 0
        var currentDate = today
        val oneDay = 24 * 60 * 60 * 1000L

        dates.forEach { date ->
            val dateCalendar = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (dateCalendar == currentDate || dateCalendar == currentDate - oneDay) {
                if (dateCalendar == currentDate - oneDay) {
                    currentDate = dateCalendar
                }
                streak++
            } else {
                return streak
            }
        }

        return streak
    }

    private fun loadTrainingInsights(view: View) {
        val workoutPrefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = workoutPrefs.getInt("history_count", 0)

        // Most common mistake (placeholder)
        val tvCommonMistake = view.findViewById<TextView>(R.id.tvCommonMistake)
        tvCommonMistake.text = if (historyCount > 0) "Knees moving inward" else "No data yet"

        // Best exercise (calculate from history)
        val tvBestExercise = view.findViewById<TextView>(R.id.tvBestExercise)
        val exerciseCounts = mutableMapOf<String, Int>()
        for (i in 0 until historyCount) {
            val exercise = workoutPrefs.getString("exercise_$i", null)
            if (exercise != null) {
                exerciseCounts[exercise] = exerciseCounts.getOrDefault(exercise, 0) + 1
            }
        }
        val bestExercise = exerciseCounts.maxByOrNull { it.value }?.key ?: "Squat"
        tvBestExercise.text = bestExercise

        // AI Accuracy
        val tvAIAccuracy = view.findViewById<TextView>(R.id.tvAIAccuracy)
        tvAIAccuracy.text = "92%"
    }

    private fun loadRecentActivity(view: View) {
        val llRecentWorkouts = view.findViewById<LinearLayout>(R.id.llRecentWorkouts)
        val workoutPrefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = workoutPrefs.getInt("history_count", 0)

        if (historyCount == 0) {
            val noActivity = TextView(requireContext()).apply {
                text = "No workouts yet"
                textSize = 14f
                setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            }
            llRecentWorkouts.addView(noActivity)
            return
        }

        // Get recent workouts (last 3)
        val workouts = mutableListOf<Triple<String, Int, Long>>()
        for (i in 0 until historyCount) {
            val exercise = workoutPrefs.getString("exercise_$i", null)
            val reps = workoutPrefs.getInt("reps_$i", 0)
            val date = workoutPrefs.getLong("date_$i", 0)
            if (exercise != null && date > 0) {
                workouts.add(Triple(exercise, reps, date))
            }
        }

        workouts.sortByDescending { it.third }
        val recentWorkouts = workouts.take(3)

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        recentWorkouts.forEach { (exercise, reps, date) ->
            val workoutDate = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dateText = when {
                workoutDate == today -> "Today"
                workoutDate == today - 24 * 60 * 60 * 1000L -> "Yesterday"
                else -> dateFormat.format(Date(date))
            }

            val workoutView = TextView(requireContext()).apply {
                text = "$exercise · $reps reps · $dateText"
                textSize = 14f
                setPadding(0, 8, 0, 8)
            }
            llRecentWorkouts.addView(workoutView)
        }

        val btnViewFullHistory = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewFullHistory)
        btnViewFullHistory.setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }
    }

    private fun setupSettings(view: View) {
        val llLogout = view.findViewById<LinearLayout>(R.id.llLogout)
        llLogout.setOnClickListener {
            requireActivity()
                .getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }

        // Units and Theme are placeholders - would open dialogs/sheets in real implementation
    }
}
