package com.troxzy.trxchess.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * King capture is not a legal chess move: pseudo moves may point at the
 * enemy king, but legalMoves() must never include them (including via
 * promotion captures).
 */
class KingCaptureTest {

    @Test
    fun `adjacent king cannot be captured`() {
        val p = ChessPosition.fromFen("8/8/8/8/8/4k3/4K3/8 w - - 0 1")
        val moves = p.legalMoves()
        assertTrue(moves.none { it.to == Square(4, 2) }) // e3
        assertTrue(moves.any { it.from == Square(4, 1) && it.to == Square(3, 0) }) // Kd1 still legal
        assertTrue(moves.any { it.from == Square(4, 1) && it.to == Square(4, 0) }) // Ke1 still legal
    }

    @Test
    fun `pawn cannot capture king onto last rank via promotion`() {
        val p = ChessPosition.fromFen("1k6/P7/8/8/8/8/8/4K3 w - - 0 1")
        val moves = p.legalMoves()
        assertTrue(moves.none { it.from == Square(0, 6) && it.to == Square(1, 7) }) // a7xb8
        assertTrue(moves.any { it.from == Square(0, 6) && it.to == Square(0, 7) && it.promotion == PieceType.QUEEN })
    }

    @Test
    fun `knight cannot capture king`() {
        val p = ChessPosition.fromFen("8/5k2/8/6N1/8/8/8/4K3 w - - 0 1")
        val moves = p.legalMoves()
        assertFalse(moves.any { it.to == Square(5, 6) }) // f7
        assertTrue(moves.any { it.from == Square(6, 4) && it.to == Square(7, 6) }) // Nh7 still legal
    }

    @Test
    fun `capturing the king never lands a checkmate status`() {
        val p = ChessPosition.fromFen("8/8/8/8/8/4k3/4K3/8 w - - 0 1")
        assertEquals(PositionStatus.CHECK, p.status())
    }
}