package com.example.fitnesscoachai.ui.exercise

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.data.repo.ExerciseRepositoryLocal
import com.example.fitnesscoachai.domain.catalog.CatalogTree
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.domain.model.SubCategory
import com.example.fitnesscoachai.domain.usecase.GetExercisesBySub
import com.example.fitnesscoachai.ui.home.MainCategoryDisplay
import com.example.fitnesscoachai.ui.home.ExerciseAdapter
import kotlinx.coroutines.launch

class ExerciseListActivity : AppCompatActivity() {

    private lateinit var exerciseAdapter: ExerciseAdapter
    private lateinit var repo: ExerciseRepositoryLocal
    private lateinit var getExercisesBySub: GetExercisesBySub

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val mainId = intent.getStringExtra(EXTRA_MAIN_CATEGORY_ID)
        val main = MainCategory.fromId(mainId)
        if (main == null) {
            finish()
            return
        }

        supportActionBar?.title = getString(R.string.exercise_list_title)

        val chipGroup = findViewById<ChipGroup>(R.id.chipGroup)
        val rvExercises = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvExercises)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)

        repo = ExerciseRepositoryLocal()
        getExercisesBySub = GetExercisesBySub(repo)

        exerciseAdapter = ExerciseAdapter(emptyList()) { exercise ->
            startActivity(ExerciseInstructionActivity.newIntent(this, exercise.id))
        }
        rvExercises.adapter = exerciseAdapter
        rvExercises.layoutManager = LinearLayoutManager(this)

        setupChips(chipGroup, main, pbLoading, tvEmpty)
    }

    private fun setupChips(
        chipGroup: ChipGroup,
        main: MainCategory,
        pbLoading: ProgressBar,
        tvEmpty: TextView
    ) {
        val subcategories = CatalogTree.subFor(main)
        if (subcategories.isEmpty()) return

        val selectedColor = getColor(MainCategoryDisplay.colorRes(main))
        val unselectedColor = getColor(android.R.color.darker_gray)
        val textSelected = getColor(android.R.color.white)
        val textUnselected = getColor(android.R.color.black)

        val bgStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                selectedColor,
                unselectedColor
            )
        )

        val textStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                textSelected,
                textUnselected
            )
        )

        chipGroup.setOnCheckedStateChangeListener(null)
        chipGroup.removeAllViews()

        var firstChipId = -1
        subcategories.forEachIndexed { index, sub ->
            val chip = createChip(sub, bgStates, textStates)
            if (index == 0) {
                firstChipId = View.generateViewId()
                chip.id = firstChipId
            }
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { group, _ ->
            val checkedId = group.checkedChipId
            val chip = group.findViewById<Chip>(checkedId) ?: return@setOnCheckedStateChangeListener
            val sub = chip.tag as? SubCategory ?: return@setOnCheckedStateChangeListener
            loadExercisesForSub(sub.id, pbLoading, tvEmpty)
        }

        if (firstChipId != -1) {
            chipGroup.check(firstChipId)
        }
    }

    private fun createChip(
        sub: SubCategory,
        bgStates: ColorStateList,
        textStates: ColorStateList
    ): Chip {
        val chip = Chip(this)
        chip.text = sub.titleEn
        chip.isCheckable = true
        chip.isClickable = true
        chip.chipCornerRadius = dpToPx(24f)
        chip.chipBackgroundColor = bgStates
        chip.setTextColor(textStates)
        chip.tag = sub

        val params = ChipGroup.LayoutParams(
            ChipGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(36f).toInt()
        )
        params.setMargins(dpToPx(4f).toInt(), 0, dpToPx(4f).toInt(), 0)
        chip.layoutParams = params

        return chip
    }

    private fun loadExercisesForSub(
        subId: String,
        pbLoading: ProgressBar,
        tvEmpty: TextView
    ) {
        pbLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            val exercises = getExercisesBySub(subId)
            exerciseAdapter.updateData(exercises)
            pbLoading.visibility = View.GONE
            tvEmpty.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    companion object {
        const val EXTRA_MAIN_CATEGORY_ID = "extra_main_category_id"

        fun newIntent(context: Context, mainCategoryId: String): Intent {
            return Intent(context, ExerciseListActivity::class.java).apply {
                putExtra(EXTRA_MAIN_CATEGORY_ID, mainCategoryId)
            }
        }
    }
}
