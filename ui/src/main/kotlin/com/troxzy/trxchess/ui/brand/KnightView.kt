package com.troxzy.trxchess.ui.brand

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.troxzy.trxchess.ui.designsystem.AnimationCategory
import com.troxzy.trxchess.ui.designsystem.DesignSystem

/**
 * Renders the TRX Knight mark with a metallic dark-armor gradient, silver
 * highlight, crimson glow and subtle battle fragments. Optionally runs a
 * red light-sweep reveal used by the splash screen.
 *
 * Rendering is static after initialization except for the explicit sweep
 * animation; drawing cost is constant regardless of size.
 */
class KnightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    private val silhouette = KnightMark.silhouette()
    private val slash = KnightMark.slash()
    private val fragment = KnightMark.fragment()

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val armorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var armorGradient: LinearGradient? = null
    private var glowGradient: RadialGradient? = null
    private var cachedW = 0f
    private var cachedH = 0f
    private var scale = 1f

    /** 0..1 progress of the red light sweep; -1 disables the sweep. */
    var sweep: Float = -1f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    /** Scale-down for press feedback (0..1). */
    var pressScale: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    init {
        contentDescription = "TRX Knight"
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeWidth = dp(1.2f)
        accentPaint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val c = designSystem.colors
        val cx = w / 2f
        val cy = h / 2f
        val s = w.coerceAtMost(h)
        val unit = s / 100f

        if (armorGradient == null || w != cachedW || h != cachedH) {
            val dark = Color.rgb(34, 38, 46)
            val mid = Color.rgb(120, 128, 140)
            val light = Color.rgb(226, 232, 240)
            armorGradient = LinearGradient(
                0f, cy - s * 0.45f, 0f, cy + s * 0.45f,
                intArrayOf(dark, mid, light),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
            glowGradient = RadialGradient(
                cx, cy, s * 0.6f,
                intArrayOf(Color.argb(70, 224, 48, 64), Color.argb(0, 224, 48, 64)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            cachedW = w
            cachedH = h
        }

        if (designSystem.visualPolicy.glowEnabled) {
            glowPaint.shader = glowGradient
            canvas.drawCircle(cx, cy, s * 0.6f, glowPaint)
            glowPaint.shader = null
        }

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(unit * pressScale, unit * pressScale)
        canvas.translate(-50f, -50f)

        // dark armor base
        armorPaint.shader = armorGradient
        canvas.drawPath(silhouette, armorPaint)
        armorPaint.shader = null

        // outline
        outlinePaint.color = Color.argb(160, 236, 242, 248)
        canvas.drawPath(silhouette, outlinePaint)

        // crimson accents
        accentPaint.color = Color.argb(90, 224, 48, 64)
        canvas.drawPath(slash, accentPaint)
        canvas.drawPath(fragment, accentPaint)

        // eye glow
        eyePaint.color = Color.rgb(255, 62, 78)
        eyePaint.setShadowLayer(dp(6f), 0f, 0f, Color.rgb(255, 62, 78))
        canvas.drawCircle(30f, 30f, 2.6f, eyePaint)
        eyePaint.clearShadowLayer()

        canvas.restore()

        // red light sweep
        if (sweep >= 0f && designSystem.visualPolicy.motionEnabled) {
            val sweepX = -s * 0.4f + sweep * s * 1.8f
            val sweepPath = Path().apply {
                moveTo(sweepX - s * 0.35f, cy - s * 0.7f)
                lineTo(sweepX + s * 0.12f, cy - s * 0.7f)
                lineTo(sweepX + s * 0.35f, cy + s * 0.7f)
                lineTo(sweepX - s * 0.12f, cy + s * 0.7f)
                close()
            }
            sweepPaint.shader = LinearGradient(
                sweepX - s * 0.4f, 0f, sweepX + s * 0.4f, 0f,
                intArrayOf(
                    Color.argb(0, 255, 80, 96),
                    Color.argb(140, 255, 80, 96),
                    Color.argb(0, 255, 80, 96),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(sweepPath, sweepPaint)
            sweepPaint.shader = null
        }
    }

    /** Runs the cinematic red light-sweep once, from left to right. */
    fun runSweep(onEnd: (() -> Unit)? = null) {
        if (!designSystem.visualPolicy.motionEnabled) {
            sweep = -1f
            onEnd?.invoke()
            return
        }
        ValueAnimator.ofFloat(-0.2f, 1.1f).apply {
            duration = designSystem.scaledDuration(AnimationCategory.CINEMATIC, 200L)
            interpolator = DecelerateInterpolator()
            addUpdateListener { sweep = it.animatedValue as Float }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    sweep = -1f
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    fun animatePressIn() {
        if (!designSystem.visualPolicy.motionEnabled) {
            pressScale = 0.94f
            return
        }
        ValueAnimator.ofFloat(pressScale, 0.94f).apply {
            duration = designSystem.scaledDuration(AnimationCategory.MICRO, 60L)
            addUpdateListener { pressScale = it.animatedValue as Float }
            start()
        }
    }

    fun animatePressOut() {
        if (!designSystem.visualPolicy.motionEnabled) {
            pressScale = 1f
            return
        }
        ValueAnimator.ofFloat(pressScale, 1f).apply {
            duration = designSystem.scaledDuration(AnimationCategory.SHORT, 80L)
            interpolator = DecelerateInterpolator()
            addUpdateListener { pressScale = it.animatedValue as Float }
            start()
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
