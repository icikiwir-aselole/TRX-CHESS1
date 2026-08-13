package com.troxzy.trxchess.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.troxzy.trxchess.ui.designsystem.DesignSystem

/**
 * Deep background with a subtle crimson radial glow. A single cached radial
 * gradient per frame; cost scales with the visual policy and collapses to a
 * flat fill when glow is disabled.
 */
class GlowBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    private val paint = Paint()
    private var radial: RadialGradient? = null
    private var cachedW = 0f
    private var cachedH = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.drawColor(c.background)

        if (designSystem.visualPolicy.glowEnabled) {
            if (radial == null || w != cachedW || h != cachedH) {
                val cx = w * 0.5f
                val cy = h * 0.14f
                val r = w.coerceAtLeast(h) * 0.7f
                radial = RadialGradient(
                    cx, cy, r,
                    intArrayOf(Color.argb(38, 224, 48, 64), Color.argb(0, 224, 48, 64)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP,
                )
                cachedW = w
                cachedH = h
            }
            paint.shader = radial
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
        }
    }
}
