package com.troxzy.trxchess.overlay

import com.troxzy.trxchess.engine.api.EngineLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OverlayData(
    val evaluation: String,
    val bestMove: String,
    val depth: Int,
    val nodes: Long,
    val nps: Long,
    val multiPv: List<EngineLine>,
    val engineActive: Boolean,
    val engineReady: Boolean,
    val timestampMs: Long,
) {
    companion object {
        val EMPTY = OverlayData("—", "—", 0, 0L, 0L, emptyList(), false, false, 0L)
    }
}

data class OverlayPrefs(
    val compact: Boolean = true,
    val opacity: Float = 0.92f,
)

/**
 * Process-wide data bridge for the overlay window.
 *
 * The overlay service renders only data published here; it never owns engine
 * logic. The analysis layer publishes coalesced snapshots (throttled to the
 * configured overlay frequency) from a single owner in the app process.
 */
class OverlayController {
    private val _data = MutableStateFlow(OverlayData.EMPTY)
    val data: StateFlow<OverlayData> = _data.asStateFlow()

    private val _prefs = MutableStateFlow(OverlayPrefs())
    val prefs: StateFlow<OverlayPrefs> = _prefs.asStateFlow()

    fun publish(snapshot: OverlayData) {
        _data.value = snapshot
    }

    fun setPrefs(prefs: OverlayPrefs) {
        _prefs.value = prefs
    }

    companion object {
        @Volatile
        private var instance: OverlayController? = null

        /** Single process-wide controller; the app container is the owning writer. */
        fun get(): OverlayController = instance ?: synchronized(this) {
            instance ?: OverlayController().also { instance = it }
        }
    }
}