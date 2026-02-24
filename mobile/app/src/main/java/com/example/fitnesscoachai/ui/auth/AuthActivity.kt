package com.example.fitnesscoachai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fitnesscoachai.MainActivity
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.models.AuthResponse
import com.example.fitnesscoachai.ui.auth.onboarding.SignUpAgeActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    private lateinit var btnLogin: MaterialButton
    private lateinit var btnContinueAsGuest: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var tvCreateAccount: TextView
    private lateinit var tvForgotPassword: TextView

    private val viewModel: AuthViewModel by viewModels()

    // Google Sign-In launcher
    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            val account = task.result
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(this, "Google token not found", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            viewModel.loginWithGoogle(idToken)
        }.onFailure {
            Toast.makeText(this, "Google sign-in failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        btnLogin = findViewById(R.id.btnLogin)
        btnContinueAsGuest = findViewById(R.id.btnContinueAsGuest)
        btnGoogle = findViewById(R.id.btnGoogle) // ⚠️ убедись, что такой id есть в xml
        tvCreateAccount = findViewById(R.id.tvCreateAccount)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        btnLogin.isEnabled = false

        setupValidation()
        observeState()

        btnLogin.setOnClickListener { submitLogin() }

        btnGoogle.setOnClickListener { startGoogleSignIn() }

        btnContinueAsGuest.setOnClickListener {
            getSharedPreferences("auth", MODE_PRIVATE).edit()
                .putBoolean("isLoggedIn", true)
                .putBoolean("isGuest", true)
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignUpStep1Activity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot password: coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            // важно: берём web client id из strings.xml
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        client.signOut() // чтобы всегда показывало аккаунт-выбор
        googleLauncher.launch(client.signInIntent)
    }

    private fun setupValidation() {
        val watcher = SimpleTextWatcher {
            tilEmail.error = null
            tilPassword.error = null
            btnLogin.isEnabled = isFormValid()
        }
        etEmail.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)
    }

    private fun isFormValid(): Boolean {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString()?.trim().orEmpty()
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && pass.isNotBlank()
    }

    private fun submitLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString()?.trim().orEmpty()

        var ok = true
        tilEmail.error = null
        tilPassword.error = null

        if (email.isBlank()) {
            tilEmail.error = "Email is required"
            ok = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            ok = false
        }

        if (pass.isBlank()) {
            tilPassword.error = "Password is required"
            ok = false
        }

        if (!ok) return
        viewModel.login(email, pass)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is AuthViewModel.LoginState.Idle -> {
                            btnLogin.isEnabled = isFormValid()
                            btnLogin.text = "Log In"
                        }

                        is AuthViewModel.LoginState.Loading -> {
                            btnLogin.isEnabled = false
                            btnGoogle.isEnabled = false
                            btnLogin.text = "Loading..."
                        }

                        is AuthViewModel.LoginState.Success -> {
                            btnGoogle.isEnabled = true
                            btnLogin.text = "Log In"

                            val auth = state.authResponse
                            saveAuthData(auth)

                            // ✅ ВОТ ТУТ ЛОГИКА: новый -> onboarding, старый -> home
                            if (auth.is_new_user) {
                                startActivity(Intent(this@AuthActivity, SignUpAgeActivity::class.java).apply {
                                    putExtra("email", auth.user.email)
                                    putExtra("from_google", true)
                                })
                            } else {
                                startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                finish()
                            }
                        }

                        is AuthViewModel.LoginState.Error -> {
                            btnGoogle.isEnabled = true
                            btnLogin.text = "Log In"
                            btnLogin.isEnabled = isFormValid()
                            Toast.makeText(this@AuthActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun saveAuthData(authResponse: AuthResponse) {
        getSharedPreferences("auth", MODE_PRIVATE).edit()
            .putString("access_token", authResponse.access)
            .putString("refresh_token", authResponse.refresh)
            .putBoolean("isLoggedIn", true)
            .putBoolean("isGuest", false)
            .putString("user_name", authResponse.user.username)
            .putString("user_email", authResponse.user.email)
            .apply()
    }
}