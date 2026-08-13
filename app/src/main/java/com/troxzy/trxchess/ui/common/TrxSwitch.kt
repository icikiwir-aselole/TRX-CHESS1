package com.troxzy.trxchess.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.troxzy.trxchess.ui.designsystem.AnimationCategory
import com.troxzy.trxchess.ui.designsystem.DesignSystem

/**
 * Custom switch with animated thumb; fully accessible (acts as a checkable
 * view) and follows the visual policy.
 */
class TrxSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var checked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateContentDescription()
                animateThumb(if (value) 1f else 0f)
                invalidate()
            }
        }

    var onCheckedChange: ((Boolean) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackRect = RectF()
    private var thumbProgress = 0f
    private var animator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
        updateContentDescription()
        minimumWidth = dp(52f).toInt()
        minimumHeight = dp(32f).toInt()
    }

    private fun updateContentDescription() {
        contentDescription = if (checked) "On" else "Off"
    }

    private fun animateThumb(target: Float) {
        if (!designSystem.visualPolicy.motionEnabled) {
            thumbProgress = target
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(thumbProgress, target).apply {
            duration = designSystem.scaledDuration(AnimationCategory.SHORT, 90L)
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                thumbProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val w = width.toFloat()
        val h = height.toFloat()
        val trackH = dp(22f)
        val trackY = (h - trackH) / 2f

        trackPaint.color = if (checked) c.primary else blend(c.surfaceElevated, Color.BLACK, 0.4f)
        trackRect.set(0f, trackY, w, trackY + trackH)
        canvas.drawRoundRect(trackRect, trackH / 2f, trackH / 2f, trackPaint)

        val thumbR = dp(12f)
        val travel = w - thumbR * 2f
        val thumbX = thumbR + travel * thumbProgress
        val thumbY = h / 2f
        thumbPaint.color = if (checked) Color.WHITE else Color.rgb(150, 160, 172)
        canvas.drawCircle(thumbX, thumbY, thumbR, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                checked = !checked
                onCheckedChange?.invoke(checked)
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(52f).toInt(), widthMeasureSpec),
            resolveSize(dp(32f).toInt(), heightMeasureSpec),
        )
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val red = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
        val green = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        return Color.rgb(red, green, blue)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}