package com.example.fitnesscoachai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.fitnesscoachai.ui.assistant.AssistantFragment
import com.example.fitnesscoachai.ui.auth.AuthActivity
import com.example.fitnesscoachai.ui.exercise.ExerciseSelectFragment
import com.example.fitnesscoachai.ui.home.HomeFragment
import com.example.fitnesscoachai.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val exerciseFragment = ExerciseSelectFragment()
    private val assistantFragment = AssistantFragment()
    private val profileFragment = ProfileFragment()

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLoggedIn = getSharedPreferences("auth", MODE_PRIVATE)
            .getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot)) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, profileFragment, "profile")
                .hide(profileFragment)
                .add(R.id.container, assistantFragment, "assistant")
                .hide(assistantFragment)
                .add(R.id.container, exerciseFragment, "exercise")
                .hide(exerciseFragment)
                .add(R.id.container, homeFragment, "home")
                .commit()

            activeFragment = homeFragment
            bottomNavigation.selectedItemId = R.id.nav_home
        } else {
            activeFragment = supportFragmentManager.findFragmentByTag("home")
                ?: homeFragment
        }

        bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> switchTo(homeFragment)
                R.id.nav_camera -> switchTo(exerciseFragment)
                R.id.nav_assistant -> switchTo(assistantFragment)
                R.id.nav_profile -> switchTo(profileFragment)
            }
            true
        }
    }

    private fun switchTo(target: Fragment) {
        if (target == activeFragment) return

        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()

        activeFragment = target
    }
}