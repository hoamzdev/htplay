package com.htdev.movie.htplay.widget

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.htdev.movie.htplay.R
import kotlin.math.roundToInt

/** A row of dots. Positions start at 1; positionSelected(1) selects the first dot. */
class DotIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private var dotCount = 4
    private var dotSize = dp(8)
    private var dotSpacing = dp(8)
    private var selectedColor = ContextCompat.getColor(context, R.color.primary_color)
    private var unselectedColor = ContextCompat.getColor(context, R.color.gray_color)

    var selectedPosition: Int = 1
        private set

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        val attributes = context.obtainStyledAttributes(
            attrs, R.styleable.DotIndicatorView, defStyleAttr, 0,
        )
        try {
            dotCount = attributes.getInt(R.styleable.DotIndicatorView_dotCount, dotCount)
                .coerceAtLeast(1)
            dotSize = attributes.getDimensionPixelSize(
                R.styleable.DotIndicatorView_dotSize, dotSize,
            ).coerceAtLeast(1)
            dotSpacing = attributes.getDimensionPixelSize(
                R.styleable.DotIndicatorView_dotSpacing, dotSpacing,
            ).coerceAtLeast(0)
            selectedColor = attributes.getColor(
                R.styleable.DotIndicatorView_dotSelectedColor, selectedColor,
            )
            unselectedColor = attributes.getColor(
                R.styleable.DotIndicatorView_dotUnselectedColor, unselectedColor,
            )
        } finally {
            attributes.recycle()
        }
        rebuildDots()
    }

    fun setDotCount(count: Int) {
        require(count > 0) { "Dot count must be greater than 0" }
        dotCount = count
        selectedPosition = selectedPosition.coerceAtMost(count)
        rebuildDots()
    }

    /** Selects a dot using a 1-based position. */
    fun positionSelected(position: Int) {
        require(position in 1..dotCount) { "Position must be in 1..$dotCount" }
        selectedPosition = position
        updateColors()
    }

    fun setPositionSelected(position: Int) = positionSelected(position)

    private fun rebuildDots() {
        removeAllViews()
        repeat(dotCount) { index ->
            val dot = View(context).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            }
            addView(dot, LayoutParams(dotSize, dotSize).apply {
                marginEnd = if (index < dotCount - 1) dotSpacing else 0
            })
        }
        updateColors()
    }

    private fun updateColors() {
        repeat(childCount) { index ->
            val dot = getChildAt(index)
            dot.isSelected = index + 1 == selectedPosition
            (dot.background as GradientDrawable).setColor(
                if (dot.isSelected) selectedColor else unselectedColor,
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}
