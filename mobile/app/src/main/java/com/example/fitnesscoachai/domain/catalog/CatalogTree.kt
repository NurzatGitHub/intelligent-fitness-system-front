package com.example.fitnesscoachai.domain.catalog

import com.example.fitnesscoachai.domain.model.MainCategory
import com.example.fitnesscoachai.domain.model.SubCategory

/**
 * Единый источник правды: дерево Main → Sub.
 * UI строится из этой структуры. Конфиг приложения — легко расширять.
 */
object CatalogTree {

    val subCategories: List<SubCategory> = listOf(
        // ARMS
        SubCategory("biceps", "Biceps", MainCategory.ARMS),
        SubCategory("triceps", "Triceps", MainCategory.ARMS),
        SubCategory("forearms", "Forearms", MainCategory.ARMS),

        // LEGS
        SubCategory("quads", "Quads", MainCategory.LEGS),
        SubCategory("hamstrings", "Hamstrings", MainCategory.LEGS),
        SubCategory("glutes", "Glutes", MainCategory.LEGS),
        SubCategory("calves", "Calves", MainCategory.LEGS),

        // BACK
        SubCategory("lats", "Lats", MainCategory.BACK),
        SubCategory("upper_back", "Upper Back", MainCategory.BACK),
        SubCategory("lower_back", "Lower Back", MainCategory.BACK),

        // CHEST
        SubCategory("pectorals", "Pectorals", MainCategory.CHEST),

        // ABS
        SubCategory("abs_core", "Core", MainCategory.ABS),

        // CARDIO
        SubCategory("hiit", "HIIT", MainCategory.CARDIO),
        SubCategory("endurance", "Endurance", MainCategory.CARDIO)
    )

    fun subFor(main: MainCategory): List<SubCategory> =
        subCategories.filter { it.main == main }

    fun subById(subId: String): SubCategory? =
        subCategories.find { it.id == subId }
}
