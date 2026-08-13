package com.troxzy.trxchess.ui.designsystem

import android.content.Context
import android.content.res.Configuration
import android.os.Build

/**
 * Central runtime access to the design system.
 *
 * A single [DesignSystem] instance is provided by the app container and
 * mutated by the theme/settings layer. Screens read colors and the current
 * [VisualPolicy] from here instead of probing the device themselves.
 */
class DesignSystem(private val appContext: Context) {

    var themeMode: ThemeMode = ThemeMode.DARK
        set(value) {
            if (field != value) {
                field = value
                notifyColorsChanged()
            }
        }

    var visualPolicy: VisualPolicy = VisualQualityPolicy.policyFor(
        VisualQuality.HIGH,
        VisualSignals(DeviceClass.HIGH_END, ThermalStress.NONE, 100, true, reducedMotion = false),
    )
        set(value) {
            if (field != value) {
                field = value
                notifyPolicyChanged()
            }
        }

    val colors: TrxColors
        get() = if (effectiveDark()) TrxColors.Dark else TrxColors.Light

    fun effectiveDark(): Boolean = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> (appContext.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /** Monospace typeface helpers used by diagnostic surfaces. */
    val monoTypeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
    val monoBoldTypeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)

    fun scaledDuration(category: AnimationCategory, floorMs: Long = 0L): Long {
        val base = category.durationMs()
        val scale = if (visualPolicy.motionEnabled) visualPolicy.maxAnimationScale else 0f
        return MotionTokens.scaled(base, scale, floorMs)
    }

    private val colorListeners = mutableSetOf<() -> Unit>()
    private val policyListeners = mutableSetOf<() -> Unit>()

    fun observeColors(listener: () -> Unit) {
        colorListeners += listener
    }

    fun observePolicy(listener: () -> Unit) {
        policyListeners += listener
    }

    fun onUiModeChanged(uiMode: Int) {
        if (themeMode == ThemeMode.SYSTEM) notifyColorsChanged()
    }

    private fun notifyColorsChanged() {
        colorListeners.forEach { it() }
    }

    private fun notifyPolicyChanged() {
        policyListeners.forEach { it() }
    }

    companion object {
        /** Android API level of the current device. */
        val apiLevel: Int = Build.VERSION.SDK_INT

        /** True when the accessibility "remove animations" is requested. */
        fun isSystemAnimatorScaleZero(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val value = android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                )
                return value == 0f
            }
            return false
        }
    }
}
