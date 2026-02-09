package com.example.fitnesscoachai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesscoachai.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText

class SignUpStep1Activity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var cbTerms: MaterialCheckBox
    private lateinit var btnNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step1)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        cbTerms = findViewById(R.id.cbTerms)
        btnNext = findViewById(R.id.btnNext)

        btnNext.setOnClickListener {
            validateAndProceed()
        }
    }

    private fun validateAndProceed() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        when {
            email.isEmpty() -> {
                etEmail.error = "Email is required"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Please enter a valid email"
                return
            }
            password.isEmpty() -> {
                etPassword.error = "Password is required"
                return
            }
            password.length < 8 -> {
                etPassword.error = "Password must be at least 8 characters"
                return
            }
            confirmPassword.isEmpty() -> {
                etConfirmPassword.error = "Please confirm your password"
                return
            }
            password != confirmPassword -> {
                etConfirmPassword.error = "Passwords do not match"
                return
            }
            !cbTerms.isChecked -> {
                Toast.makeText(this, "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Pass data to Step 2
        val intent = Intent(this, SignUpStep2Activity::class.java)
        intent.putExtra("email", email)
        intent.putExtra("password", password)
        startActivity(intent)
    }
}
