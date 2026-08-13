package com.troxzy.trxchess.overlay

import com.troxzy.trxchess.engine.api.EngineAnalysis
import com.troxzy.trxchess.engine.api.EngineState
import com.troxzy.trxchess.engine.api.Evaluation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Coalesces engine analysis into the overlay window.
 *
 * Engine events may arrive at high frequency; this publisher throttles the
 * snapshot to the configured overlay frequency and only writes meaningful
 * changes. It never runs engine logic — it only formats and forwards data
 * produced by the analysis layer.
 */
class OverlayPublisher(
    private val controller: OverlayController,
    private val scope: CoroutineScope,
    private val throttleMs: Long = 250L,
) {
    private var job: Job? = null
    private var lastSentNs = 0L

    fun bind(
        stateFlow: Flow<EngineState>,
        analysisFlow: Flow<EngineAnalysis?>,
    ) {
        job?.cancel()
        job = scope.launch {
            var lastState: EngineState? = null
            stateFlow.collect { state ->
                engineState = state
                if (state != lastState) {
                    lastState = state
                    publish(state, currentAnalysis)
                }
            }
        }
        job = scope.launch {
            analysisFlow.collect { result ->
                currentAnalysis = result
                val now = System.nanoTime()
                if (now - lastSentNs >= throttleMs * 1_000_000L) {
                    lastSentNs = now
                    publish(engineState, result)
                }
            }
        }
    }

    @Volatile
    private var currentAnalysis: EngineAnalysis? = null

    @Volatile
    private var engineState: EngineState = EngineState.Uninitialized

    private fun publish(state: EngineState, result: EngineAnalysis?) {
        val line = result?.lines?.firstOrNull()
        val evaluation = line?.evaluation?.let { formatScore(it) } ?: "—"
        val bestMove = line?.pv?.firstOrNull() ?: ""
        val active = state == EngineState.Analyzing
        controller.publish(
            OverlayData(
                evaluation = evaluation,
                bestMove = bestMove,
                depth = line?.depth ?: 0,
                nodes = line?.nodes ?: 0,
                nps = line?.nps ?: 0,
                multiPv = result?.lines ?: emptyList(),
                engineActive = active,
                engineReady = state == EngineState.Ready,
                timestampMs = System.currentTimeMillis(),
            )
        )
    }

    private fun formatScore(ev: Evaluation): String = when (ev) {
        is Evaluation.Mate -> "M${ev.plies}"
        is Evaluation.Centipawn -> "%+.2f".format(ev.value / 100.0)
        is Evaluation.LowerBound -> formatScore(ev.score)
        is Evaluation.UpperBound -> formatScore(ev.score)
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}