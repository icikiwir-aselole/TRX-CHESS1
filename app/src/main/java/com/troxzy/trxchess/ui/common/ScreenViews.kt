package com.troxzy.trxchess.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.Radius
import com.troxzy.trxchess.ui.designsystem.TypeTokens

/**
 * Screen top bar: back button (when not home), title, optional right action.
 */
class TopBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var title: CharSequence = ""
        set(value) {
            field = value
            contentDescription = value
            invalidate()
        }

    var showBack: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var onBack: (() -> Unit)? = null

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backPath = Path()
    private val dividerPaint = Paint()

    init {
        isClickable = true
        minimumHeight = dp(56f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(240f).toInt(), widthMeasureSpec),
            resolveSize(dp(56f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val h = height.toFloat()

        titlePaint.textSize = TypeTokens.Title.sizeSp * resources.displayMetrics.scaledDensity
        titlePaint.typeface = TypeTokens.Title.typeface
        titlePaint.color = c.textPrimary
        titlePaint.textAlign = Paint.Align.LEFT

        var left = dp(20f)
        if (showBack) {
            val backX = dp(24f)
            val backY = h / 2f
            backPaint.color = c.textSecondary
            backPaint.style = Paint.Style.STROKE
            backPaint.strokeWidth = dp(2f)
            backPaint.strokeCap = Paint.Cap.ROUND
            backPaint.strokeJoin = Paint.Join.ROUND
            backPath.rewind()
            backPath.moveTo(backX + dp(5f), backY - dp(7f))
            backPath.lineTo(backX - dp(3f), backY)
            backPath.lineTo(backX + dp(5f), backY + dp(7f))
            canvas.drawPath(backPath, backPaint)
            left = dp(44f)
        }

        val baseline = h / 2f - (titlePaint.descent() + titlePaint.ascent()) / 2f
        canvas.drawText(title.toString(), left, baseline, titlePaint)

        dividerPaint.color = c.divider
        dividerPaint.strokeWidth = dp(1f)
        canvas.drawRect(0f, h - dp(1f), width.toFloat(), h, dividerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP && showBack) {
            onBack?.invoke()
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private class Path : android.graphics.Path()
}

/**
 * Settings row: label on the left, custom control on the right.
 */
class SettingRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var label: CharSequence = ""
        set(value) {
            field = value
            contentDescription = value
            invalidate()
        }

    var subtitle: CharSequence? = null
        set(value) {
            field = value
            invalidate()
        }

    var onTap: (() -> Unit)? = null

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chevronPath = Path()
    private val dividerPaint = Paint()
    private var pressed = false

    var valueText: CharSequence? = null
        set(value) {
            field = value
            invalidate()
        }

    var chevron: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        isClickable = true
        isFocusable = true
        minimumHeight = dp(56f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(200f).toInt(), widthMeasureSpec),
            resolveSize(dp(56f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val w = width.toFloat()
        val h = height.toFloat()

        if (pressed) {
            canvas.drawColor(blend(c.surface, c.primary, 0.06f))
        }

        labelPaint.textSize = TypeTokens.Body.sizeSp * resources.displayMetrics.scaledDensity
        labelPaint.typeface = TypeTokens.Body.typeface
        labelPaint.color = c.textPrimary
        labelPaint.textAlign = Paint.Align.LEFT

        val hasSubtitle = !subtitle.isNullOrBlank()
        val centerY = if (hasSubtitle) h * 0.38f else h / 2f
        val baseline = centerY - (labelPaint.descent() + labelPaint.ascent()) / 2f
        canvas.drawText(label.toString(), dp(20f), baseline, labelPaint)

        if (hasSubtitle) {
            subtitlePaint.textSize = TypeTokens.Caption.sizeSp * resources.displayMetrics.scaledDensity
            subtitlePaint.typeface = TypeTokens.Caption.typeface
            subtitlePaint.color = c.textMuted
            val subBaseline = h * 0.72f - (subtitlePaint.descent() + subtitlePaint.ascent()) / 2f
            canvas.drawText(subtitle.toString(), dp(20f), subBaseline, subtitlePaint)
        }

        val value = valueText
        if (value != null && !chevron) {
            valuePaint.textSize = TypeTokens.Body.sizeSp * resources.displayMetrics.scaledDensity
            valuePaint.typeface = TypeTokens.Body.typeface
            valuePaint.color = c.textSecondary
            valuePaint.textAlign = Paint.Align.RIGHT
            val vw = valuePaint.measureText(value.toString())
            canvas.drawText(value.toString(), w - dp(20f), baseline, valuePaint)
        }

        if (chevron) {
            val cx = w - dp(24f)
            val cy = h / 2f
            chevronPaint.color = c.textMuted
            chevronPaint.style = Paint.Style.STROKE
            chevronPaint.strokeWidth = dp(2f)
            chevronPaint.strokeCap = Paint.Cap.ROUND
            chevronPaint.strokeJoin = Paint.Join.ROUND
            chevronPath.rewind()
            chevronPath.moveTo(cx - dp(3f), cy - dp(5f))
            chevronPath.lineTo(cx + dp(3f), cy)
            chevronPath.lineTo(cx - dp(3f), cy + dp(5f))
            canvas.drawPath(chevronPath, chevronPaint)
        }

        dividerPaint.color = c.divider
        dividerPaint.strokeWidth = dp(1f)
        canvas.drawRect(dp(20f), h - dp(1f), w, h, dividerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                pressed = false
                invalidate()
                onTap?.invoke()
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressed = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val red = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
        val green = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
        val blue = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        return Color.rgb(red, green, blue)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private class Path : android.graphics.Path()
}

/**
 * Section header label.
 */
class SectionLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var text: CharSequence = ""
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(200f).toInt(), widthMeasureSpec),
            resolveSize(dp(36f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        paint.textSize = TypeTokens.Label.sizeSp * resources.displayMetrics.scaledDensity
        paint.typeface = TypeTokens.Label.typeface
        paint.color = c.primary
        paint.textAlign = Paint.Align.LEFT
        val baseline = height / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text.toString().uppercase(), dp(20f), baseline, paint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}