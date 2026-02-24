package com.example.fitnesscoachai.ui.auth.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitnesscoachai.MainActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.api.RetrofitClient
import com.example.fitnesscoachai.data.models.RegisterRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch

class SignUpFrequencyActivity : AppCompatActivity() {

    private lateinit var btnNext: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var chip2to3: Chip
    private lateinit var chip3to4: Chip
    private lateinit var chip5to6: Chip
    private lateinit var chipEveryday: Chip
    private lateinit var loadingIndicator: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_frequency)

        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(100, true)

        chip2to3 = findViewById(R.id.chip2to3)
        chip3to4 = findViewById(R.id.chip3to4)
        chip5to6 = findViewById(R.id.chip5to6)
        chipEveryday = findViewById(R.id.chipEveryday)

        chip3to4.isChecked = true

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            val frequency = when {
                chip2to3.isChecked -> "2-3"
                chip3to4.isChecked -> "3-4"
                chip5to6.isChecked -> "5-6"
                else -> "everyday"
            }

            // Собираем все данные онбординга
            val email       = intent.getStringExtra("email").orEmpty()
            val password    = intent.getStringExtra("password").orEmpty()
            val age         = intent.getIntExtra("age", -1).takeIf { it != -1 }
            val heightCm    = intent.getIntExtra("height_cm", -1).takeIf { it != -1 }?.toFloat()
            val weightKg    = intent.getFloatExtra("weight_kg", -1f).takeIf { it != -1f }
            val fitnessLevel = intent.getStringExtra("fitness_level").orEmpty().ifEmpty { "beginner" }
            val goal        = intent.getStringExtra("goal").orEmpty()
            val limitations = intent.getStringExtra("limitations").orEmpty()

            // ✅ Отправляем на сервер
            registerUser(
                email, password, age, heightCm, weightKg,
                fitnessLevel, goal, limitations, frequency
            )
        }
    }

    private fun registerUser(
        email: String,
        password: String,
        age: Int?,
        height: Float?,
        weight: Float?,
        fitnessLevel: String,
        goal: String,
        limitations: String,
        frequency: String,
    ) {
        setLoading(true)

        val request = RegisterRequest(
            email = email,
            password = password,
            age = age,
            height = height,
            weight = weight,
            fitness_level = fitnessLevel,
            goal = goal,
            limitations = limitations,
            frequency = frequency,
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.register(request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // ✅ Сохраняем только токен — НЕ пароль
                    getSharedPreferences("auth", MODE_PRIVATE).edit()
                        .putBoolean("isLoggedIn", true)
                        .putBoolean("isGuest", false)
                        .putString("access_token", body.access)
                        .putString("refresh_token", body.refresh)
                        .putString("user_email", body.user.email)
                        .apply()

                    // Идём на главный экран
                    startActivity(
                        Intent(this@SignUpFrequencyActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(this@SignUpFrequencyActivity, errorMsg, Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SignUpFrequencyActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        loadingIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        btnNext.isEnabled = !loading
        btnBack.isEnabled = !loading
    }
}