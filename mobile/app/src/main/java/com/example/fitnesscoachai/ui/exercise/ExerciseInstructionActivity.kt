package com.example.fitnesscoachai.ui.exercise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.repo.ExerciseRepositoryLocal
import kotlinx.coroutines.launch

class ExerciseInstructionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_instruction)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val exerciseId = intent.getStringExtra(EXTRA_EXERCISE_ID)
        if (exerciseId == null) {
            finish()
            return
        }

        val repo = ExerciseRepositoryLocal()
        val stepsContainer = findViewById<LinearLayout>(R.id.stepsContainer)
        val tipsContainer = findViewById<LinearLayout>(R.id.tipsContainer)

        lifecycleScope.launch {
            val exercise = repo.getExerciseById(exerciseId)
            if (exercise == null) {
                finish()
                return@launch
            }
            supportActionBar?.title = exercise.titleEn

            findViewById<TextView>(R.id.tvExerciseName).text = exercise.titleEn
            findViewById<TextView>(R.id.tvExerciseMeta).text = buildString {
                exercise.equipment?.let { append(it.replaceFirstChar { c -> c.uppercase() }) }
                exercise.difficulty?.let {
                    if (isNotEmpty()) append(" · ")
                    append(it)
                }
                if (isEmpty()) append("-")
            }
            findViewById<TextView>(R.id.tvDescription).text = exercise.description

            stepsContainer.removeAllViews()
            exercise.steps.forEachIndexed { index, step ->
                val view = LayoutInflater.from(this@ExerciseInstructionActivity)
                    .inflate(R.layout.item_step, stepsContainer, false)
                (view as TextView).text = "${index + 1}. $step"
                stepsContainer.addView(view)
            }

            tipsContainer.removeAllViews()
            exercise.tips.forEach { tip ->
                val view = LayoutInflater.from(this@ExerciseInstructionActivity)
                    .inflate(R.layout.item_step, tipsContainer, false)
                (view as TextView).text = "• $tip"
                tipsContainer.addView(view)
            }
        }
    }

    companion object {
        const val EXTRA_EXERCISE_ID = "extra_exercise_id"

        fun newIntent(context: Context, exerciseId: String): Intent {
            return Intent(context, ExerciseInstructionActivity::class.java).apply {
                putExtra(EXTRA_EXERCISE_ID, exerciseId)
            }
        }
    }
}
