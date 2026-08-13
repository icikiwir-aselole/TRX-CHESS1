package com.troxzy.trxchess.ui.components

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
import com.troxzy.trxchess.ui.designsystem.Elevation
import com.troxzy.trxchess.ui.designsystem.Radius

/**
 * Glass surface card with optional crimson glow and press micro-interaction.
 */
class GlassCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    var glow: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var radius: Float = Radius.Large
        set(value) {
            field = value
            invalidate()
        }

    private var press = 0f
    private var pressAnimator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "Card"
        minimumHeight = dp(64f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val w = width.toFloat()
        val h = height.toFloat()

        shadowPaint.setShadowLayer(Elevation.Card, 0f, dp(3f), Color.argb(90, 0, 0, 0))
        shadowPaint.color = c.surfaceElevated
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, shadowPaint)

        if (glow && designSystem.visualPolicy.glowEnabled) {
            glowPaint.color = Color.argb((30 * designSystem.visualPolicy.particleMultiplier).toInt(), 224, 48, 64)
            glowPaint.style = Paint.Style.STROKE
            glowPaint.strokeWidth = dp(2f)
            rect.set(dp(1f), dp(1f), w - dp(1f), h - dp(1f))
            canvas.drawRoundRect(rect, radius, radius, glowPaint)
        }

        val baseColor = c.surfaceElevated
        cardPaint.color = blend(baseColor, Color.WHITE, press * 0.06f)
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, cardPaint)

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = dp(1f)
        borderPaint.color = blend(c.divider, c.primary, if (glow) 0.35f else 0f)
        rect.set(dp(0.5f), dp(0.5f), w - dp(0.5f), h - dp(0.5f))
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isClickable) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animatePress(1f)
                return super.onTouchEvent(event) || true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animatePress(0f)
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animatePress(target: Float) {
        if (!designSystem.visualPolicy.motionEnabled) {
            press = target
            invalidate()
            return
        }
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(press, target).apply {
            duration = designSystem.scaledDuration(AnimationCategory.MICRO, 80L)
            interpolator = DecelerateInterpolator()
            addUpdateListener { press = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val alpha = (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t).toInt()
        val red = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
        val green = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        return Color.argb(alpha, red, green, blue)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
