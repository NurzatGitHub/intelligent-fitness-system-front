package com.example.fitnesscoachai.ui.exercise

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.workout.WorkoutActivity
import com.google.android.material.card.MaterialCardView

class ExerciseSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_select)

        val cardSquat = findViewById<MaterialCardView>(R.id.cardSquat)
        val cardPushUp = findViewById<MaterialCardView>(R.id.cardPushUp)
        val cardPlank = findViewById<MaterialCardView>(R.id.cardPlank)

        cardSquat.setOnClickListener {
            startWorkout("Squat")
        }

        cardPushUp.setOnClickListener {
            startWorkout("Push-up")
        }

        cardPlank.setOnClickListener {
            startWorkout("Plank")
        }
    }

    private fun startWorkout(exerciseName: String) {
        val intent = Intent(this, WorkoutActivity::class.java)
        intent.putExtra("exercise_name", exerciseName)
        startActivity(intent)
    }
}
