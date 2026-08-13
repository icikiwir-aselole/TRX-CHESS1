package com.troxzy.trxchess.ui.screens

import androidx.activity.ComponentActivity
import android.app.AlertDialog
import android.content.Context
import android.provider.Settings
import android.graphics.Canvas
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
import androidx.lifecycle.ViewModelProvider
import com.troxzy.trxchess.R
import com.troxzy.trxchess.core.AppSettings
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.common.SettingRowView
import com.troxzy.trxchess.ui.common.SectionLabelView
import com.troxzy.trxchess.ui.common.TopBarView
import com.troxzy.trxchess.ui.common.TrxSwitch
import com.troxzy.trxchess.ui.components.GlowBackground
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.ThemeMode
import com.troxzy.trxchess.ui.designsystem.TypeTokens
import com.troxzy.trxchess.ui.designsystem.VisualQuality
import com.troxzy.trxchess.vm.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Settings screen: every control writes through [SettingsViewModel] to
 * DataStore and is consumed by a real implementation. Overlay enablement
 * requires a permission flow handled by the host activity.
 */
class SettingsScreen @JvmOverloads constructor(
    context: Context,
    private val container: AppContainer,
    private val onOverlayPermissionRequested: () -> Unit,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val designSystem: DesignSystem = container.designSystem
    private val activity = context as ComponentActivity
    private val viewModel: SettingsViewModel = ViewModelProvider(
        activity,
        SettingsViewModel.factory(container),
    )[SettingsViewModel::class.java]

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val scroll = ScrollView(context)
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val topBar = TopBarView(context, null, designSystem).apply {
        title = resources.getString(R.string.settings_title)
        showBack = true
        onBack = { activity.onBackPressedDispatcher.onBackPressed() }
    }

    init {
        addView(GlowBackground(context, null, designSystem), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(topBar, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
        addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = dp(56) })
        scroll.addView(content)

        scope.launch {
            viewModel.settings.collect { settings -> render(settings) }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    private fun render(settings: AppSettings) {
        content.removeAllViews()

        section(R.string.settings_appearance)
        optionRow(
            R.string.settings_appearance_theme,
            themeLabel(settings.themeMode),
            listOf(
                resources.getString(R.string.settings_appearance_theme_dark),
                resources.getString(R.string.settings_appearance_theme_light),
                resources.getString(R.string.settings_appearance_theme_system),
            ),
        ) { index ->
            viewModel.setThemeMode(ThemeMode.entries[index])
        }
        optionRow(
            R.string.settings_appearance_board_theme,
            settings.boardThemeId.replaceFirstChar { it.uppercase() },
            listOf("Classic", "Crimson", "Carbon", "Neon", "Custom"),
        ) { index ->
            viewModel.setBoardTheme(listOf("classic", "crimson", "carbon", "neon", "custom")[index])
        }
        optionRow(
            R.string.settings_animation_quality,
            settings.visualQuality.name.replaceFirstChar { it.uppercase() },
            listOf("Low", "Medium", "High", "Ultra", "Custom"),
        ) { index ->
            viewModel.setVisualQuality(VisualQuality.entries[index])
        }

        section(R.string.settings_engine)
        stepperRow(R.string.settings_engine_threads, settings.threads, 1, 4, 1) { viewModel.setThreads(it) }
        stepperRow(R.string.settings_engine_hash, settings.hashMb, 16, 256, 16) { viewModel.setHashMb(it) }
        stepperRow(R.string.settings_engine_multipv, settings.multiPv, 1, 8, 1) { viewModel.setMultiPv(it) }
        stepperRow(R.string.settings_analysis_default_depth, settings.defaultDepth, 1, 40, 1) { viewModel.setDefaultDepth(it) }

        section(R.string.settings_animation)
        toggleRow(R.string.settings_animation_enabled, null, settings.animationEnabled) { viewModel.setAnimationEnabled(it) }
        toggleRow(R.string.settings_animation_particles, null, settings.particlesEnabled) { viewModel.setParticlesEnabled(it) }
        toggleRow(R.string.settings_animation_reduced, null, settings.reducedMotion) { viewModel.setReducedMotion(it) }

        section(R.string.settings_overlay)
        toggleRow(R.string.overlay_enable, null, settings.overlayEnabled) { enabled ->
            if (enabled && !Settings.canDrawOverlays(context)) {
                onOverlayPermissionRequested()
            } else {
                viewModel.setOverlayEnabled(enabled)
                container.setOverlayRunning(enabled)
            }
        }
        toggleRow(R.string.overlay_compact, null, settings.overlayCompact) { viewModel.setOverlayCompact(it) }
        stepperRow(R.string.overlay_opacity, (settings.overlayOpacity * 100).toInt(), 50, 100, 5) {
            viewModel.setOverlayOpacity(it / 100f)
        }

        section(R.string.settings_performance)
        toggleRow(R.string.settings_performance_power_saver, null, settings.powerSaver) { viewModel.setPowerSaver(it) }
        toggleRow(R.string.settings_battery_low_battery_stop, null, settings.lowBatteryStop) { viewModel.setLowBatteryStop(it) }

        section(R.string.settings_security)
        infoRow(R.string.settings_security_keystore, R.string.settings_security_keystore_desc)

        section(R.string.settings_diagnostics)
        toggleRow(R.string.settings_diagnostics_telemetry, null, settings.telemetryEnabled) { viewModel.setTelemetryEnabled(it) }

        content.addView(View(context), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)))
    }

    private fun section(titleRes: Int) {
        content.addView(
            SectionLabelView(context, null, designSystem).apply { text = resources.getString(titleRes).uppercase() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)),
        )
    }

    private fun toggleRow(labelRes: Int, subtitleRes: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(12), 0)
            minimumHeight = dp(56)
        }
        val label = TextView(context).apply {
            text = resources.getString(labelRes)
            setTextColor(designSystem.colors.textPrimary)
            textSize = TypeTokens.Body.sizeSp
            typeface = TypeTokens.Body.typeface
        }
        row.addView(label, LinearLayout.LayoutParams(0, dp(28), 1f))
        val switch = TrxSwitch(context, null, designSystem).apply {
            this.checked = checked
            onCheckedChange = onChange
        }
        row.addView(switch, LinearLayout.LayoutParams(dp(52), dp(32)))
        content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
    }

    private fun optionRow(labelRes: Int, value: String, options: List<String>, onPick: (Int) -> Unit) {
        val row = SettingRowView(context, null, designSystem).apply {
            label = resources.getString(labelRes)
            this.valueText = value
            chevron = true
            onTap = {
                AlertDialog.Builder(context)
                    .setTitle(resources.getString(labelRes))
                    .setItems(options.toTypedArray()) { _, which -> onPick(which) }
                    .setNegativeButton(R.string.nav_back, null)
                    .show()
            }
        }
        content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
    }

    private fun stepperRow(labelRes: Int, value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
        content.addView(
            StepperRow(context, designSystem, resources.getString(labelRes), value, min, max, step, onChange),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)),
        )
    }

    private fun infoRow(labelRes: Int, descRes: Int) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }
        row.addView(TextView(context).apply {
            text = resources.getString(labelRes)
            setTextColor(designSystem.colors.textPrimary)
            textSize = TypeTokens.Body.sizeSp
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)))
        row.addView(TextView(context).apply {
            text = resources.getString(descRes)
            setTextColor(designSystem.colors.textMuted)
            textSize = TypeTokens.Caption.sizeSp
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(20)))
        content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
    }

    private fun themeLabel(mode: ThemeMode): String = when (mode) {
        ThemeMode.DARK -> resources.getString(R.string.settings_appearance_theme_dark)
        ThemeMode.LIGHT -> resources.getString(R.string.settings_appearance_theme_light)
        ThemeMode.SYSTEM -> resources.getString(R.string.settings_appearance_theme_system)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}

/** Stepper row: - value + */
private class StepperRow(
    context: Context,
    private val designSystem: DesignSystem,
    private val label: String,
    private val value: Int,
    private val min: Int,
    private val max: Int,
    private val step: Int,
    private val onChange: (Int) -> Unit,
) : View(context) {

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val radius = dp(8f)

    init {
        isClickable = true
        contentDescription = "$label $value"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(dp(280f).toInt(), widthMeasureSpec),
            resolveSize(dp(56f).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = designSystem.colors
        val h = height.toFloat()

        labelPaint.textSize = TypeTokens.Body.sizeSp * resources.displayMetrics.scaledDensity
        labelPaint.typeface = TypeTokens.Body.typeface
        labelPaint.color = c.textPrimary
        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(label, dp(20f), h / 2f - (labelPaint.descent() + labelPaint.ascent()) / 2f, labelPaint)

        valuePaint.textSize = TypeTokens.BodyStrong.sizeSp * resources.displayMetrics.scaledDensity
        valuePaint.typeface = TypeTokens.BodyStrong.typeface
        valuePaint.color = c.primary
        valuePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(value.toString(), width / 2f + dp(24f), h / 2f - (valuePaint.descent() + valuePaint.ascent()) / 2f, valuePaint)

        btnPaint.color = c.surfaceElevated
        val btnSize = dp(34f)
        val btnY = (h - btnSize) / 2f
        val minusX = width - dp(92f)
        val plusX = width - dp(44f)
        rect.set(minusX, btnY, minusX + btnSize, btnY + btnSize)
        canvas.drawRoundRect(rect, radius, radius, btnPaint)
        rect.set(plusX, btnY, plusX + btnSize, btnY + btnSize)
        canvas.drawRoundRect(rect, radius, radius, btnPaint)

        btnTextPaint.color = c.textPrimary
        btnTextPaint.textSize = dp(16f)
        btnTextPaint.textAlign = Paint.Align.CENTER
        val baseY = h / 2f - (btnTextPaint.descent() + btnTextPaint.ascent()) / 2f
        canvas.drawText("−", minusX + btnSize / 2f, baseY, btnTextPaint)
        canvas.drawText("+", plusX + btnSize / 2f, baseY, btnTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val btnSize = dp(34f)
            val minusX = width - dp(92f)
            val plusX = width - dp(44f)
            val x = event.x
            val y = event.y
            val within = { bx: Float -> x in bx..(bx + btnSize) && y in (height - btnSize) / 2f..((height - btnSize) / 2f + btnSize) }
            when {
                within(minusX) -> onChange((value - step).coerceAtLeast(min))
                within(plusX) -> onChange((value + step).coerceAtMost(max))
                else -> return super.onTouchEvent(event)
            }
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}