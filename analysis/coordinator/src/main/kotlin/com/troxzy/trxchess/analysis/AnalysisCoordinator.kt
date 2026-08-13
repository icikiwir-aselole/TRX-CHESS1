package com.troxzy.trxchess.analysis

import com.troxzy.trxchess.chess.ChessPosition
import com.troxzy.trxchess.chess.Fen
import com.troxzy.trxchess.core.common.AppDispatchers
import com.troxzy.trxchess.engine.api.AnalysisRequest
import com.troxzy.trxchess.engine.api.ChessEngine
import com.troxzy.trxchess.engine.api.EngineAnalysis
import com.troxzy.trxchess.engine.api.EngineConfig
import com.troxzy.trxchess.engine.api.EngineState
import com.troxzy.trxchess.engine.api.Priority
import com.troxzy.trxchess.engine.api.SearchLimit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Single owner of engine interaction for the UI layer.
 *
 * State machines are explicit ([CoordinatorState], [EngineState]). Analysis
 * results carry a position identity so stale results from a previous
 * position are never applied.
 */
class AnalysisCoordinator(
    private val engine: ChessEngine,
    private val dispatchers: AppDispatchers = AppDispatchers(),
) {
    private val _state = MutableStateFlow<CoordinatorState>(CoordinatorState.Idle)
    val state: StateFlow<CoordinatorState> = _state.asStateFlow()

    private val _analysis = MutableStateFlow<EngineAnalysis?>(null)
    val analysis: StateFlow<EngineAnalysis?> = _analysis.asStateFlow()

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private var collector: Job? = null
    private var activeHash: String? = null
    private var engineWatcher: Job? = null

    init {
        engineWatcher = CoroutineScope(SupervisorJob() + dispatchers.engine).launch {
            engine.observeState().collect { _engineState.value = it }
        }
    }

    suspend fun initialize(c: EngineConfig) = engine.initialize(c)

    suspend fun analyze(
        p: ChessPosition,
        limit: SearchLimit,
        multiPv: Int = 1,
        priority: Priority = Priority.INTERACTIVE,
    ) {
        stop()
        val hash = Fen.serialize(p).hashCode().toString(16)
        activeHash = hash
        _state.value = CoordinatorState.Analyzing(UUID.randomUUID().toString())
        collector = CoroutineScope(SupervisorJob() + dispatchers.engine).launch {
            engine.observeAnalysis().collectLatest { r ->
                if (activeHash == hash) {
                    _analysis.value = r
                    _state.value = CoordinatorState.Ready
                }
            }
        }
        engine.startAnalysis(AnalysisRequest(p, limit, multiPv, priority))
    }

    suspend fun stop() {
        collector?.cancelAndJoin()
        collector = null
        engine.stopAnalysis()
        if (_state.value !is CoordinatorState.Error) _state.value = CoordinatorState.Ready
    }

    suspend fun shutdown() {
        stop()
        engine.shutdown()
        engineWatcher?.cancel()
        _state.value = CoordinatorState.Stopped
    }
}

sealed interface CoordinatorState {
    data object Idle : CoordinatorState
    data class Analyzing(val id: String) : CoordinatorState
    data object Ready : CoordinatorState
    data object Stopped : CoordinatorState
    data class Error(val reason: String) : CoordinatorState
}