package com.example.firstproject.ui.common

import androidx.annotation.DrawableRes
import com.example.firstproject.R
import java.util.Locale

/**
 * The single place that maps a habit category name to its glossy orb.
 * Categories themselves come from the backend; any name not listed here (a new or renamed
 * category, or null) falls back to the "Other" orb, so an icon is always shown.
 */
object CategoryIcons {

    private val orbsByName = mapOf(
        "exercise" to R.drawable.ic_orb_category_exercise,
        "reading" to R.drawable.ic_orb_category_reading,
        "study" to R.drawable.ic_orb_category_study,
        "hydration" to R.drawable.ic_orb_category_hydration,
        "writing" to R.drawable.ic_orb_category_writing,
        "running" to R.drawable.ic_orb_category_running,
        "other" to R.drawable.ic_orb_category_other
    )

    @DrawableRes
    fun orbFor(categoryName: String?): Int =
        categoryName?.trim()?.lowercase(Locale.ROOT)?.let(orbsByName::get)
            ?: R.drawable.ic_orb_category_other
}
