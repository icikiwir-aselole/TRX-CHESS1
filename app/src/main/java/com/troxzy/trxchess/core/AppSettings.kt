package com.troxzy.trxchess.core

import com.troxzy.trxchess.data.Keys
import com.troxzy.trxchess.data.trxSettings
import com.troxzy.trxchess.ui.designsystem.ThemeMode
import com.troxzy.trxchess.ui.designsystem.VisualQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope

/**
 * User-configurable application settings.
 *
 * Every setting maps to a real implementation: theme mode drives
 * [com.troxzy.trxchess.ui.designsystem.DesignSystem], animation settings drive
 * the visual policy, engine settings drive [EngineConfig], and the overlay
 * settings drive the overlay window. No cosmetic-only switches.
 */
data class AppSettings(
    val threads: Int = 1,
    val hashMb: Int = 64,
    val multiPv: Int = 1,
    val defaultDepth: Int = 14,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val boardThemeId: String = "carbon",
    val animationEnabled: Boolean = true,
    val visualQuality: VisualQuality = VisualQuality.HIGH,
    val particlesEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val overlayEnabled: Boolean = false,
    val overlayCompact: Boolean = true,
    val overlayOpacity: Float = 0.92f,
    val powerSaver: Boolean = false,
    val lowBatteryStop: Boolean = true,
    val telemetryEnabled: Boolean = false,
)

class SettingsRepository(private val context: Context) {

    val settings: StateFlow<AppSettings> = context.trxSettings.data
        .map { prefs ->
            AppSettings(
                threads = prefs[Keys.threads] ?: 1,
                hashMb = prefs[Keys.hash] ?: 64,
                multiPv = prefs[Keys.multiPv] ?: 1,
                defaultDepth = prefs[Keys.defaultDepth] ?: 14,
                themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.themeMode] ?: "DARK") }
                    .getOrDefault(ThemeMode.DARK),
                boardThemeId = prefs[Keys.boardTheme] ?: "carbon",
                animationEnabled = prefs[Keys.animationEnabled] ?: true,
                visualQuality = runCatching { VisualQuality.valueOf(prefs[Keys.visualQuality] ?: "HIGH") }
                    .getOrDefault(VisualQuality.HIGH),
                particlesEnabled = prefs[Keys.particlesEnabled] ?: true,
                reducedMotion = prefs[Keys.reducedMotion] ?: false,
                overlayEnabled = prefs[Keys.overlay] ?: false,
                overlayCompact = prefs[Keys.overlayCompact] ?: true,
                overlayOpacity = prefs[Keys.overlayOpacity] ?: 0.92f,
                powerSaver = prefs[Keys.powerSaver] ?: false,
                lowBatteryStop = prefs[Keys.lowBatteryStop] ?: true,
                telemetryEnabled = prefs[Keys.telemetryEnabled] ?: false,
            )
        }
        .stateIn(CoroutineScope(kotlinx.coroutines.Dispatchers.IO), kotlinx.coroutines.flow.SharingStarted.Eagerly, AppSettings())

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = settings.value
        val next = transform(current)
        context.trxSettings.edit { prefs ->
            prefs[Keys.threads] = next.threads
            prefs[Keys.hash] = next.hashMb
            prefs[Keys.multiPv] = next.multiPv
            prefs[Keys.defaultDepth] = next.defaultDepth
            prefs[Keys.themeMode] = next.themeMode.name
            prefs[Keys.boardTheme] = next.boardThemeId
            prefs[Keys.animationEnabled] = next.animationEnabled
            prefs[Keys.visualQuality] = next.visualQuality.name
            prefs[Keys.particlesEnabled] = next.particlesEnabled
            prefs[Keys.reducedMotion] = next.reducedMotion
            prefs[Keys.overlay] = next.overlayEnabled
            prefs[Keys.overlayCompact] = next.overlayCompact
            prefs[Keys.overlayOpacity] = next.overlayOpacity
            prefs[Keys.powerSaver] = next.powerSaver
            prefs[Keys.lowBatteryStop] = next.lowBatteryStop
            prefs[Keys.telemetryEnabled] = next.telemetryEnabled
        }
    }
}