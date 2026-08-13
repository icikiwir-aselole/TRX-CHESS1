package com.troxzy.trxchess.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.troxzy.trxchess.R
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.brand.KnightView
import com.troxzy.trxchess.ui.common.SectionLabelView
import com.troxzy.trxchess.ui.components.GlowBackground
import com.troxzy.trxchess.ui.components.ParticleField
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens

/**
 * Home screen: brand hero (knight + wordmark), then a hierarchy of feature
 * cards with micro-interactions. Navigation callbacks are injected; the
 * screen holds no business logic.
 */
class HomeScreen @JvmOverloads constructor(
    context: Context,
    private val container: AppContainer,
    private val onNavigate: (HomeDestination) -> Unit,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val designSystem: DesignSystem = container.designSystem
    private val scroll = ScrollView(context)
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val particleField = ParticleField(context, null, designSystem)

    init {
        addView(GlowBackground(context, null, designSystem), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(particleField, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300)))

        scroll.isFillViewport = false
        addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        scroll.addView(content)

        content.addView(hero(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300)))

        addSection(R.string.home_quick_analysis, "ANALYSIS")
        addCard(HomeDestination.QUICK_ANALYSIS)
        addCard(HomeDestination.ADVANCED_ANALYSIS)
        addCard(HomeDestination.BOARD_EDITOR)

        addSection(null, "LIBRARY")
        addCard(HomeDestination.HISTORY)
        addCard(HomeDestination.OVERLAY)

        addSection(null, "SYSTEM")
        addCard(HomeDestination.SETTINGS)
        addCard(HomeDestination.DIAGNOSTICS)
        addCard(HomeDestination.ABOUT)

        content.addView(View(context), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)))
    }

    private fun hero(): View {
        val hero = FrameLayout(context)
        val knight = KnightView(context, null, designSystem)
        val size = dp(120)
        hero.addView(
            knight,
            FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                topMargin = dp(16)
            },
        )
        val wordmark = TextView(context).apply {
            text = resources.getString(R.string.home_title)
            setTextColor(designSystem.colors.textPrimary)
            textSize = TypeTokens.Display.sizeSp
            typeface = TypeTokens.Display.typeface
            gravity = Gravity.CENTER
        }
        hero.addView(
            wordmark,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = dp(40)
            },
        )
        val tagline = TextView(context).apply {
            text = resources.getString(R.string.home_subtitle)
            setTextColor(designSystem.colors.textMuted)
            textSize = TypeTokens.Caption.sizeSp
            typeface = TypeTokens.Caption.typeface
            gravity = Gravity.CENTER
        }
        hero.addView(
            tagline,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = dp(16)
            },
        )
        return hero
    }

    private fun addSection(description: Int?, title: String) {
        content.addView(
            SectionLabelView(context, null, designSystem).apply { text = title },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)),
        )
    }

    private fun addCard(destination: HomeDestination) {
        content.addView(
            NavCard(context, designSystem, destination) { onNavigate(destination) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(76)).apply {
                leftMargin = dp(Spacing)
                rightMargin = dp(Spacing)
                bottomMargin = dp(12)
            },
        )
    }

    fun onResume() {
        particleField.visibility = if (designSystem.visualPolicy.particlesEnabled) View.VISIBLE else View.GONE
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val Spacing = 16
    }
}

enum class HomeDestination { QUICK_ANALYSIS, ADVANCED_ANALYSIS, BOARD_EDITOR, HISTORY, OVERLAY, SETTINGS, DIAGNOSTICS, ABOUT }

/** Navigation card with title, description and press micro-interaction. */
private class NavCard(
    context: Context,
    private val designSystem: DesignSystem,
    private val destination: HomeDestination,
    private val onClick: () -> Unit,
) : View(context) {

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var pressed = false

    init {
        isClickable = true
        isFocusable = true
        contentDescription = resources.getString(titleRes(destination))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(280f).toInt(), widthMeasureSpec),
            resolveSize(dp(76f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = dp(16f)

        cardPaint.color = c.surfaceElevated
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, cardPaint)

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = dp(1f)
        borderPaint.color = if (pressed) c.primary else c.divider
        rect.set(dp(0.5f), dp(0.5f), w - dp(0.5f), h - dp(0.5f))
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        if (designSystem.visualPolicy.glowEnabled && pressed) {
            borderPaint.strokeWidth = dp(2f)
            borderPaint.color = c.primaryGlow
            rect.set(dp(2f), dp(2f), w - dp(2f), h - dp(2f))
            canvas.drawRoundRect(rect, radius, radius, borderPaint)
        }

        titlePaint.textSize = TypeTokens.BodyStrong.sizeSp * resources.displayMetrics.scaledDensity
        titlePaint.typeface = TypeTokens.BodyStrong.typeface
        titlePaint.color = c.textPrimary
        titlePaint.textAlign = Paint.Align.LEFT
        canvas.drawText(resources.getString(titleRes(destination)), dp(20f), dp(28f), titlePaint)

        descPaint.textSize = TypeTokens.Caption.sizeSp * resources.displayMetrics.scaledDensity
        descPaint.typeface = TypeTokens.Caption.typeface
        descPaint.color = c.textMuted
        descPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(resources.getString(descRes(destination)), dp(20f), dp(50f), descPaint)

        arrowPaint.color = c.primary
        arrowPaint.style = Paint.Style.STROKE
        arrowPaint.strokeWidth = dp(2f)
        arrowPaint.strokeCap = Paint.Cap.ROUND
        val ax = w - dp(26f)
        val ay = h / 2f
        canvas.drawLine(ax - dp(6f), ay - dp(5f), ax, ay, arrowPaint)
        canvas.drawLine(ax, ay, ax - dp(6f), ay + dp(5f), arrowPaint)
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
                onClick()
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

    private fun titleRes(d: HomeDestination): Int = when (d) {
        HomeDestination.QUICK_ANALYSIS -> R.string.home_quick_analysis
        HomeDestination.ADVANCED_ANALYSIS -> R.string.home_advanced_analysis
        HomeDestination.BOARD_EDITOR -> R.string.home_board_editor
        HomeDestination.HISTORY -> R.string.home_history
        HomeDestination.OVERLAY -> R.string.home_overlay
        HomeDestination.SETTINGS -> R.string.home_settings
        HomeDestination.DIAGNOSTICS -> R.string.home_diagnostics
        HomeDestination.ABOUT -> R.string.home_about
    }

    private fun descRes(d: HomeDestination): Int = when (d) {
        HomeDestination.QUICK_ANALYSIS -> R.string.home_quick_analysis_desc
        HomeDestination.ADVANCED_ANALYSIS -> R.string.home_advanced_analysis_desc
        HomeDestination.BOARD_EDITOR -> R.string.home_board_editor_desc
        HomeDestination.HISTORY -> R.string.home_history_desc
        HomeDestination.OVERLAY -> R.string.home_overlay_desc
        HomeDestination.SETTINGS -> R.string.home_settings_desc
        HomeDestination.DIAGNOSTICS -> R.string.home_diagnostics_desc
        HomeDestination.ABOUT -> R.string.home_about_desc
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}