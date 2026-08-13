package com.troxzy.trxchess.chess

/**
 * FEN parsing and serialization.
 *
 * Parsing is strict and categorized: callers receive a structured
 * [FenError] instead of a raw exception, so the UI can render a
 * human-readable message per failure class. All fields are validated and
 * bounded (board layout, piece symbols, side to move, castling rights,
 * en-passant square, clocks).
 */
object Fen {

    /** Human-facing error categories for malformed FEN input. */
    enum class FenError {
        INVALID_FIELD_COUNT,
        INVALID_BOARD,
        INVALID_PIECE,
        INVALID_SIDE_TO_MOVE,
        INVALID_CASTLING_RIGHTS,
        INVALID_EN_PASSANT,
        INVALID_HALFMOVE_CLOCK,
        INVALID_FULLMOVE_NUMBER,
    }

    sealed interface FenResult {
        data class Ok(val position: ChessPosition) : FenResult
        data class Err(val error: FenError) : FenResult
    }

    /**
     * Strict parse. Leading/trailing whitespace is trimmed and multiple
     * internal spaces are collapsed before field splitting.
     */
    fun parseStrict(fen: String): FenResult {
        val fields = fen.trim().split(Regex("\\s+"))
        if (fields.size !in 4..6) return FenResult.Err(FenError.INVALID_FIELD_COUNT)

        val board = parseBoard(fields[0]) ?: return boardError(fields[0])
        if (board.values.none { it.type == PieceType.KING }) {
            return FenResult.Err(FenError.INVALID_BOARD)
        }

        val stm = when (fields[1]) {
            "w" -> Side.WHITE
            "b" -> Side.BLACK
            else -> return FenResult.Err(FenError.INVALID_SIDE_TO_MOVE)
        }

        val castling = fields[2]
        if (castling != "-") {
            if (castling.any { it !in "KQkq" }) return FenResult.Err(FenError.INVALID_CASTLING_RIGHTS)
            if (castling.groupingBy { it }.eachCount().any { it.value > 1 }) {
                return FenResult.Err(FenError.INVALID_CASTLING_RIGHTS)
            }
        }
        val castlingSet = castling.filter { it != '-' }.map { it.toString() }.toSet()

        val ep: Square? = if (fields[3] == "-") {
            null
        } else {
            if (fields[3].length != 2) return FenResult.Err(FenError.INVALID_EN_PASSANT)
            val file = fields[3][0].code - 'a'.code
            val rank = fields[3][1].code - '1'.code
            if (file !in 0..7 || rank !in 0..7) return FenResult.Err(FenError.INVALID_EN_PASSANT)
            Square(file, rank)
        }

        val halfmove = if (fields.size >= 5) {
            fields[4].toIntOrNull()?.takeIf { it >= 0 }
                ?: return FenResult.Err(FenError.INVALID_HALFMOVE_CLOCK)
        } else {
            0
        }

        val fullmove = if (fields.size >= 6) {
            fields[5].toIntOrNull()?.takeIf { it >= 1 }
                ?: return FenResult.Err(FenError.INVALID_FULLMOVE_NUMBER)
        } else {
            1
        }

        return FenResult.Ok(ChessPosition(board, stm, castlingSet, ep, halfmove, fullmove))
    }

    private fun boardError(row: String): FenResult.Err = when {
        row.any { c -> c != '/' && !c.isDigit() && c.lowercaseChar() !in "pnbrqk" } ->
            FenResult.Err(FenError.INVALID_PIECE)
        else -> FenResult.Err(FenError.INVALID_BOARD)
    }

    private fun parseBoard(ranks: String): Map<Square, Piece>? {
        val rows = ranks.split('/')
        if (rows.size != 8) return null
        val board = mutableMapOf<Square, Piece>()
        rows.forEachIndexed { ri, row ->
            var f = 0
            for (c in row) {
                when {
                    c.isDigit() -> {
                        val d = c.digitToIntOrNull() ?: return null
                        if (d == 0 || f + d > 8) return null
                        f += d
                    }
                    c == '/' -> return null
                    else -> {
                        val type = when (c.lowercaseChar()) {
                            'p' -> PieceType.PAWN
                            'n' -> PieceType.KNIGHT
                            'b' -> PieceType.BISHOP
                            'r' -> PieceType.ROOK
                            'q' -> PieceType.QUEEN
                            'k' -> PieceType.KING
                            else -> return null
                        }
                        if (f >= 8) return null
                        val side = if (c.isUpperCase()) Side.WHITE else Side.BLACK
                        board[Square(f, 7 - ri)] = Piece(side, type)
                        f++
                    }
                }
            }
            if (f != 8) return null
        }
        return board
    }

    fun serialize(p: ChessPosition): String {
        val rows = (7 downTo 0).map { rank ->
            var empty = 0
            val sb = StringBuilder()
            for (file in 0..7) {
                val piece = p.board[Square(file, rank)]
                if (piece == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(symbol(piece))
                }
            }
            if (empty > 0) sb.append(empty)
            sb.toString()
        }
        val castling = if (p.castling.isEmpty()) "-" else p.castling.joinToString("")
        val ep = p.enPassant?.toString() ?: "-"
        return "${rows.joinToString("/")} ${if (p.sideToMove == Side.WHITE) "w" else "b"} $castling $ep ${p.halfmove} ${p.fullmove}"
    }

    private fun symbol(p: Piece): Char {
        val c = when (p.type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
        }
        return if (p.side == Side.WHITE) c.uppercaseChar() else c
    }
}