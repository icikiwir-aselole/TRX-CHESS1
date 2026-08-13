package com.troxzy.trxchess.engine.uci

import com.troxzy.trxchess.chess.ChessPosition
import com.troxzy.trxchess.chess.Fen
import com.troxzy.trxchess.engine.api.AnalysisRequest
import com.troxzy.trxchess.engine.api.ChessEngine
import com.troxzy.trxchess.engine.api.EngineAnalysis
import com.troxzy.trxchess.engine.api.EngineConfig
import com.troxzy.trxchess.engine.api.EngineLine
import com.troxzy.trxchess.engine.api.EngineResult
import com.troxzy.trxchess.engine.api.EngineState
import com.troxzy.trxchess.engine.api.Evaluation
import com.troxzy.trxchess.engine.api.SearchLimit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * UCI-compatible engine implementation.
 *
 * When a native backend is supplied it is invoked synchronously per analysis
 * request (baseline path). Otherwise a deterministic fallback enumerates the
 * legal moves of the position. Both paths emit a single `EngineAnalysis`
 * snapshot, which the coordinator layer coalesces for the UI.
 */
class UciEngine(
    private val native: (suspend (String) -> List<String>?)? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ChessEngine {

    override val version: String = "uci-1.0.0"

    private val state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    private val analysis = MutableSharedFlow<EngineAnalysis>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var current: ChessPosition? = null
    private var config = EngineConfig()
    private var job: Job? = null

    override suspend fun initialize(config: EngineConfig): EngineResult {
        state.value = EngineState.Initializing
        this.config = config
        delay(1)
        state.value = EngineState.Ready
        return EngineResult.Ok
    }

    override suspend fun setPosition(position: ChessPosition): EngineResult {
        current = position
        return EngineResult.Ok
    }

    override suspend fun startAnalysis(request: AnalysisRequest) {
        stopAnalysis()
        val p = request.position
        current = p
        job = scope.launch {
            state.value = EngineState.Analyzing
            val reqId = request.analysisId.ifBlank { UUID.randomUUID().toString() }
            val key = Fen.serialize(p).hashCode().toString(16)
            val lines = if (native != null) {
                parseNative(native.invoke(buildUciCommand(request)) ?: emptyList()).lines
            } else {
                fallback(p, request)
            }
            analysis.emit(EngineAnalysis(reqId, key, lines, System.currentTimeMillis()))
            state.value = EngineState.Ready
        }
    }

    override suspend fun stopAnalysis() {
        job?.cancelAndJoin()
        job = null
        if (state.value == EngineState.Analyzing) state.value = EngineState.Ready
    }

    override suspend fun shutdown() {
        job?.cancel()
        job = null
        state.value = EngineState.Shutdown
        scope.cancel()
    }

    override fun observeState(): Flow<EngineState> = state.asStateFlow()

    override fun observeAnalysis(): Flow<EngineAnalysis> = analysis.asSharedFlow()

    private fun buildUciCommand(r: AnalysisRequest): String = buildString {
        append("position fen ")
        append(Fen.serialize(r.position))
        append("\ngo ")
        when (val l = r.limit) {
            is SearchLimit.Depth -> append("depth ").append(l.value)
            is SearchLimit.TimeMs -> append("movetime ").append(l.value)
            is SearchLimit.Nodes -> append("nodes ").append(l.value)
        }
        append("\n")
    }

    private fun parseNative(lines: List<String>): EngineAnalysis {
        val infos = lines.mapNotNull(UciParser::parseInfo)
        val grouped = infos.groupBy { it.multiPv }
        val out = grouped.toSortedMap().values.map { xs ->
            val x = xs.maxByOrNull { it.depth ?: 0 }!!
            EngineLine(x.multiPv, x.score ?: Evaluation.Centipawn(0), x.depth ?: 0, x.nodes ?: 0, x.nps ?: 0, x.pv)
        }
        return EngineAnalysis(UUID.randomUUID().toString(), "native", out, System.currentTimeMillis())
    }

    private fun fallback(p: ChessPosition, r: AnalysisRequest): List<EngineLine> {
        val moves = p.legalMoves().take(r.multiPv.coerceIn(1, 8))
        return moves.mapIndexed { i, move ->
            EngineLine(i + 1, Evaluation.Centipawn(0), 1, 0, 0, listOf(move.toString()))
        }
    }
}