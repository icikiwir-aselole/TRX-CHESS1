package com.troxzy.trxchess.ui.analysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.troxzy.trxchess.engine.api.Evaluation
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Evaluation history sparkline.
 *
 * Maintains a bounded history of evaluation samples and renders them as a
 * smooth path with a crimson-to-transparent fill. The graph redraws on new
 * samples only, throttled by the caller (analysis layer), so it never forces
 * board redraws.
 */
class EvalGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    private val history = ArrayDeque<Float>()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var fillShader: LinearGradient? = null
    private var cachedW = 0f
    private var cachedH = 0f

    init {
        linePaint.color = Color.rgb(224, 48, 64)
        linePaint.strokeWidth = dp(2f)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND
        baselinePaint.color = Color.argb(80, 110, 122, 134)
        baselinePaint.strokeWidth = dp(1f)
        contentDescription = "Evaluation graph"
    }

    fun addSample(ev: Evaluation) {
        history.addLast(fraction(ev))
        if (history.size > MAX_SAMPLES) history.removeFirst()
        invalidate()
    }

    fun clear() {
        history.clear()
        invalidate()
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
        if (w <= 0f || h <= 0f || history.isEmpty()) return

        canvas.drawRect(0f, h / 2f - dp(0.5f), w, h / 2f + dp(0.5f), baselinePaint)

        if (fillShader == null || w != cachedW || h != cachedH) {
            fillShader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(Color.argb(70, 224, 48, 64), Color.argb(0, 224, 48, 64)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            cachedW = w
            cachedH = h
        }

        val n = history.size
        val step = w / (MAX_SAMPLES - 1).toFloat()
        val path = Path()
        val fill = Path()
        history.forEachIndexed { i, value ->
            val x = (MAX_SAMPLES - 1 - (n - 1 - i)) * step
            val y = h * (1f - value)
            if (i == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo((MAX_SAMPLES - 1) * step, h)
        fill.lineTo(0f, h)
        fill.close()

        fillPaint.shader = fillShader
        canvas.drawPath(fill, fillPaint)
        fillPaint.shader = null
        canvas.drawPath(path, linePaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    companion object {
        const val MAX_SAMPLES = 96
    }
}