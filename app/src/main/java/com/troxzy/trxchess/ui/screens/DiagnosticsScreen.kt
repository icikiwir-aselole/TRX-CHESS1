package com.troxzy.trxchess.ui.screens

import androidx.activity.ComponentActivity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.troxzy.trxchess.R
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.common.TopBarView
import com.troxzy.trxchess.ui.components.GlowBackground
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.TypeTokens
import com.troxzy.trxchess.vm.DiagnosticsUiState
import com.troxzy.trxchess.vm.DiagnosticsViewModel

/**
 * Diagnostics screen: real telemetry only (system, engine, rendering,
 * storage, network, security). Refreshes on visibility and via a manual
 * refresh row.
 */
class DiagnosticsScreen @JvmOverloads constructor(
    context: Context,
    private val container: AppContainer,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val designSystem: DesignSystem = container.designSystem
    private val activity = context as ComponentActivity
    private val viewModel: DiagnosticsViewModel = ViewModelProvider(
        activity,
        DiagnosticsViewModel.factory(container),
    )[DiagnosticsViewModel::class.java]

    private val scroll = ScrollView(context)
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val topBar = TopBarView(context, null, designSystem).apply {
        title = resources.getString(R.string.diagnostics_title)
        showBack = true
        onBack = { activity.onBackPressedDispatcher.onBackPressed() }
    }

    init {
        addView(GlowBackground(context, null, designSystem), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
        addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = dp(56) })
        scroll.addView(content)

        viewModel.onScreenVisible(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel.onScreenVisible(false)
    }

    fun refresh() {
        viewModel.refresh()
        render(viewModel.state.value)
    }

    private fun render(state: DiagnosticsUiState) {
        content.removeAllViews()
        val rows = listOf(
            Triple(R.string.diagnostics_system, "API", state.apiLevel.toString()),
            Triple(R.string.diagnostics_system, "ABI", state.abi),
            Triple(R.string.diagnostics_system, "Cores", state.cpuCores.toString()),
            Triple(R.string.diagnostics_system, "Memory class", "${state.memoryClassMb} MB"),
            Triple(R.string.diagnostics_system, "Battery", "${state.batteryPct} · ${if (state.charging) "charging" else "not charging"}"),
            Triple(R.string.diagnostics_system, "Thermal", state.thermal),
            Triple(R.string.diagnostics_engine, "State", state.engineState.toString()),
            Triple(R.string.diagnostics_engine, "Threads", state.threads.toString()),
            Triple(R.string.diagnostics_engine, "Hash", "${state.hashMb} MB"),
            Triple(R.string.diagnostics_performance, "Avg frame", "${"%.1f".format(state.frameMs)} ms"),
            Triple(R.string.diagnostics_performance, "Janky frames", state.jankyFrames.toString()),
            Triple(R.string.diagnostics_storage, "Free", "${state.storageFreeMb} MB"),
            Triple(R.string.diagnostics_network, "Network", state.network),
            Triple(R.string.diagnostics_security, "Keystore", if (state.keystoreReady) "Ready" else "Unavailable"),
        )
        var lastSection: Int? = null
        rows.forEach { (section, label, value) ->
            if (lastSection != section) {
                content.addView(
                    SectionLabel(context, designSystem).apply { text = resources.getString(section).uppercase() },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)),
                )
                lastSection = section
            }
            content.addView(
                MetricRow(context, designSystem, label, value),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)),
            )
        }
        content.addView(View(context), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}

private class MetricRow(
    context: Context,
    private val designSystem: DesignSystem,
    private val label: String,
    private val value: String,
) : View(context) {

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(280f).toInt(), widthMeasureSpec),
            resolveSize(dp(44f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        labelPaint.textSize = TypeTokens.Body.sizeSp * resources.displayMetrics.scaledDensity
        labelPaint.typeface = TypeTokens.Body.typeface
        labelPaint.color = c.textSecondary
        labelPaint.textAlign = Paint.Align.LEFT
        valuePaint.textSize = TypeTokens.BodyStrong.sizeSp * resources.displayMetrics.scaledDensity
        valuePaint.typeface = designSystem.monoTypeface
        valuePaint.color = c.textPrimary
        valuePaint.textAlign = Paint.Align.RIGHT
        val baseline = height / 2f - (labelPaint.descent() + labelPaint.ascent()) / 2f
        canvas.drawText(label, dp(20f), baseline, labelPaint)
        canvas.drawText(value, width - dp(20f), baseline, valuePaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}

private class SectionLabel(
    context: Context,
    private val designSystem: DesignSystem,
) : TextView(context) {

    init {
        textSize = TypeTokens.Label.sizeSp
        typeface = TypeTokens.Label.typeface
        setPadding(dp(20), 0, 0, 0)
        setTextColor(designSystem.colors.primary)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}