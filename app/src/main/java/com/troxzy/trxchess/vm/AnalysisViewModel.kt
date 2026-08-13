package com.troxzy.trxchess.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.troxzy.trxchess.analysis.AnalysisCoordinator
import com.troxzy.trxchess.analysis.CoordinatorState
import com.troxzy.trxchess.chess.ChessPosition
import com.troxzy.trxchess.chess.Move
import com.troxzy.trxchess.chess.Square
import com.troxzy.trxchess.di.HistoryStore
import com.troxzy.trxchess.di.AppContainer
import com.troxzy.trxchess.engine.api.EngineAnalysis
import com.troxzy.trxchess.engine.api.EngineState
import com.troxzy.trxchess.engine.api.Evaluation
import com.troxzy.trxchess.engine.api.SearchLimit
import com.troxzy.trxchess.overlay.OverlayPublisher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val position: ChessPosition = ChessPosition.start(),
    val history: List<Move> = emptyList(),
    val selected: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
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
)

/**
 * Analysis screen state.
 *
 * Owns the position history, selection, legal moves and the engine
 * interaction through the coordinator. Board views render this state and
 * forward taps back here; no engine logic lives in the view.
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

    init {
        viewModelScope.launch {
            coordinator.state.collect { coordState ->
                _state.value = _state.value.copy(coordinatorState = coordState)
            }
        }
        viewModelScope.launch {
            coordinator.analysis.collect { result ->
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

    fun loadFen(fen: String?): Boolean {
        if (fen == null) return false
        val position = runCatching { ChessPosition.fromFen(fen) }.getOrNull() ?: return false
        _state.value = AnalysisUiState(
            position = position,
            engineState = _state.value.engineState,
            coordinatorState = _state.value.coordinatorState,
        )
        viewModelScope.launch {
            container.history.save(fen, "FEN session")
        }
        return true
    }

    fun newGame() {
        viewModelScope.launch { coordinator.stop() }
        _state.value = AnalysisUiState(
            position = ChessPosition.start(),
            engineState = _state.value.engineState,
        )
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
        val position = runCatching { s.position.apply(move) }.getOrNull() ?: return
        _state.value = s.copy(
            position = position,
            history = s.history + move,
            selected = null,
            legalTargets = emptySet(),
        )
        viewModelScope.launch { coordinator.stop() }
    }

    fun undo() {
        val s = _state.value
        if (s.history.isEmpty()) return
        val history = s.history.dropLast(1)
        val position = history.fold(ChessPosition.start()) { acc, m -> acc.apply(m) }
        _state.value = s.copy(position = position, history = history, selected = null, legalTargets = emptySet())
        viewModelScope.launch { coordinator.stop() }
    }

    fun startAnalysis(depth: Int? = null) {
        val s = _state.value
        if (s.position.legalMoves().isEmpty()) {
            _state.value = s.copy(engineError = "No legal moves")
            return
        }
        _state.value = s.copy(thinking = true, engineError = null)
        val limit = SearchLimit.Depth(depth ?: settings.settings.value.defaultDepth)
        val multiPv = settings.settings.value.multiPv
        viewModelScope.launch {
            ensureEngineReady()
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
        if (result is com.troxzy.trxchess.engine.api.EngineResult.Error) {
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