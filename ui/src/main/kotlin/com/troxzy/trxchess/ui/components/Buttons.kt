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
import com.troxzy.trxchess.ui.designsystem.Radius
import com.troxzy.trxchess.ui.designsystem.TypeTokens

enum class ButtonStyle { PRIMARY, SECONDARY, GHOST, DANGER }

/**
 * TRX-CHESS button with press micro-interaction and consistent styling.
 */
class TrxButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var style: ButtonStyle = ButtonStyle.PRIMARY
        set(value) {
            field = value
            invalidate()
        }

    var text: CharSequence = ""
        set(value) {
            field = value
            contentDescription = value
            invalidate()
        }

    var enabledState: Boolean = true
        set(value) {
            field = value
            isEnabled = value
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var press = 0f
    private var pressAnimator: ValueAnimator? = null

    init {
        isClickable = true
        isFocusable = true
        minimumHeight = dp(48f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val textWidth = textPaint.measureText(text.toString())
        val w = textWidth + dp(32f)
        val h = dp(48f)
        setMeasuredDimension(
            resolveSize((w + paddingLeft + paddingRight).toInt(), widthMeasureSpec),
            resolveSize((h + paddingTop + paddingBottom).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = Radius.Medium

        textPaint.textSize = TypeTokens.Label.sizeSp * resources.displayMetrics.scaledDensity
        textPaint.typeface = TypeTokens.Label.typeface
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true

        val alphaMul = if (enabledState) 1f else 0.45f
        val bgColor: Int
        val fgColor: Int
        val borderColor: Int
        when (style) {
            ButtonStyle.PRIMARY -> {
                bgColor = c.primary
                fgColor = Color.WHITE
                borderColor = c.primary
            }
            ButtonStyle.SECONDARY -> {
                bgColor = blend(c.surface, c.textMuted, 0.08f)
                fgColor = c.textPrimary
                borderColor = c.divider
            }
            ButtonStyle.GHOST -> {
                bgColor = Color.TRANSPARENT
                fgColor = c.primary
                borderColor = c.primaryDim
            }
            ButtonStyle.DANGER -> {
                bgColor = c.danger
                fgColor = Color.WHITE
                borderColor = c.danger
            }
        }

        bgPaint.color = blend(bgColor, Color.WHITE, if (style != ButtonStyle.GHOST) press * 0.12f else 0f)
        bgPaint.alpha = (Color.alpha(bgColor) * alphaMul).toInt()
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = dp(1f)
        borderPaint.color = blend(borderColor, c.textMuted, 0.3f)
        borderPaint.alpha = (Color.alpha(borderColor) * alphaMul).toInt()
        rect.set(dp(0.5f), dp(0.5f), w - dp(0.5f), h - dp(0.5f))
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        textPaint.color = fgColor
        textPaint.alpha = (255 * alphaMul).toInt()
        val baseline = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text.toString(), w / 2f, baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animatePress(1f)
                return true
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
        val red = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
        val green = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        return Color.argb(Color.alpha(a), red, green, blue)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
