package com.troxzy.trxchess.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import kotlin.random.Random

/**
 * Bounded, visibility-aware particle field.
 *
 * Used sparingly (splash, engine-active accent). Particle count scales with
 * the [DesignSystem] visual policy and drops to zero when particles are
 * disabled or the view is not attached/visible. Uses a single fixed frame
 * timer and is cancelled on detach.
 */
class ParticleField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    private val maxParticles = 40
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var particles = arrayListOf<Particle>()
    private var running = false
    private var lastFrame = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            val dt = ((now - lastFrame).coerceIn(8L, 100L)) / 1000f
            lastFrame = now
            step(dt)
            invalidate()
            handler.postDelayed(this, FRAME_MS)
        }
    }

    private class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var alpha: Float,
        var seed: Int,
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) start() else stop()
    }

    private fun start() {
        if (running || !designSystem.visualPolicy.particlesEnabled) return
        running = true
        lastFrame = SystemClock.uptimeMillis()
        handler.removeCallbacks(frame)
        handler.post(frame)
    }

    private fun stop() {
        running = false
        handler.removeCallbacks(frame)
    }

    private fun step(dt: Float) {
        val density = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val multiplier = designSystem.visualPolicy.particleMultiplier
        if (multiplier <= 0f) {
            particles.clear()
            return
        }
        val count = (maxParticles * multiplier).toInt()
        while (particles.size < count) {
            particles.add(
                Particle(
                    Random.nextFloat() * w,
                    h + dp(10f, density),
                    (Random.nextFloat() - 0.5f) * dp(8f, density),
                    -dp(14f + Random.nextFloat() * 22f, density),
                    dp(1.5f + Random.nextFloat() * 2.5f, density),
                    0.25f + Random.nextFloat() * 0.4f,
                    Random.nextInt(),
                ),
            )
        }
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (p.y < -dp(12f, density)) iterator.remove()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!designSystem.visualPolicy.particlesEnabled) return
        val density = resources.displayMetrics.density
        val glowAlpha = (designSystem.visualPolicy.glowEnabled).let { if (it) 40 else 18 }
        for (p in particles) {
            val alpha = (255 * p.alpha).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, 224, 48, 64)
            canvas.drawCircle(p.x, p.y, p.size, paint)
            if (glowAlpha > 0) {
                paint.color = Color.argb((alpha * glowAlpha / 255).coerceIn(0, 255), 224, 48, 64)
                canvas.drawCircle(p.x, p.y, p.size * 3f, paint)
            }
        }
    }

    private fun dp(v: Float, density: Float): Float = v * density

    companion object {
        private const val FRAME_MS = 33L
    }
}
