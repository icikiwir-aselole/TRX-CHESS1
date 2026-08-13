package com.troxzy.trxchess.ui.analysis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.troxzy.trxchess.engine.api.EngineState
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens

/**
 * Engine live indicator.
 *
 * Renders the actual [EngineState] reported by the engine layer — never a
 * claimed state. Shows a status label, a live pulse while analyzing, and the
 * latest depth/nodes/NPS metrics. The pulse is a bounded, visibility-aware
 * timer.
 */
class EngineStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var engineState: EngineState = EngineState.Uninitialized
        set(value) {
            field = value
            updateContentDescription()
            invalidate()
        }

    var depth: Int = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var nodes: Long = 0L
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var nps: Long = 0L
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private var pulseOn = false
    private val pulse = object : Runnable {
        override fun run() {
            if (engineState != EngineState.Analyzing || !isAttachedToWindow || visibility != VISIBLE) {
                pulseOn = false
                invalidate()
                return
            }
            pulseOn = !pulseOn
            invalidate()
            handler.postDelayed(this, 500L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (engineState == EngineState.Analyzing) {
            handler.post(pulse)
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(pulse)
        super.onDetachedFromWindow()
    }

    private fun updateContentDescription() {
        contentDescription = "Engine ${stateLabel()}"
    }

    private fun stateLabel(): String = when (engineState) {
        EngineState.Uninitialized -> "Unknown"
        EngineState.Initializing -> "Initializing"
        EngineState.Ready -> "Ready"
        EngineState.Analyzing -> "Analyzing"
        EngineState.Stopping -> "Stopping"
        EngineState.Crashed -> "Recovering"
        EngineState.Failed -> "Error"
        EngineState.Shutdown -> "Stopped"
    }

    private fun stateColor(): Int {
        val c = designSystem.colors
        return when (engineState) {
            EngineState.Analyzing -> c.primary
            EngineState.Ready -> c.success
            EngineState.Initializing, EngineState.Stopping, EngineState.Crashed -> c.warning
            EngineState.Failed -> c.danger
            else -> c.textMuted
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val density = resources.displayMetrics.density
        val left = dp(18f)

        labelPaint.textSize = TypeTokens.Label.sizeSp * resources.displayMetrics.scaledDensity
        labelPaint.typeface = TypeTokens.Label.typeface
        labelPaint.color = stateColor()

        metaPaint.textSize = TypeTokens.MonoSmall.sizeSp * resources.displayMetrics.scaledDensity
        metaPaint.typeface = designSystem.monoTypeface
        metaPaint.color = c.textMuted

        val analyzing = engineState == EngineState.Analyzing
        if (analyzing) {
            dotPaint.color = c.primary
            canvas.drawCircle(dp(9f), dp(10f), if (pulseOn) dp(4f) else dp(2.5f), dotPaint)
        } else {
            dotPaint.color = stateColor()
            canvas.drawCircle(dp(9f), dp(10f), dp(2.5f), dotPaint)
        }

        canvas.drawText(stateLabel(), left, dp(14f), labelPaint)

        val meta = "D${depth}  ${formatNodes(nodes)}  ${formatNps(nps)}"
        canvas.drawText(meta, left, dp(30f), metaPaint)
    }

    private fun formatNodes(n: Long): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000 -> "%.1fK".format(n / 1_000.0)
        else -> n.toString()
    }

    private fun formatNps(n: Long): String = when {
        n >= 1_000_000 -> "%.1fM/s".format(n / 1_000_000.0)
        n >= 1_000 -> "%.1fK/s".format(n / 1_000.0)
        else -> "$n/s"
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(220f).toInt(), widthMeasureSpec),
            resolveSize(dp(42f).toInt(), heightMeasureSpec),
        )
    }
}