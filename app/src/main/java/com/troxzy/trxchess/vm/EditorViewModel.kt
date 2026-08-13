package com.troxzy.trxchess.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.troxzy.trxchess.chess.ChessPosition
import com.troxzy.trxchess.chess.Fen
import com.troxzy.trxchess.chess.Piece
import com.troxzy.trxchess.chess.PieceType
import com.troxzy.trxchess.chess.Side
import com.troxzy.trxchess.chess.Square
import com.troxzy.trxchess.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorUiState(
    val position: ChessPosition = ChessPosition.start(),
    val fenText: String = "",
    val selectedPiece: Piece = Piece(Side.WHITE, PieceType.QUEEN),
    val whiteSide: Boolean = true,
    val fenError: String? = null,
)

/**
 * Board editor state: place pieces, clear, load FEN.
 */
class EditorViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    fun setPieceType(type: PieceType) {
        val s = _state.value
        _state.value = s.copy(selectedPiece = Piece(if (s.whiteSide) Side.WHITE else Side.BLACK, type))
    }

    fun setSide(white: Boolean) {
        val s = _state.value
        _state.value = s.copy(
            whiteSide = white,
            selectedPiece = Piece(if (white) Side.WHITE else Side.BLACK, s.selectedPiece.type),
        )
    }

    fun toggleSquare(square: Square) {
        val s = _state.value
        val current = s.position.board[square]
        val position = if (current != null && current.side == s.selectedPiece.side && current.type == s.selectedPiece.type) {
            // tap same piece -> remove
            s.position.copy(board = s.position.board - square)
        } else {
            s.position.copy(board = s.position.board + (square to s.selectedPiece))
        }
        _state.value = s.copy(position = position, fenText = Fen.serialize(position), fenError = null)
    }

    fun clearBoard() {
        val s = _state.value
        val position = ChessPosition.fromFen("8/8/8/8/8/8/8/8 w - - 0 1")
        _state.value = s.copy(position = position, fenText = Fen.serialize(position), fenError = null)
    }

    fun startPosition() {
        val s = _state.value
        val position = ChessPosition.start()
        _state.value = s.copy(position = position, fenText = Fen.serialize(position), fenError = null)
    }

    fun loadFen(fen: String): Boolean {
        val position = runCatching { ChessPosition.fromFen(fen) }.getOrNull()
        if (position == null) {
            _state.value = _state.value.copy(fenError = "Invalid FEN")
            return false
        }
        _state.value = _state.value.copy(position = position, fenText = fen, fenError = null)
        return true
    }

    fun currentFen(): String = _state.value.fenText

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditorViewModel(container) as T
        }
    }
}