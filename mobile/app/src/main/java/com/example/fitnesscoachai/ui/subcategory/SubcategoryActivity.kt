package com.example.fitnesscoachai.ui.subcategory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnesscoachai.R
import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.domain.usecase.GetSubCategories
import com.example.fitnesscoachai.ui.exercise.ExerciseListActivity

class SubcategoryActivity : AppCompatActivity() {

    private lateinit var adapter: SubcategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subcategory)

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

        supportActionBar?.title = getString(R.string.subcategory_title)

        val getSubCategories = GetSubCategories()
        val subcategories = getSubCategories(main)

        val rvSubcategories = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSubcategories)
        adapter = SubcategoryAdapter(subcategories) { sub ->
            startActivity(ExerciseListActivity.newIntent(this, sub.id))
        }
        rvSubcategories.adapter = adapter
        rvSubcategories.layoutManager = LinearLayoutManager(this)
        adapter.setItems(subcategories)
    }

    companion object {
        const val EXTRA_MAIN_CATEGORY_ID = "extra_main_category_id"

        fun newIntent(context: Context, mainCategoryId: String): Intent {
            return Intent(context, SubcategoryActivity::class.java).apply {
                putExtra(EXTRA_MAIN_CATEGORY_ID, mainCategoryId)
            }
        }
    }
}
