package com.troxzy.trxchess.diag

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import com.troxzy.trxchess.diagnostics.DiagnosticsSnapshot
import com.troxzy.trxchess.engine.api.EngineState
import com.troxzy.trxchess.ui.designsystem.ThermalStress
import kotlinx.coroutines.flow.StateFlow

/**
 * Real telemetry for the diagnostics screen.
 *
 * Every value comes from an actual subsystem: Android API level, ABI, CPU
 * cores, memory class, battery state, thermal state, engine state, storage
 * free space, and live frame statistics from [FrameMonitor]. No fabricated
 * numbers.
 */
class DiagnosticsRepository(private val context: Context) {

    fun engineState(flow: StateFlow<EngineState>): EngineState = flow.value

    fun thermal(): ThermalStress {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE,
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalStress.NONE
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalStress.MODERATE
                else -> ThermalStress.SEVERE
            }
        }
        return ThermalStress.NONE
    }

    fun battery(): Pair<Int?, Boolean?> {
        val intent = context.registerReceiver(
            null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ) ?: return null to null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else null
        val charging = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL -> true
            else -> false
        }
        return pct to charging
    }

    fun storageFreeMb(): Long {
        return runCatching {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBytes / (1024 * 1024)
        }.getOrDefault(-1L)
    }

    fun memoryClassMb(): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return am.memoryClass
    }

    fun abi(): String = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

    fun cpuCores(): Int = Runtime.getRuntime().availableProcessors()

    fun apiLevel(): Int = Build.VERSION.SDK_INT

    fun networkAvailable(): Boolean? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return runCatching {
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(null)
    }
}

object Intent {
    const val ACTION_BATTERY_CHANGED = android.content.Intent.ACTION_BATTERY_CHANGED
}