package com.troxzy.trxchess.ui.analysis

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.troxzy.trxchess.engine.api.Evaluation
import com.troxzy.trxchess.ui.designsystem.AnimationCategory
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Vertical evaluation bar.
 *
 * White advantage grows upward, black downward. The boundary animates only
 * when the value changes meaningfully (threshold in [SIGNIFICANT_CP]), so
 * engine updates do not cause continuous jitter.
 */
class EvalBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var evaluation: Evaluation = Evaluation.Centipawn(0)
        set(value) {
            val newFraction = fraction(value)
            if (kotlin.math.abs(newFraction - currentFraction) > SIGNIFICANT_FRACTION) {
                field = value
                animateTo(newFraction)
            } else {
                field = value
            }
        }

    private var currentFraction = 0.5f
    private var animator: ValueAnimator? = null

    private val whitePaint = Paint()
    private val blackPaint = Paint()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        whitePaint.color = Color.rgb(242, 244, 248)
        blackPaint.color = Color.rgb(24, 27, 32)
        linePaint.color = Color.argb(140, 224, 48, 64)
        linePaint.strokeWidth = dp(2f)
        contentDescription = "Evaluation bar"
    }

    private fun animateTo(target: Float) {
        if (!designSystem.visualPolicy.motionEnabled) {
            currentFraction = target
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(currentFraction, target).apply {
            duration = designSystem.scaledDuration(AnimationCategory.STANDARD, 120L)
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                currentFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    fun fraction(ev: Evaluation): Float = when (ev) {
        is Evaluation.Mate -> if (ev.plies > 0) 1f else 0f
        is Evaluation.Centipawn -> {
            val e = ev.value / 800f
            (1f / (1f + exp(-e))).toFloat()
        }
        is Evaluation.LowerBound -> fraction(ev.score).coerceAtLeast(0.97f)
        is Evaluation.UpperBound -> fraction(ev.score).coerceAtMost(0.03f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // white (top) vs black (bottom)
        val boundaryY = h * (1f - currentFraction)
        blackPaint.color = Color.rgb(24, 27, 32)
        canvas.drawRect(0f, boundaryY, w, h, blackPaint)
        whitePaint.color = Color.rgb(242, 244, 248)
        canvas.drawRect(0f, 0f, w, boundaryY, whitePaint)

        // center indicator
        linePaint.color = Color.argb(120, 224, 48, 64)
        canvas.drawRect(0f, h / 2f - dp(1f), w, h / 2f + dp(1f), linePaint)

        // boundary line
        linePaint.color = Color.rgb(224, 48, 64)
        canvas.drawRect(0f, boundaryY - dp(1f), w, boundaryY + dp(1f), linePaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    companion object {
        private const val SIGNIFICANT_FRACTION = 0.015f
    }
}
