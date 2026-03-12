package com.example.fitnesscoachai.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.auth.AuthActivity
import com.example.fitnesscoachai.ui.history.HistoryActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

class ProfileFragment : Fragment() {

    private val avatarPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    requireContext()
                        .getSharedPreferences("user_profile", AppCompatActivity.MODE_PRIVATE)
                        .edit()
                        .putString("avatar_uri", uri.toString())
                        .apply()

                    val ivAvatar = view?.findViewById<ImageView>(R.id.ivAvatar)
                    ivAvatar?.setImageURI(uri)
                    ivAvatar?.clearColorFilter()
                    ivAvatar?.background = null

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

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
        setupActions(view)
    }

    private fun setupActions(view: View) {
        view.findViewById<View>(R.id.btnChangeAvatar).setOnClickListener {
            avatarPicker.launch(arrayOf("image/*"))
        }

        view.findViewById<MaterialButton>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun loadUserProfile(view: View) {
        val authPrefs = requireContext().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
        val userName = authPrefs.getString("user_name", "Azamat") ?: "Azamat"

        val profilePrefs = requireContext().getSharedPreferences("user_profile", AppCompatActivity.MODE_PRIVATE)
        val fitnessLevel = profilePrefs.getString("fitness_level", "Beginner") ?: "Beginner"
        val trainingGoal = profilePrefs.getString("training_goal", null)
        val avatarUri = profilePrefs.getString("avatar_uri", null)

        view.findViewById<TextView>(R.id.tvUserName).text = userName
        view.findViewById<TextView>(R.id.tvUserSubtitle).text =
            if (trainingGoal != null) "Training Goal: $trainingGoal" else "Fitness Level: $fitnessLevel"

        val ivAvatar = view.findViewById<ImageView>(R.id.ivAvatar)
        if (!avatarUri.isNullOrBlank()) {
            ivAvatar.setImageURI(Uri.parse(avatarUri))
            ivAvatar.clearColorFilter()
            ivAvatar.background = null
        }
    }

    private fun loadQuickStats(view: View) {
        val workoutPrefs = requireContext().getSharedPreferences("workout_history", AppCompatActivity.MODE_PRIVATE)
        val historyCount = workoutPrefs.getInt("history_count", 0)

        view.findViewById<TextView>(R.id.tvTotalWorkouts).text = historyCount.toString()

        var totalReps = 0
        for (i in 0 until historyCount) {
            totalReps += workoutPrefs.getInt("reps_$i", 0)
        }
        view.findViewById<TextView>(R.id.tvTotalReps).text = totalReps.toString()

        val avgScore = if (historyCount > 0) 78 else 0
        view.findViewById<TextView>(R.id.tvAvgFormScore).text = "$avgScore%"
    }

    private fun loadFitnessProfile(view: View) {
        val prefs = requireContext().getSharedPreferences("user_profile", AppCompatActivity.MODE_PRIVATE)

        view.findViewById<TextView>(R.id.tvAge).text = prefs.getInt("age", 25).toString()
        view.findViewById<TextView>(R.id.tvGender).text = prefs.getString("gender", "Male") ?: "Male"
        view.findViewById<TextView>(R.id.tvWeight).text = "${prefs.getFloat("weight", 75f).toInt()} kg"
        view.findViewById<TextView>(R.id.tvHeight).text = "${prefs.getFloat("height", 180f).toInt()} cm"
        view.findViewById<TextView>(R.id.tvTrainingFrequency).text =
            prefs.getString("training_frequency", "3-4 times per week") ?: "3-4 times per week"
        view.findViewById<TextView>(R.id.tvWorkoutDuration).text =
            prefs.getString("workout_duration", "30-60 min") ?: "30-60 min"
        view.findViewById<TextView>(R.id.tvLimitations).text =
            prefs.getString("injuries", "No Limitations") ?: "No Limitations"
    }

    private fun loadAchievements(view: View) {
        val llAchievements = view.findViewById<LinearLayout>(R.id.llAchievements)
        llAchievements.removeAllViews()

        val workoutPrefs = requireContext().getSharedPreferences("workout_history", AppCompatActivity.MODE_PRIVATE)
        val historyCount = workoutPrefs.getInt("history_count", 0)

        val achievements = mutableListOf<String>()
        if (historyCount >= 1) achievements.add("First workout completed")
        if (historyCount >= 5) achievements.add("5 workouts done")

        val streak = calculateStreak(workoutPrefs, historyCount)
        if (streak > 0) achievements.add("Consistency streak: $streak days")

        if (achievements.isEmpty()) {
            llAchievements.addView(createSecondaryText("Complete your first workout to unlock achievements!"))
        } else {
            achievements.forEach { text ->
                llAchievements.addView(createPrimaryListText("✓ $text"))
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
            if (date > 0) dates.add(date)
        }

        dates.sortDescending()

        var streak = 0
        var currentDate = today
        val oneDay = 24 * 60 * 60 * 1000L

        dates.forEach { date ->
            val normalized = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (normalized == currentDate || normalized == currentDate - oneDay) {
                if (normalized == currentDate - oneDay) currentDate = normalized
                streak++
            } else {
                return streak
            }
        }

        return streak
    }

    private fun loadTrainingInsights(view: View) {
        val workoutPrefs = requireContext().getSharedPreferences("workout_history", AppCompatActivity.MODE_PRIVATE)
        val historyCount = workoutPrefs.getInt("history_count", 0)

        view.findViewById<TextView>(R.id.tvCommonMistake).text =
            if (historyCount > 0) "Knees moving inward" else "No data yet"

        val exerciseCounts = mutableMapOf<String, Int>()
        for (i in 0 until historyCount) {
            val exercise = workoutPrefs.getString("exercise_$i", null)
            if (exercise != null) {
                exerciseCounts[exercise] = exerciseCounts.getOrDefault(exercise, 0) + 1
            }
        }

        view.findViewById<TextView>(R.id.tvBestExercise).text =
            exerciseCounts.maxByOrNull { it.value }?.key ?: "Squat"

        view.findViewById<TextView>(R.id.tvAIAccuracy).text = "92%"
    }

    private fun loadRecentActivity(view: View) {
        val llRecentWorkouts = view.findViewById<LinearLayout>(R.id.llRecentWorkouts)
        llRecentWorkouts.removeAllViews()

        val workoutPrefs = requireContext().getSharedPreferences("workout_history", AppCompatActivity.MODE_PRIVATE)
        val historyCount = workoutPrefs.getInt("history_count", 0)

        if (historyCount == 0) {
            llRecentWorkouts.addView(createSecondaryText("No workouts yet"))
            return
        }

        val workouts = mutableListOf<Triple<String, Int, Long>>()
        for (i in 0 until historyCount) {
            val exercise = workoutPrefs.getString("exercise_$i", null)
            val reps = workoutPrefs.getInt("reps_$i", 0)
            val date = workoutPrefs.getLong("date_$i", 0)
            if (exercise != null && date > 0) workouts.add(Triple(exercise, reps, date))
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

            llRecentWorkouts.addView(createPrimaryListText("$exercise · $reps reps · $dateText"))
        }

        view.findViewById<MaterialButton>(R.id.btnViewFullHistory).setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }
    }

    private fun setupSettings(view: View) {
        val prefs = requireContext().getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val currentTheme = prefs.getString("theme_mode", "system") ?: "system"

        view.findViewById<TextView>(R.id.tvTheme).text = when (currentTheme) {
            "light" -> "Light"
            "dark" -> "Dark"
            else -> "System"
        }

        view.findViewById<TextView>(R.id.tvUnits).text =
            prefs.getString("units", "kg / cm") ?: "kg / cm"

        view.findViewById<View>(R.id.llTheme).setOnClickListener {
            showThemeDialog()
        }

        view.findViewById<View>(R.id.llUnits).setOnClickListener {
            showUnitsDialog()
        }

        view.findViewById<View>(R.id.llLogout).setOnClickListener {
            requireActivity()
                .getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun showThemeDialog() {
        val items = arrayOf("System", "Light", "Dark")
        val values = arrayOf("system", "light", "dark")
        val prefs = requireContext().getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val current = prefs.getString("theme_mode", "system") ?: "system"
        val checked = values.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(items, checked) { dialog, which ->
                val value = values[which]
                prefs.edit().putString("theme_mode", value).apply()

                when (value) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }

                dialog.dismiss()
            }
            .show()
    }

    private fun showUnitsDialog() {
        val items = arrayOf("kg / cm", "lb / ft")
        val prefs = requireContext().getSharedPreferences("app_settings", AppCompatActivity.MODE_PRIVATE)
        val current = prefs.getString("units", "kg / cm") ?: "kg / cm"
        val checked = items.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Units")
            .setSingleChoiceItems(items, checked) { dialog, which ->
                prefs.edit().putString("units", items[which]).apply()
                view?.findViewById<TextView>(R.id.tvUnits)?.text = items[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun showEditProfileDialog() {
        val prefs = requireContext().getSharedPreferences("user_profile", AppCompatActivity.MODE_PRIVATE)

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val etName = EditText(requireContext()).apply {
            hint = "Name"
            setText(
                requireContext()
                    .getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
                    .getString("user_name", "Azamat") ?: "Azamat"
            )
        }

        val etAge = EditText(requireContext()).apply {
            hint = "Age"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("age", 25).toString())
        }

        val etWeight = EditText(requireContext()).apply {
            hint = "Weight (kg)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(prefs.getFloat("weight", 75f).toString())
        }

        val etHeight = EditText(requireContext()).apply {
            hint = "Height (cm)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getFloat("height", 180f).toInt().toString())
        }

        container.addView(etName)
        container.addView(etAge)
        container.addView(etWeight)
        container.addView(etHeight)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Personal Data")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim().ifBlank { "Azamat" }
                val age = etAge.text.toString().toIntOrNull() ?: 25
                val weight = etWeight.text.toString().toFloatOrNull() ?: 75f
                val height = etHeight.text.toString().toFloatOrNull() ?: 180f

                requireContext().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
                    .edit()
                    .putString("user_name", name)
                    .apply()

                prefs.edit()
                    .putInt("age", age)
                    .putFloat("weight", weight)
                    .putFloat("height", height)
                    .apply()

                view?.let {
                    loadUserProfile(it)
                    loadFitnessProfile(it)
                }
            }
            .show()
    }

    private fun createPrimaryListText(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
    }

    private fun createSecondaryText(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }
    }
}