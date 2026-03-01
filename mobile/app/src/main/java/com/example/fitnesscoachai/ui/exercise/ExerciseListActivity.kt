package com.example.fitnesscoachai.ui.exercise

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.repo.ExerciseRepositoryLocal
import com.example.fitnesscoachai.domain.usecase.GetExercisesBySub
import com.example.fitnesscoachai.ui.home.ExerciseAdapter
import kotlinx.coroutines.launch

class ExerciseListActivity : AppCompatActivity() {

    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val subId = intent.getStringExtra(EXTRA_SUBCATEGORY_ID)
        if (subId == null) {
            finish()
            return
        }

        supportActionBar?.title = getString(R.string.exercise_list_title)

        val rvExercises = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvExercises)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)

        val repo = ExerciseRepositoryLocal()
        val getExercisesBySub = GetExercisesBySub(repo)

        exerciseAdapter = ExerciseAdapter(emptyList()) { exercise ->
            startActivity(ExerciseInstructionActivity.newIntent(this, exercise.id))
        }
        rvExercises.adapter = exerciseAdapter
        rvExercises.layoutManager = LinearLayoutManager(this)

        pbLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            val exercises = getExercisesBySub(subId)
            exerciseAdapter.updateData(exercises)
            pbLoading.visibility = View.GONE
            tvEmpty.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    companion object {
        const val EXTRA_SUBCATEGORY_ID = "extra_subcategory_id"

        fun newIntent(context: Context, subCategoryId: String): Intent {
            return Intent(context, ExerciseListActivity::class.java).apply {
                putExtra(EXTRA_SUBCATEGORY_ID, subCategoryId)
            }
        }
    }
}
