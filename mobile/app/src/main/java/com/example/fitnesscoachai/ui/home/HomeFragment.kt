package com.example.fitnesscoachai.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.ui.exercise.ExerciseListActivity

class HomeFragment : Fragment() {

    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Имя пользователя из SharedPreferences
        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "beginner") ?: "beginner"
        view.findViewById<TextView>(R.id.tvUserName)?.text = "$userName 👋"

        // 2. Категории из domain (CatalogTree / MainCategory)
        setupCategoryRecyclerView(view)
        categoryAdapter.setCategories(MainCategory.entries)

        // 3. Логика прогресса и статистики
        loadWeeklyProgress(view)
        loadOverallStatus(view)
    }

    private fun setupCategoryRecyclerView(view: View) {
        val rvCategories = view.findViewById<RecyclerView>(R.id.rvCategories)
        categoryAdapter = CategoryAdapter(emptyList()) { main ->
            startActivity(ExerciseListActivity.newIntent(requireContext(), main.id))
        }
        rvCategories.adapter = categoryAdapter
        rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun loadWeeklyProgress(view: View) {
        val prefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = prefs.getInt("history_count", 0)
        val progressBar = view.findViewById<ProgressBar>(R.id.pbWeekly)

        val goal = 5
        val progressPercent = (historyCount * 100 / goal).coerceAtMost(100)
        progressBar?.progress = progressPercent
    }

    private fun loadOverallStatus(view: View) {
        val prefs = requireContext().getSharedPreferences("workout_history", android.content.Context.MODE_PRIVATE)
        val historyCount = prefs.getInt("history_count", 0)

        view.findViewById<TextView>(R.id.tvTotalWorkouts)?.text = historyCount.toString()

        val score = if (historyCount > 0) 78 else 0
        view.findViewById<TextView>(R.id.tvAverageFormScore)?.text = "$score%"
    }
}
