package com.troxzy.trxchess.ui.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualQualityPolicyTest {

    private fun signals(
        deviceClass: DeviceClass = DeviceClass.HIGH_END,
        thermal: ThermalStress = ThermalStress.NONE,
        batteryPct: Int = 100,
        charging: Boolean = true,
        reducedMotion: Boolean = false,
    ) = VisualSignals(deviceClass, thermal, batteryPct, charging, reducedMotion)

    @Test
    fun `high end device with full battery resolves to ultra`() {
        val policy = VisualQualityPolicy.resolve(signals())
        assertEquals(VisualQuality.ULTRA, policy.visualQuality)
        assertTrue(policy.motionEnabled)
        assertTrue(policy.particlesEnabled)
        assertTrue(policy.glowEnabled)
        assertEquals(1f, policy.maxAnimationScale)
        assertEquals(1f, policy.particleMultiplier)
    }

    @Test
    fun `reduced motion forces static low quality`() {
        val policy = VisualQualityPolicy.resolve(signals(reducedMotion = true))
        assertEquals(VisualQuality.LOW, policy.visualQuality)
        assertFalse(policy.motionEnabled)
        assertFalse(policy.particlesEnabled)
        assertFalse(policy.glowEnabled)
        assertEquals(0f, policy.maxAnimationScale)
        assertTrue(policy.isStatic)
        assertEquals(0f, policy.animationScale)
    }

    @Test
    fun `severe thermal caps to low regardless of device`() {
        val policy = VisualQualityPolicy.resolve(signals(thermal = ThermalStress.SEVERE))
        assertEquals(VisualQuality.LOW, policy.visualQuality)
        assertFalse(policy.motionEnabled)
        assertFalse(policy.particlesEnabled)
        assertFalse(policy.glowEnabled)
    }

    @Test
    fun `moderate thermal reduces motion and particles`() {
        val policy = VisualQualityPolicy.resolve(signals(thermal = ThermalStress.MODERATE))
        assertEquals(VisualQuality.MEDIUM, policy.visualQuality)
        assertTrue(policy.motionEnabled)
        assertFalse(policy.particlesEnabled)
        assertTrue(policy.maxAnimationScale < 1f)
    }

    @Test
    fun `moderate thermal on low end disables particles entirely`() {
        val policy = VisualQualityPolicy.resolve(
            signals(deviceClass = DeviceClass.LOW_END, thermal = ThermalStress.MODERATE),
        )
        assertFalse(policy.particlesEnabled)
    }

    @Test
    fun `low battery without charging steps quality down`() {
        assertEquals(
            VisualQuality.MEDIUM,
            VisualQualityPolicy.resolve(signals(batteryPct = 10, charging = false)).visualQuality,
        )
        assertEquals(
            VisualQuality.HIGH,
            VisualQualityPolicy.resolve(signals(batteryPct = 20, charging = false)).visualQuality,
        )
    }

    @Test
    fun `charging battery keeps high quality`() {
        assertEquals(
            VisualQuality.ULTRA,
            VisualQualityPolicy.resolve(signals(batteryPct = 5, charging = true)).visualQuality,
        )
    }

    @Test
    fun `low end device resolves to medium`() {
        assertEquals(
            VisualQuality.MEDIUM,
            VisualQualityPolicy.resolve(signals(deviceClass = DeviceClass.LOW_END)).visualQuality,
        )
        assertEquals(
            VisualQuality.HIGH,
            VisualQualityPolicy.resolve(signals(deviceClass = DeviceClass.MID_RANGE)).visualQuality,
        )
    }

    @Test
    fun `explicit quality mapping is stable`() {
        val high = VisualQualityPolicy.policyFor(VisualQuality.HIGH, signals())
        assertEquals(0.9f, high.maxAnimationScale)
        assertEquals(0.8f, high.particleMultiplier)
        assertTrue(high.particlesEnabled)

        val custom = VisualQualityPolicy.policyFor(VisualQuality.CUSTOM, signals(reducedMotion = true))
        assertFalse(custom.motionEnabled)
        assertFalse(custom.particlesEnabled)
    }

    @Test
    fun `user choice is overridden by severe thermal`() {
        val policy = VisualQualityPolicy.policyFor(
            VisualQuality.ULTRA,
            signals(thermal = ThermalStress.SEVERE),
        )
        assertEquals(VisualQuality.ULTRA, policy.visualQuality)
        assertFalse(policy.motionEnabled)
        assertEquals(0f, policy.maxAnimationScale)
    }
}
