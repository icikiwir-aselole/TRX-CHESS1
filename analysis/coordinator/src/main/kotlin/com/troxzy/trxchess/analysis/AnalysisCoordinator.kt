package com.troxzy.trxchess.analysis

import com.troxzy.trxchess.chess.ChessPosition
import com.troxzy.trxchess.chess.Fen
import com.troxzy.trxchess.core.common.AppDispatchers
import com.troxzy.trxchess.engine.api.AnalysisRequest
import com.troxzy.trxchess.engine.api.ChessEngine
import com.troxzy.trxchess.engine.api.EngineAnalysis
import com.troxzy.trxchess.engine.api.EngineConfig
import com.troxzy.trxchess.engine.api.EngineResult
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
 * Stale-result protection: every analysis run records an identity made of
 * analysisId + positionHash + engineVersion + configHash + active session.
 * An engine result is applied only when ALL components match, so a late
 * result from a superseded search (or a re-initialized engine, or a changed
 * configuration) can never reach the UI. Timestamps alone are never used.
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
    private var currentMeta: AnalysisMeta? = null
    private var activeConfig: EngineConfig = EngineConfig()
    private var engineWatcher: Job? = null

    /** Identity of the active user session; changes invalidate in-flight results. */
    var sessionId: String = "default"
        private set

    init {
        engineWatcher = CoroutineScope(SupervisorJob() + dispatchers.engine).launch {
            engine.observeState().collect { _engineState.value = it }
        }
    }

    suspend fun initialize(c: EngineConfig): EngineResult = runCatching {
        val result = engine.initialize(c)
        activeConfig = c
        result
    }.getOrElse { EngineResult.Error("engine init failed", it) }

    /** Starts a new session; results tagged with an older session are rejected. */
    fun beginSession(id: String) {
        sessionId = id
    }

    suspend fun analyze(
        p: ChessPosition,
        limit: SearchLimit,
        multiPv: Int = 1,
        priority: Priority = Priority.INTERACTIVE,
    ) {
        stop()
        val hash = Fen.serialize(p).hashCode().toString(16)
        val analysisId = UUID.randomUUID().toString()
        val meta = AnalysisMeta(
            analysisId = analysisId,
            positionHash = hash,
            engineVersion = engine.version,
            configHash = activeConfig.configHash(),
            sessionId = sessionId,
        )
        currentMeta = meta
        _state.value = CoordinatorState.Analyzing(analysisId)
        collector = CoroutineScope(SupervisorJob() + dispatchers.engine).launch {
            engine.observeAnalysis().collectLatest { r ->
                if (isCurrent(r, meta)) {
                    _analysis.value = r
                    _state.value = CoordinatorState.Ready
                }
            }
        }
        engine.startAnalysis(
            AnalysisRequest(
                position = p,
                limit = limit,
                multiPv = multiPv,
                priority = priority,
                analysisId = analysisId,
            )
        )
    }

    /** Full identity match: analysisId, positionHash, engineVersion, configHash, session. */
    private fun isCurrent(r: EngineAnalysis, meta: AnalysisMeta): Boolean =
        r.requestId == meta.analysisId &&
            r.positionKey == meta.positionHash &&
            engine.version == meta.engineVersion &&
            activeConfig.configHash() == meta.configHash &&
            sessionId == meta.sessionId

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
        currentMeta = null
        _state.value = CoordinatorState.Stopped
    }
}

private data class AnalysisMeta(
    val analysisId: String,
    val positionHash: String,
    val engineVersion: String,
    val configHash: String,
    val sessionId: String,
)

sealed interface CoordinatorState {
    data object Idle : CoordinatorState
    data class Analyzing(val id: String) : CoordinatorState
    data object Ready : CoordinatorState
    data object Stopped : CoordinatorState
    data class Error(val reason: String) : CoordinatorState
}