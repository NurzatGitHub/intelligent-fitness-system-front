package com.example.fitnesscoachai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.ui.welcome.WelcomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignUpStep2Activity : AppCompatActivity() {

    private lateinit var etAge: TextInputEditText
    private lateinit var etWeight: TextInputEditText
    private lateinit var etHeight: TextInputEditText
    private lateinit var rgFitnessLevel: RadioGroup
    private lateinit var rgTrainingGoal: RadioGroup
    private lateinit var rgInjuries: RadioGroup
    private lateinit var rgTrainingFrequency: RadioGroup
    private lateinit var btnCreateProfile: MaterialButton

    private var email: String = ""
    private var password: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step2)

        email = intent.getStringExtra("email") ?: ""
        password = intent.getStringExtra("password") ?: ""

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Missing account information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        etAge = findViewById(R.id.etAge)
        etWeight = findViewById(R.id.etWeight)
        etHeight = findViewById(R.id.etHeight)
        rgFitnessLevel = findViewById(R.id.rgFitnessLevel)
        rgTrainingGoal = findViewById(R.id.rgTrainingGoal)
        rgInjuries = findViewById(R.id.rgInjuries)
        rgTrainingFrequency = findViewById(R.id.rgTrainingFrequency)
        btnCreateProfile = findViewById(R.id.btnCreateProfile)
    }

    private fun setupListeners() {
        btnCreateProfile.setOnClickListener {
            validateAndCreateProfile()
        }
    }

    private fun validateAndCreateProfile() {
        val age = etAge.text.toString().trim()
        val weight = etWeight.text.toString().trim()
        val height = etHeight.text.toString().trim()

        when {
            age.isEmpty() -> {
                etAge.error = "Age is required"
                return
            }
            age.toIntOrNull() == null || age.toInt() < 1 || age.toInt() > 120 -> {
                etAge.error = "Please enter a valid age"
                return
            }
            weight.isEmpty() -> {
                etWeight.error = "Weight is required"
                return
            }
            weight.toDoubleOrNull() == null || weight.toDouble() <= 0 -> {
                etWeight.error = "Please enter a valid weight"
                return
            }
            height.isEmpty() -> {
                etHeight.error = "Height is required"
                return
            }
            height.toDoubleOrNull() == null || height.toDouble() <= 0 -> {
                etHeight.error = "Please enter a valid height"
                return
            }
            rgFitnessLevel.checkedRadioButtonId == -1 -> {
                Toast.makeText(this, "Please select your fitness level", Toast.LENGTH_SHORT).show()
                return
            }
            rgTrainingGoal.checkedRadioButtonId == -1 -> {
                Toast.makeText(this, "Please select your training goal", Toast.LENGTH_SHORT).show()
                return
            }
            rgTrainingFrequency.checkedRadioButtonId == -1 -> {
                Toast.makeText(this, "Please select your training frequency", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Get selected values
        val fitnessLevel = getSelectedRadioButtonText(rgFitnessLevel)
        val trainingGoal = getSelectedRadioButtonText(rgTrainingGoal)
        val injuries = if (rgInjuries.checkedRadioButtonId != -1) {
            getSelectedRadioButtonText(rgInjuries)
        } else {
            "No Limitations"
        }
        val trainingFrequency = getSelectedRadioButtonText(rgTrainingFrequency)

        // TODO: Save profile data to backend
        // For now, we'll just save locally and proceed to welcome screen
        saveProfileData(
            age = age.toInt(),
            weight = weight.toDouble(),
            height = height.toDouble(),
            fitnessLevel = fitnessLevel,
            trainingGoal = trainingGoal,
            injuries = injuries,
            trainingFrequency = trainingFrequency
        )

        // Navigate to welcome screen
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getSelectedRadioButtonText(radioGroup: RadioGroup): String {
        val selectedId = radioGroup.checkedRadioButtonId
        if (selectedId != -1) {
            val radioButton = findViewById<RadioButton>(selectedId)
            return radioButton.text.toString()
        }
        return ""
    }

    private fun saveProfileData(
        age: Int,
        weight: Double,
        height: Double,
        fitnessLevel: String,
        trainingGoal: String,
        injuries: String,
        trainingFrequency: String
    ) {
        // Save to SharedPreferences (temporary solution)
        val prefs = getSharedPreferences("user_profile", MODE_PRIVATE)
        prefs.edit()
            .putInt("age", age)
            .putFloat("weight", weight.toFloat())
            .putFloat("height", height.toFloat())
            .putString("fitness_level", fitnessLevel)
            .putString("training_goal", trainingGoal)
            .putString("injuries", injuries)
            .putString("training_frequency", trainingFrequency)
            .apply()
    }
}
