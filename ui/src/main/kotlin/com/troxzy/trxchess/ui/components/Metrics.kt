package com.troxzy.trxchess.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens

/**
 * Monospace metric tile used in diagnostics and engine surfaces.
 */
class StatTile @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val designSystem: DesignSystem,
) : View(context, attrs) {

    var label: CharSequence = ""
        set(value) {
            field = value
            contentDescription = "$value: $valueText"
            invalidate()
        }

    var valueText: CharSequence = "—"
        set(value) {
            field = value
            contentDescription = "$label: $value"
            invalidate()
        }

    var accent: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        minimumWidth = dp(96f).toInt()
        minimumHeight = dp(52f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val density = resources.displayMetrics.density

        labelPaint.textSize = TypeTokens.MonoSmall.sizeSp * resources.displayMetrics.scaledDensity
        labelPaint.typeface = designSystem.monoTypeface
        labelPaint.color = c.textMuted

        valuePaint.textSize = TypeTokens.MonoBold.sizeSp * resources.displayMetrics.scaledDensity
        valuePaint.typeface = designSystem.monoBoldTypeface
        valuePaint.color = if (accent) c.primary else c.textPrimary

        canvas.drawText(label.toString(), dp(12f), dp(18f), labelPaint)
        canvas.drawText(valueText.toString(), dp(12f), dp(40f), valuePaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
