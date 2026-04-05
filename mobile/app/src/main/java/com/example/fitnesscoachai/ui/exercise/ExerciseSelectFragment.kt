package com.example.fitnesscoachai.ui.exercise

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.repo.ExerciseRepositoryLocal
import com.example.fitnesscoachai.domain.model.Exercise
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.ui.workout.pushup.PushupActivity
import com.example.fitnesscoachai.ui.workout.squat.SquatActivity
import kotlinx.coroutines.launch

class ExerciseSelectFragment : Fragment() {

    private var allExercises: List<Exercise> = emptyList()
    private var adapter: ExerciseSelectAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_exercise_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvExerciseSelect)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)

        adapter = ExerciseSelectAdapter(emptyList()) { exercise ->
            routeToWorkout(exercise)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val repo = ExerciseRepositoryLocal()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val all = repo.getExercisesByMainCategory(MainCategory.BACK) +
                        repo.getExercisesByMainCategory(MainCategory.CHEST) +
                        repo.getExercisesByMainCategory(MainCategory.LEGS) +
                        repo.getExercisesByMainCategory(MainCategory.ARMS) +
                        repo.getExercisesByMainCategory(MainCategory.ABS) +
                        repo.getExercisesByMainCategory(MainCategory.CARDIO)

                allExercises = all
                adapter?.submitList(allExercises)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().trim()
                val filtered = if (query.isEmpty()) {
                    allExercises
                } else {
                    allExercises.filter { it.titleEn.contains(query, ignoreCase = true) }
                }
                adapter?.submitList(filtered)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
    }

    private fun routeToWorkout(exercise: Exercise) {
        val ctx = requireContext()

        val intent = when (exercise.id) {
            "ex16" -> Intent(ctx, PushupActivity::class.java)
            "ex6" -> Intent(ctx, SquatActivity::class.java)
            else -> {
                val slug = exercise.titleEn
                    .lowercase()
                    .replace("(", "")
                    .replace(")", "")
                    .replace("’", "")
                    .replace("'", "")
                    .replace(" ", "-")
                ExerciseInstructionActivity.newIntent(ctx, slug)
            }
        }

        intent.putExtra("exercise_name", exercise.titleEn)
        startActivity(intent)
    }
}