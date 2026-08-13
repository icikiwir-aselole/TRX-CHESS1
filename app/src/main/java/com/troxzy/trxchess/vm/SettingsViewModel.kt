package com.troxzy.trxchess.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.troxzy.trxchess.core.AppSettings
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.ui.designsystem.ThemeMode
import com.troxzy.trxchess.ui.designsystem.VisualQuality
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Settings screen state.
 *
 * Every setter writes through [AppContainer.settings] (DataStore) and every
 * value is consumed by a real implementation — theme mode, visual quality,
 * board theme, engine config, overlay prefs.
 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settings.settings

    fun setThreads(value: Int) = update { it.copy(threads = value.coerceIn(1, 8)) }
    fun setHashMb(value: Int) = update { it.copy(hashMb = value.coerceIn(16, 256)) }
    fun setMultiPv(value: Int) = update { it.copy(multiPv = value.coerceIn(1, 8)) }
    fun setDefaultDepth(value: Int) = update { it.copy(defaultDepth = value.coerceIn(1, 40)) }
    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setBoardTheme(id: String) = update { it.copy(boardThemeId = id) }
    fun setAnimationEnabled(value: Boolean) = update { it.copy(animationEnabled = value) }
    fun setVisualQuality(value: VisualQuality) = update { it.copy(visualQuality = value) }
    fun setParticlesEnabled(value: Boolean) = update { it.copy(particlesEnabled = value) }
    fun setReducedMotion(value: Boolean) = update { it.copy(reducedMotion = value) }
    fun setOverlayEnabled(value: Boolean) = update { it.copy(overlayEnabled = value) }
    fun setOverlayCompact(value: Boolean) = update { it.copy(overlayCompact = value) }
    fun setOverlayOpacity(value: Float) = update { it.copy(overlayOpacity = value.coerceIn(0.5f, 1f)) }
    fun setPowerSaver(value: Boolean) = update { it.copy(powerSaver = value) }
    fun setLowBatteryStop(value: Boolean) = update { it.copy(lowBatteryStop = value) }
    fun setTelemetryEnabled(value: Boolean) = update { it.copy(telemetryEnabled = value) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            container.settings.update(transform)
            container.designSystem.themeMode = container.settings.settings.value.themeMode
            container.updateOverlayPrefs()
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container) as T
        }
    }
}