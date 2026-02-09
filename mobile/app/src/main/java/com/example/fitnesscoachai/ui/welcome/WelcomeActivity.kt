package com.example.fitnesscoachai.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.MainActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {

    private lateinit var btnStartTraining: MaterialButton
    private lateinit var btnCompleteProfileLater: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btnStartTraining = findViewById(R.id.btnStartTraining)
        btnCompleteProfileLater = findViewById(R.id.btnCompleteProfileLater)

        // Mark user as logged in
        getSharedPreferences("auth", MODE_PRIVATE).edit()
            .putBoolean("isLoggedIn", true)
            .putBoolean("isGuest", false)
            .apply()

        btnStartTraining.setOnClickListener {
            // Navigate to main activity (which will show home/training selection)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnCompleteProfileLater.setOnClickListener {
            // Navigate to main activity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
