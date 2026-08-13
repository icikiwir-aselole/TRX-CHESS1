package com.troxzy.trxchess.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.troxzy.trxchess.analysis.AnalysisCoordinator
import com.troxzy.trxchess.analysis.CoordinatorState
import com.troxzy.trxchess.chess.ChessPosition
import com.troxzy.trxchess.chess.Fen
import com.troxzy.trxchess.chess.Move
import com.troxzy.trxchess.chess.PieceType
import com.troxzy.trxchess.chess.PositionStatus
import com.troxzy.trxchess.chess.Square
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.engine.api.EngineAnalysis
import com.troxzy.trxchess.engine.api.EngineResult
import com.troxzy.trxchess.engine.api.EngineState
import com.troxzy.trxchess.engine.api.Evaluation
import com.troxzy.trxchess.engine.api.SearchLimit
import com.troxzy.trxchess.overlay.OverlayPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class AnalysisUiState(
    val position: ChessPosition = ChessPosition.start(),
    val history: List<Move> = emptyList(),
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val status: PositionStatus = PositionStatus.NORMAL,
    /** Base move awaiting a promotion piece choice; non-null shows the dialog. */
    val pendingPromotion: Move? = null,
    val engineState: EngineState = EngineState.Uninitialized,
    val coordinatorState: CoordinatorState = CoordinatorState.Idle,
    val analysis: EngineAnalysis? = null,
    val depth: Int = 0,
    val nodes: Long = 0,
    val nps: Long = 0,
    val multiPv: Int = 1,
    val flipped: Boolean = false,
    val engineError: String? = null,
    val thinking: Boolean = false,
    val fenError: Fen.FenError? = null,
)

/**
 * Analysis screen state.
 *
 * Owns the position history, selection, legal moves, promotion flow and the
 * engine interaction through the coordinator. Board views render this state
 * and forward taps back here; no engine logic lives in the view.
 */
class AnalysisViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val coordinator: AnalysisCoordinator = container.coordinator
    private val settings = container.settings

    private val _state = MutableStateFlow(AnalysisUiState())
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    private val publisher = OverlayPublisher(container.overlay, container.appScope)

    private var engineInitialized = false

    private var fenLoadGeneration = 0L

    init {
        viewModelScope.launch {
            coordinator.state.collect { coordState ->
                _state.value = _state.value.copy(coordinatorState = coordState)
            }
        }
        viewModelScope.launch {
            coordinator.analysis.sample(100).collect { result ->
                val best = result?.lines?.firstOrNull()
                _state.value = _state.value.copy(
                    analysis = result,
                    depth = best?.depth ?: 0,
                    nodes = best?.nodes ?: 0,
                    nps = best?.nps ?: 0,
                    thinking = false,
                )
            }
        }
        viewModelScope.launch {
            container.coordinator.engineState.collect { engineState ->
                _state.value = _state.value.copy(engineState = engineState)
            }
        }
    }

    fun bindOverlay() {
        publisher.bind(container.coordinator.engineState, container.coordinator.analysis)
    }

    override fun onCleared() {
        publisher.stop()
        viewModelScope.launch { coordinator.stop() }
        super.onCleared()
    }

    /** Strict, off-main-thread FEN import. Stale engine results are rejected by the coordinator. */
    fun loadFen(fen: String) {
        val generation = ++fenLoadGeneration
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { Fen.parseStrict(fen.trim()) }
            if (generation != fenLoadGeneration) return@launch
            when (result) {
                is Fen.FenResult.Err -> {
                    _state.value = _state.value.copy(fenError = result.error)
                }
                is Fen.FenResult.Ok -> {
                    val position = result.position
                    _state.value = _state.value.copy(
                        position = position,
                        history = emptyList(),
                        selected = null,
                        legalTargets = emptySet(),
                        pendingPromotion = null,
                        status = position.status(),
                        analysis = null,
                        depth = 0,
                        nodes = 0,
                        nps = 0,
                        thinking = false,
                        engineError = null,
                        fenError = null,
                    )
                    coordinator.beginSession(UUID.randomUUID().toString())
                    viewModelScope.launch {
                        container.history.save(Fen.serialize(position), "FEN session")
                    }
                }
            }
        }
    }

    fun newGame() {
        viewModelScope.launch { coordinator.stop() }
        val position = ChessPosition.start()
        _state.value = _state.value.copy(
            position = position,
            history = emptyList(),
            selected = null,
            legalTargets = emptySet(),
            pendingPromotion = null,
            status = position.status(),
            analysis = null,
            depth = 0,
            nodes = 0,
            nps = 0,
            thinking = false,
            fenError = null,
        )
        coordinator.beginSession(UUID.randomUUID().toString())
    }

    fun flip() {
        _state.value = _state.value.copy(flipped = !_state.value.flipped)
    }

    fun onSquareTap(square: Square) {
        val s = _state.value
        val piece = s.position.board[square] ?: return
        if (piece.side != s.position.sideToMove) return
        val targets = s.position.legalMoves().filter { it.from == square }.map { it.to }.toSet()
        _state.value = s.copy(selected = square, legalTargets = targets)
    }

    fun onMovePlayed(move: Move) {
        val s = _state.value
        if (s.pendingPromotion != null) return
        val isPromotion = s.position.legalMoves().any {
            it.from == move.from && it.to == move.to && it.promotion != null
        }
        if (isPromotion) {
            _state.value = s.copy(pendingPromotion = move, selected = null, legalTargets = emptySet())
            return
        }
        applyMove(move)
    }

    fun choosePromotion(type: PieceType) {
        val s = _state.value
        val base = s.pendingPromotion ?: return
        val move = s.position.legalMoves().firstOrNull {
            it.from == base.from && it.to == base.to && it.promotion == type
        } ?: return
        applyMove(move)
    }

    fun cancelPromotion() {
        val s = _state.value
        if (s.pendingPromotion == null) return
        _state.value = s.copy(pendingPromotion = null, selected = null, legalTargets = emptySet())
    }

    private fun applyMove(move: Move) {
        val s = _state.value
        val position = runCatching { s.position.apply(move) }.getOrNull() ?: return
        // apply() is a no-op when the move is not legal on the current
        // position (e.g. a duplicate rapid tap); never record it.
        if (position == s.position) return
        _state.value = s.copy(
            position = position,
            history = s.history + move,
            selected = null,
            legalTargets = emptySet(),
            pendingPromotion = null,
            status = position.status(),
            thinking = false,
        )
        coordinator.beginSession(UUID.randomUUID().toString())
        viewModelScope.launch { coordinator.stop() }
    }

    fun undo() {
        val s = _state.value
        if (s.history.isEmpty()) return
        val history = s.history.dropLast(1)
        val position = history.fold(ChessPosition.start()) { acc, m -> acc.apply(m) }
        _state.value = s.copy(
            position = position,
            history = history,
            selected = null,
            legalTargets = emptySet(),
            pendingPromotion = null,
            status = position.status(),
            thinking = false,
        )
        coordinator.beginSession(UUID.randomUUID().toString())
        viewModelScope.launch { coordinator.stop() }
    }

    fun startAnalysis(depth: Int? = null) {
        val limit = SearchLimit.Depth(depth ?: settings.settings.value.defaultDepth)
        val multiPv = settings.settings.value.multiPv
        viewModelScope.launch {
            val s0 = _state.value
            if (s0.position.legalMoves().isEmpty()) {
                _state.value = s0.copy(engineError = "No legal moves", thinking = false)
                return@launch
            }
            _state.value = s0.copy(thinking = true, engineError = null)
            ensureEngineReady()
            // A move/undo/stop between the press and engine readiness sets
            // thinking=false; never analyze a stale snapshot.
            val s = _state.value
            if (!s.thinking) return@launch
            coordinator.analyze(s.position, limit, multiPv)
        }
    }

    fun stopAnalysis() {
        viewModelScope.launch { coordinator.stop() }
        _state.value = _state.value.copy(thinking = false)
    }

    private suspend fun ensureEngineReady() {
        if (engineInitialized) return
        val result = coordinator.initialize(container.engineConfig())
        if (result is EngineResult.Error) {
            _state.value = _state.value.copy(engineError = result.message)
        } else {
            engineInitialized = true
        }
    }

    fun retryEngine() {
        engineInitialized = false
        viewModelScope.launch {
            coordinator.initialize(container.engineConfig())
        }
    }

    fun formatEvaluation(ev: Evaluation?): String = when (ev) {
        null -> "—"
        is Evaluation.Mate -> "M${ev.plies}"
        is Evaluation.Centipawn -> "%+.2f".format(ev.value / 100.0)
        is Evaluation.LowerBound -> formatEvaluation(ev.score)
        is Evaluation.UpperBound -> formatEvaluation(ev.score)
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AnalysisViewModel(container) as T
        }
    }
}