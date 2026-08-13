package com.troxzy.trxchess.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * TRX-CHESS overlay panel.
 *
 * Renders the analysis snapshot as a dark glass panel with a thin crimson
 * border. Compact mode shows a single-line summary; expanded mode shows
 * evaluation, best line, MultiPV, depth, nodes and NPS. The panel is
 * draggable and snaps to the nearest horizontal edge.
 *
 * Rendering is throttled: the view coalesces snapshot updates and redraws
 * at most [MAX_RENDER_HZ] times per second while visible.
 */
class OverlayPanelView @JvmOverloads constructor(
    context: Context,
    private val controller: OverlayController = OverlayController.get(),
    initialUpdater: ((WindowManager.LayoutParams) -> Unit)? = null,
) : View(context) {

    var windowUpdater: ((WindowManager.LayoutParams) -> Unit)? = initialUpdater

    private var snapshot: OverlayData = OverlayData.EMPTY
    private var compact: Boolean = true
    private var opacity: Float = 0.92f
    private var windowWidthPx: Int = context.resources.displayMetrics.widthPixels
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val evalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelRect = RectF()

    private var lastRenderMs = 0L
    private var pendingInvalidate = false

    // drag state
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var originX = 0
    private var originY = 0

    private var snapAnimator: ValueAnimator? = null

    init {
        panelPaint.color = Color.rgb(13, 16, 22)
        panelPaint.alpha = (opacity * 255).toInt()
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = dp(1f)
        borderPaint.color = Color.rgb(120, 18, 30)
        evalPaint.textSize = sp(15f)
        evalPaint.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
        evalPaint.color = Color.rgb(242, 245, 247)
        textPaint.textSize = sp(12f)
        textPaint.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        textPaint.color = Color.rgb(184, 194, 204)
        mutedPaint.textSize = sp(10f)
        mutedPaint.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        mutedPaint.color = Color.rgb(110, 122, 134)
        accentPaint.textSize = sp(11f)
        accentPaint.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
        accentPaint.color = Color.rgb(224, 48, 64)
        dotPaint.color = Color.rgb(224, 48, 64)

        controller.data.observeOnUi { update ->
            snapshot = update
            requestThrottledRender()
        }
        controller.prefs.observeOnUi { p ->
            compact = p.compact
            opacity = p.opacity
            panelPaint.alpha = (opacity * 255).toInt()
            requestLayout()
            requestThrottledRender()
        }
    }

    private fun <T> StateFlow<T>.observeOnUi(block: (T) -> Unit) {
        scope.launch { collect(block) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    fun setWindowWidth(widthPx: Int) {
        windowWidthPx = widthPx
    }

    private fun requestThrottledRender() {
        val now = SystemClock.uptimeMillis()
        if (now - lastRenderMs >= 1000L / MAX_RENDER_HZ) {
            lastRenderMs = now
            invalidate()
        } else if (!pendingInvalidate) {
            pendingInvalidate = true
            post {
                pendingInvalidate = false
                lastRenderMs = SystemClock.uptimeMillis()
                invalidate()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = if (compact) dp(190f) else dp(240f)
        val h = if (compact) dp(44f) else dp(178f)
        val desiredW = w + paddingLeft + paddingRight
        val desiredH = h + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(desiredW.toInt(), widthMeasureSpec),
            resolveSize(desiredH.toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = dp(14f)

        glowPaint.style = Paint.Style.STROKE
        glowPaint.strokeWidth = dp(6f)
        glowPaint.color = Color.argb(40, 224, 48, 64)
        panelRect.set(0f, 0f, w, h)
        canvas.drawRoundRect(panelRect, radius, radius, glowPaint)
        canvas.drawRoundRect(panelRect, radius, radius, panelPaint)
        canvas.drawRoundRect(panelRect, radius, radius, borderPaint)

        if (compact) {
            drawCompact(canvas, w, h)
        } else {
            drawExpanded(canvas, w, h)
        }
    }

    private fun drawCompact(canvas: Canvas, w: Float, h: Float) {
        val centerY = h / 2f
        val active = snapshot.engineActive
        if (active) {
            canvas.drawCircle(dp(13f), centerY, dp(3f), dotPaint)
        }
        val label = if (active) "● " else ""
        canvas.drawText("$label${snapshot.evaluation}", dp(22f), centerY + sp(5f), evalPaint)
        val moveText = snapshot.bestMove.ifBlank { "—" }
        val depthText = "D${snapshot.depth}"
        val depthWidth = mutedPaint.measureText("  $depthText")
        val moveWidth = textPaint.measureText("  $moveText")
        canvas.drawText("  $moveText", w - depthWidth - moveWidth - dp(6f), centerY + sp(4f), textPaint)
        canvas.drawText("  $depthText", w - dp(6f), centerY + sp(4f), mutedPaint)
    }

    private fun drawExpanded(canvas: Canvas, w: Float, h: Float) {
        var y = dp(20f)
        val left = dp(14f)
        val activeText = when {
            snapshot.engineActive -> "● LIVE"
            snapshot.engineReady -> "READY"
            else -> "IDLE"
        }
        canvas.drawText(activeText, left, y, if (snapshot.engineActive) accentPaint else mutedPaint)
        canvas.drawText(snapshot.evaluation, w - dp(14f) - evalPaint.measureText(snapshot.evaluation), y, evalPaint)

        y += dp(20f)
        val bestLine = snapshot.multiPv.firstOrNull()?.pv?.joinToString(" ") ?: snapshot.bestMove.ifBlank { "—" }
        canvas.drawText("best", left, y, mutedPaint)
        canvas.drawText(bestLine, w - dp(14f) - textPaint.measureText(bestLine), y, textPaint)

        y += dp(16f)
        for (line in snapshot.multiPv.take(3)) {
            val label = "${line.multiPv}. ${line.pv.take(3).joinToString(" ")}"
            canvas.drawText(label, left, y, textPaint)
            val score = formatScore(line)
            canvas.drawText(score, w - dp(14f) - mutedPaint.measureText(score), y, mutedPaint)
            y += dp(14f)
        }

        y += dp(4f)
        canvas.drawText(
            "D${snapshot.depth}  ${formatNodes(snapshot.nodes)}  ${formatNps(snapshot.nps)}",
            left,
            y,
            mutedPaint,
        )
    }

    private fun formatScore(line: com.troxzy.trxchess.engine.api.EngineLine): String {
        val ev = line.evaluation
        return when (ev) {
            is com.troxzy.trxchess.engine.api.Evaluation.Mate -> "M${ev.plies}"
            is com.troxzy.trxchess.engine.api.Evaluation.Centipawn -> "%+.2f".format(ev.value / 100.0)
            else -> "?"
        }
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

    fun setCompact(value: Boolean) {
        compact = value
        requestLayout()
        invalidate()
    }

    fun isCompact() = compact

    fun snapToEdge(edge: Int) {
        val lp = layoutParams as? WindowManager.LayoutParams ?: return
        val targetX = if (edge == EDGE_LEFT) 8 else windowWidthPx - width - 8
        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(lp.x, targetX).apply {
            duration = SNAP_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                lp.x = a.animatedValue as Int
                windowUpdater?.invoke(lp)
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                downX = event.rawX
                downY = event.rawY
                val lp = layoutParams as? WindowManager.LayoutParams
                originX = lp?.x ?: 0
                originY = lp?.y ?: 0
                snapAnimator?.cancel()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val lp = layoutParams as? WindowManager.LayoutParams ?: return true
                lp.x = originX + (event.rawX - downX).toInt()
                lp.y = originY + (event.rawY - downY).toInt()
                windowUpdater?.invoke(lp)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                val wasTap = abs(dx) < 10 && abs(dy) < 10
                if (wasTap) {
                    setCompact(!compact)
                    controller.setPrefs(controller.prefs.value.copy(compact = !compact))
                } else {
                    val lp = layoutParams as? WindowManager.LayoutParams ?: return true
                    val centerX = lp.x + width / 2f
                    val edge = if (centerX < windowWidthPx / 2f) EDGE_LEFT else EDGE_RIGHT
                    snapToEdge(edge)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private fun sp(v: Float): Float = v * resources.displayMetrics.scaledDensity

    companion object {
        const val EDGE_LEFT = 0
        const val EDGE_RIGHT = 1
        const val MAX_RENDER_HZ = 30
        const val SNAP_DURATION_MS = 160L
    }
}