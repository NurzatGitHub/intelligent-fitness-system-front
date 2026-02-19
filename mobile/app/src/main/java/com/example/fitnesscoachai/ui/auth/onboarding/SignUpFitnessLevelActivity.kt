package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator

class SignUpFitnessLevelActivity : AppCompatActivity() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_fitness_level)

        chipGroup = findViewById(R.id.chipGroup)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(57, true) // step 5/8

        val email = intent.getStringExtra("email").orEmpty()
        val password = intent.getStringExtra("password").orEmpty()
        val age = intent.getIntExtra("age", -1)
        val height = intent.getIntExtra("height", -1)
        val weight = intent.getIntExtra("weight", -1)

        btnNext.isEnabled = false

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            btnNext.isEnabled = checkedIds.isNotEmpty()
        }

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            val selectedId = chipGroup.checkedChipId
            val level = findViewById<Chip>(selectedId).text.toString()

            // NEXT STEP: Training Goal
            val i = Intent(this, SignUpGoalActivity::class.java).apply {
                putExtra("email", email)
                putExtra("password", password)
                putExtra("age", age)
                putExtra("height", height)
                putExtra("weight", weight)
                putExtra("fitness_level", level)
            }
            startActivity(i)
        }
    }
}
