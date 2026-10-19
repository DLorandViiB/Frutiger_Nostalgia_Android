package com.example.firstproject.ui.common

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.firstproject.R

/**
 * Decorative layer of translucent bubbles that slowly bob up and down.
 * Positions are fixed fractions of the view size, so the layout is deterministic and cheap.
 */
class BubbleFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private class Bubble(
        val xFraction: Float,
        val yFraction: Float,
        val sizeDp: Int,
        val driftDp: Int,
        val durationMs: Long,
        val alpha: Float
    )

    private val bubbles = listOf(
        Bubble(0.12f, 0.18f, 46, 16, 7_000, 0.85f),
        Bubble(0.86f, 0.30f, 70, 22, 9_500, 0.75f),
        Bubble(0.70f, 0.08f, 28, 12, 6_000, 0.9f),
        Bubble(0.06f, 0.55f, 84, 24, 11_000, 0.6f),
        Bubble(0.92f, 0.62f, 36, 14, 8_000, 0.8f),
        Bubble(0.30f, 0.80f, 54, 18, 10_000, 0.7f),
        Bubble(0.62f, 0.72f, 22, 12, 6_500, 0.9f)
    )

    private val density = resources.displayMetrics.density
    private val animators = mutableListOf<ValueAnimator>()

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        bubbles.forEach { bubble ->
            val size = (bubble.sizeDp * density).toInt()
            addView(ImageView(context).apply {
                setImageResource(R.drawable.bubble)
                alpha = bubble.alpha
            }, LayoutParams(size, size))
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val w = right - left
        val h = bottom - top
        bubbles.forEachIndexed { i, bubble ->
            val child = getChildAt(i)
            val x = (bubble.xFraction * w - child.measuredWidth / 2f).toInt()
            val y = (bubble.yFraction * h - child.measuredHeight / 2f).toInt()
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bubbles.forEachIndexed { i, bubble ->
            val drift = bubble.driftDp * density
            animators += ObjectAnimator.ofFloat(getChildAt(i), View.TRANSLATION_Y, -drift, drift).apply {
                duration = bubble.durationMs
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                // Stagger the bubbles so they don't move in lockstep
                currentPlayTime = bubble.durationMs * i / bubbles.size
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        animators.forEach { it.cancel() }
        animators.clear()
        super.onDetachedFromWindow()
    }
}
