package com.example.firstproject.ui.common

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.firstproject.R
import com.google.android.material.snackbar.Snackbar

/**
 * Shows a glossy Aero snackbar on the activity root, so it survives the fragment being replaced
 * (e.g. a welcome message right after navigating away from login). Sits above the bottom nav when visible.
 */
fun Fragment.showAeroSnackbar(message: CharSequence, isError: Boolean = false) {
    val activity = activity ?: return
    val root = activity.findViewById<View>(R.id.main) ?: return
    val snackbar = Snackbar.make(
        root,
        message,
        if (isError) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
    )
    activity.findViewById<View>(R.id.bottom_nav)
        ?.takeIf { it.isVisible }
        ?.let { snackbar.anchorView = it }
    if (isError) {
        snackbar.view.background = ContextCompat.getDrawable(activity, R.drawable.bg_snackbar_error)
    }
    snackbar.show()
}
