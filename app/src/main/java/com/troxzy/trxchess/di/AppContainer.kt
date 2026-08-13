package com.troxzy.trxchess.di

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.troxzy.trxchess.analysis.AnalysisCoordinator
import com.troxzy.trxchess.core.AppSettings
import com.troxzy.trxchess.core.SettingsRepository
import com.troxzy.trxchess.data.TrxDatabase
import com.troxzy.trxchess.engine.api.ChessEngine
import com.troxzy.trxchess.engine.api.EngineConfig
import com.troxzy.trxchess.engine.nativeengine.NativeEngine
import com.troxzy.trxchess.overlay.OverlayController
import com.troxzy.trxchess.security.SecureStorage
import com.troxzy.trxchess.ui.designsystem.DesignSystem
import com.troxzy.trxchess.ui.designsystem.DeviceClass
import com.troxzy.trxchess.ui.designsystem.ThermalStress
import com.troxzy.trxchess.ui.designsystem.VisualPolicy
import com.troxzy.trxchess.ui.designsystem.VisualQualityPolicy
import com.troxzy.trxchess.ui.designsystem.VisualSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Process-scoped dependency container.
 *
 * Owns the single engine + coordinator instance, the settings repository, the
 * design system and the overlay bridge. Created once in [TrxApp]; screens
 * receive it through the activity. No global mutable state object: ownership
 * is explicit and confined here.
 */
class AppContainer(private val appContext: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Application context for subsystems that need it. */
    val context: Context = appContext

    val settings: SettingsRepository = SettingsRepository(appContext)

    val designSystem: DesignSystem = DesignSystem(appContext)

    val secureStorage: SecureStorage = SecureStorage(appContext)

    val overlay: OverlayController = OverlayController.get()

    val history: HistoryStore = HistoryStore(TrxDatabase.get(appContext))

    val engine: ChessEngine = NativeEngine()

    val coordinator: AnalysisCoordinator = AnalysisCoordinator(engine)

    private val deviceSignals = MutableStateFlow(
        VisualSignals(
            deviceClass = detectDeviceClass(),
            thermal = ThermalStress.NONE,
            batteryPct = 100,
            charging = true,
            reducedMotion = false,
        )
    )

    /** Live visual policy: settings + device signals → policy. */
    val visualPolicy: StateFlow<VisualPolicy> = combine(
        settings.settings,
        deviceSignals,
    ) { s, signals ->
        var policy = VisualQualityPolicy.resolve(
            signals.copy(
                reducedMotion = signals.reducedMotion || s.reducedMotion || !s.animationEnabled,
                deviceClass = if (s.powerSaver) DeviceClass.LOW_END else signals.deviceClass,
            )
        )
        if (!s.animationEnabled) policy = policy.copy(motionEnabled = false, maxAnimationScale = 0f)
        if (!s.particlesEnabled) policy = policy.copy(particlesEnabled = false, particleMultiplier = 0f)
        policy
    }.stateIn(appScope, SharingStarted.Eagerly, VisualQualityPolicy.policyFor(
        com.troxzy.trxchess.ui.designsystem.VisualQuality.HIGH,
        VisualSignals(DeviceClass.HIGH_END, ThermalStress.NONE, 100, true, false),
    ))

    fun updateDeviceSignals(signals: VisualSignals) {
        deviceSignals.value = signals
    }

    /** Engine configuration derived from settings. */
    fun engineConfig(): EngineConfig {
        val s = settings.settings.value
        return EngineConfig(
            threads = s.threads.coerceIn(1, 4),
            hashMb = s.hashMb.coerceIn(16, 256),
            multiPv = s.multiPv.coerceIn(1, 8),
        )
    }

    fun updateOverlayPrefs() {
        val s = settings.settings.value
        overlay.setPrefs(
            com.troxzy.trxchess.overlay.OverlayPrefs(
                compact = s.overlayCompact,
                opacity = s.overlayOpacity,
            )
        )
    }

    /** Start/stop the floating overlay service (hosts must check overlay permission first). */
    fun setOverlayRunning(enabled: Boolean) {
        val intent = android.content.Intent(appContext, com.troxzy.trxchess.overlay.OverlayService::class.java)
        if (enabled) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            }
        } else {
            runCatching { appContext.stopService(intent) }
        }
    }

    private var signalJob: kotlinx.coroutines.Job? = null

    /** Periodically refresh battery/thermal signals into [visualPolicy]. */
    fun startSignalMonitoring() {
        if (signalJob?.isActive == true) return
        signalJob = appScope.launch {
            while (isActive) {
                updateDeviceSignals(readDeviceSignals())
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    private fun readDeviceSignals(): VisualSignals {
        val am = appContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                am.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE -> ThermalStress.SEVERE
                am.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE -> ThermalStress.MODERATE
                else -> ThermalStress.NONE
            }
        } else {
            ThermalStress.NONE
        }
        var batteryPct = 100
        var charging = true
        val batteryIntent = appContext.registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
        )
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) batteryPct = (level * 100 / scale).coerceIn(0, 100)
            val status = batteryIntent.getIntExtra(
                android.os.BatteryManager.EXTRA_STATUS,
                android.os.BatteryManager.BATTERY_STATUS_UNKNOWN,
            )
            charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL
        }
        return VisualSignals(
            deviceClass = detectDeviceClass(),
            thermal = thermal,
            batteryPct = batteryPct,
            charging = charging,
            reducedMotion = DesignSystem.isSystemAnimatorScaleZero(appContext),
        )
    }

    private fun detectDeviceClass(): DeviceClass {
        val cores = Runtime.getRuntime().availableProcessors()
        val memoryMb = runCatching {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.memoryClass
        }.getOrDefault(128)
        return when {
            cores <= 2 || memoryMb <= 128 -> DeviceClass.LOW_END
            cores <= 4 || memoryMb <= 256 -> DeviceClass.MID_RANGE
            else -> DeviceClass.HIGH_END
        }
    }

    fun shutdown() {
        appScope.launchSafe { coordinator.shutdown() }
        appScope.cancel()
    }

    private fun CoroutineScope.launchSafe(block: suspend () -> Unit) {
        launch { runCatching { block() } }
    }
}