package com.example.fitnesscoachai.ui.exercise

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.repo.ExerciseRepositoryLocal
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.ui.home.ExerciseAdapter
import com.example.fitnesscoachai.ui.workout.WorkoutActivity
import kotlinx.coroutines.launch

class ExerciseSelectActivity : AppCompatActivity() {

    private var allExercises: List<com.example.fitnesscoachai.domain.model.Exercise> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_select)

        val rv = findViewById<RecyclerView>(R.id.rvExerciseSelect)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        val adapter = ExerciseAdapter(emptyList()) { exercise ->
            startWorkout(exercise.titleEn)
        }

        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)

        val repo = ExerciseRepositoryLocal()
        lifecycleScope.launch {
            val all = repo.getExercisesByMainCategory(MainCategory.BACK) +
                    repo.getExercisesByMainCategory(MainCategory.CHEST) +
                    repo.getExercisesByMainCategory(MainCategory.LEGS) +
                    repo.getExercisesByMainCategory(MainCategory.ARMS) +
                    repo.getExercisesByMainCategory(MainCategory.ABS) +
                    repo.getExercisesByMainCategory(MainCategory.CARDIO)

            allExercises = all
            adapter.updateData(allExercises)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().trim()
                val filtered = if (query.isEmpty()) {
                    allExercises
                } else {
                    allExercises.filter {
                        it.titleEn.contains(query, ignoreCase = true)
                    }
                }
                adapter.updateData(filtered)
            }
        })
    }

    private fun startWorkout(exerciseName: String) {

        val code = when (exerciseName.trim().lowercase()) {
            "push-up", "push up", "pushup" -> "push_up"
            "squat" -> "squat"
            else -> "push_up" // пока по умолчанию, чтобы не ломалось
        }

        val intent = Intent(this, WorkoutActivity::class.java)
        intent.putExtra("exercise_name", exerciseName)
        intent.putExtra("exercise_code", code) // ✅ NEW
        startActivity(intent)
    }
}