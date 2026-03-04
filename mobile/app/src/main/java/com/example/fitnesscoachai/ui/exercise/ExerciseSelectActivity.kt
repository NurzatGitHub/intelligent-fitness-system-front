package com.example.fitnesscoachai.ui.exercise

import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_select)

        val rv = findViewById<RecyclerView>(R.id.rvExerciseSelect)
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
            adapter.updateData(all)
        }
    }

    private fun startWorkout(exerciseName: String) {
        val intent = Intent(this, WorkoutActivity::class.java)
        intent.putExtra("exercise_name", exerciseName)
        startActivity(intent)
    }
}
