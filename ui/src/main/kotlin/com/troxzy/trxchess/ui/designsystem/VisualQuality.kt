package com.troxzy.trxchess.ui.designsystem

/**
 * Adaptive visual quality.
 *
 * Pure policy: device capability + thermal + battery + reduced-motion settings
 * are reduced to a single [VisualPolicy] that every screen reads. This keeps
 * per-screen code free of device probing and lets the platform degrade visual
 * cost under pressure without disabling usability.
 */
enum class VisualQuality { LOW, MEDIUM, HIGH, ULTRA, CUSTOM }

data class VisualSignals(
    val deviceClass: DeviceClass,
    val thermal: ThermalStress,
    val batteryPct: Int,
    val charging: Boolean,
    val reducedMotion: Boolean,
)

enum class DeviceClass { LOW_END, MID_RANGE, HIGH_END }
enum class ThermalStress { NONE, MODERATE, SEVERE }

data class VisualPolicy(
    val visualQuality: VisualQuality = VisualQuality.HIGH,
    val motionEnabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val blurEnabled: Boolean = true,
    val glowEnabled: Boolean = true,
    val maxAnimationScale: Float = 1f,
    val particleMultiplier: Float = 1f,
) {
    val isStatic: Boolean get() = !motionEnabled
    val animationScale: Float get() = if (motionEnabled) maxAnimationScale else 0f
}

object VisualQualityPolicy {

    fun resolve(signals: VisualSignals): VisualPolicy {
        val quality = when {
            signals.reducedMotion -> VisualQuality.LOW
            signals.thermal == ThermalStress.SEVERE -> VisualQuality.LOW
            signals.thermal == ThermalStress.MODERATE -> VisualQuality.MEDIUM
            signals.batteryPct < 15 && !signals.charging -> VisualQuality.MEDIUM
            signals.batteryPct < 30 && !signals.charging -> VisualQuality.HIGH
            signals.deviceClass == DeviceClass.LOW_END -> VisualQuality.MEDIUM
            signals.deviceClass == DeviceClass.MID_RANGE -> VisualQuality.HIGH
            else -> VisualQuality.ULTRA
        }
        return policyFor(quality, signals)
    }

    fun policyFor(quality: VisualQuality, signals: VisualSignals): VisualPolicy {
        val base = when (quality) {
            VisualQuality.LOW -> VisualPolicy(
                visualQuality = quality,
                motionEnabled = false,
                particlesEnabled = false,
                blurEnabled = false,
                glowEnabled = false,
                maxAnimationScale = 0f,
                particleMultiplier = 0f,
            )
            VisualQuality.MEDIUM -> VisualPolicy(
                visualQuality = quality,
                motionEnabled = true,
                particlesEnabled = false,
                blurEnabled = false,
                glowEnabled = true,
                maxAnimationScale = 0.7f,
                particleMultiplier = 0.4f,
            )
            VisualQuality.HIGH -> VisualPolicy(
                visualQuality = quality,
                motionEnabled = true,
                particlesEnabled = true,
                blurEnabled = true,
                glowEnabled = true,
                maxAnimationScale = 0.9f,
                particleMultiplier = 0.8f,
            )
            VisualQuality.ULTRA -> VisualPolicy(
                visualQuality = quality,
                motionEnabled = true,
                particlesEnabled = true,
                blurEnabled = true,
                glowEnabled = true,
                maxAnimationScale = 1f,
                particleMultiplier = 1f,
            )
            VisualQuality.CUSTOM -> VisualPolicy(
                visualQuality = quality,
                motionEnabled = !signals.reducedMotion,
                particlesEnabled = !signals.reducedMotion,
                blurEnabled = true,
                glowEnabled = true,
                maxAnimationScale = 1f,
                particleMultiplier = 1f,
            )
        }
        // Thermal and battery always cap the budget regardless of user choice.
        return when (signals.thermal) {
            ThermalStress.SEVERE -> base.copy(
                motionEnabled = false,
                particlesEnabled = false,
                glowEnabled = false,
                maxAnimationScale = 0f,
                particleMultiplier = 0f,
            )
            ThermalStress.MODERATE -> base.copy(
                particlesEnabled = base.particlesEnabled && signals.deviceClass != DeviceClass.LOW_END,
                particleMultiplier = base.particleMultiplier * 0.5f,
                maxAnimationScale = (base.maxAnimationScale * 0.8f).coerceIn(0f, 1f),
            )
            ThermalStress.NONE -> base
        }
    }
}
