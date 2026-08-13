package com.troxzy.trxchess.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.diag.DiagnosticsRepository
import com.troxzy.trxchess.diag.FrameMonitor
import com.troxzy.trxchess.engine.api.EngineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiagnosticsUiState(
    val apiLevel: Int = 0,
    val abi: String = "unknown",
    val cpuCores: Int = 0,
    val memoryClassMb: Int = 0,
    val batteryPct: String = "—",
    val charging: Boolean = false,
    val thermal: String = "—",
    val engineState: EngineState = EngineState.Uninitialized,
    val threads: Int = 0,
    val hashMb: Int = 0,
    val storageFreeMb: Long = -1L,
    val network: String = "—",
    val frameMs: Double = 0.0,
    val jankyFrames: Int = 0,
    val keystoreReady: Boolean = false,
)

/**
 * Diagnostics screen state — all values from real telemetry.
 */
class DiagnosticsViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = DiagnosticsRepository(container.context)
    private val frameMonitor = FrameMonitor()

    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    fun refresh() {
        val s = container.settings.settings.value
        val (batteryPct, charging) = repo.battery()
        _state.value = DiagnosticsUiState(
            apiLevel = repo.apiLevel(),
            abi = repo.abi(),
            cpuCores = repo.cpuCores(),
            memoryClassMb = repo.memoryClassMb(),
            batteryPct = batteryPct?.let { "$it%" } ?: "—",
            charging = charging ?: false,
            thermal = when (repo.thermal()) {
                com.troxzy.trxchess.ui.designsystem.ThermalStress.NONE -> "None"
                com.troxzy.trxchess.ui.designsystem.ThermalStress.MODERATE -> "Moderate"
                com.troxzy.trxchess.ui.designsystem.ThermalStress.SEVERE -> "Severe"
            },
            engineState = repo.engineState(container.coordinator.engineState),
            threads = s.threads,
            hashMb = s.hashMb,
            storageFreeMb = repo.storageFreeMb(),
            network = when (repo.networkAvailable()) {
                true -> "Connected"
                false -> "Offline"
                null -> "Unknown"
            },
            frameMs = frameMonitor.avgFrameMs,
            jankyFrames = frameMonitor.jankyFrames,
            keystoreReady = runCatching {
                container.secureStorage.put("diag_probe", "ok")
                container.secureStorage.get("diag_probe") == "ok"
            }.getOrDefault(false),
        )
    }

    fun onScreenVisible(visible: Boolean) {
        if (visible) {
            refresh()
            frameMonitor.start()
        } else {
            frameMonitor.stop()
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DiagnosticsViewModel(container) as T
        }
    }
}