package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator

class SignUpFrequencyActivity : AppCompatActivity() {

    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton

    private lateinit var chip2to3: Chip
    private lateinit var chip3to4: Chip
    private lateinit var chip5to6: Chip
    private lateinit var chipEveryday: Chip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_frequency)

        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(100, true) // step 8/8

        chip2to3 = findViewById(R.id.chip2to3)
        chip3to4 = findViewById(R.id.chip3to4)
        chip5to6 = findViewById(R.id.chip5to6)
        chipEveryday = findViewById(R.id.chipEveryday)

        // default selection
        chip3to4.isChecked = true

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            val frequency = when {
                chip2to3.isChecked -> "2-3"
                chip3to4.isChecked -> "3-4"
                chip5to6.isChecked -> "5-6"
                else -> "everyday"
            }

            // собираем все данные
            val email = intent.getStringExtra("email").orEmpty()
            val password = intent.getStringExtra("password").orEmpty() // можно НЕ хранить, но пока ок для демо
            val age = intent.getIntExtra("age", -1)
            val height = intent.getIntExtra("height_cm", -1)
            val weight = intent.getFloatExtra("weight_kg", -1f)
            val fitnessLevel = intent.getStringExtra("fitness_level").orEmpty()
            val goal = intent.getStringExtra("goal").orEmpty()
            val limitations = intent.getStringExtra("limitations").orEmpty()

            // сохраняем
            val sp = getSharedPreferences("auth", MODE_PRIVATE)
            sp.edit()
                .putBoolean("isLoggedIn", true)
                .putBoolean("isGuest", false)
                .putBoolean("onboardingCompleted", true)
                .putString("email", email)
                .putString("password_demo", password) // лучше убрать позже
                .putInt("age", age)
                .putInt("height_cm", height)
                .putFloat("weight_kg", weight)
                .putString("fitness_level", fitnessLevel)
                .putString("goal", goal)
                .putString("limitations", limitations)
                .putString("frequency", frequency)
                .apply()

            // сразу home и чистим стек
            val i = Intent(this, com.example.fitnesscoachai.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(i)
        }

    }
}
