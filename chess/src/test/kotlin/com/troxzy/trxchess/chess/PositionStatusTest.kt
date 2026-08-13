package com.troxzy.trxchess.chess

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionStatusTest {

    @Test
    fun `start position is normal`() {
        assertEquals(PositionStatus.NORMAL, ChessPosition.start().status())
    }

    @Test
    fun `position under check is check`() {
        val p = ChessPosition.fromFen("4k3/8/8/8/8/8/4r3/4K3 w - - 0 1")
        assertEquals(PositionStatus.CHECK, p.status())
    }

    @Test
    fun `back rank mate is checkmate`() {
        val p = ChessPosition.fromFen("4R2k/5ppp/8/8/8/8/8/4K3 b - - 0 1")
        assertEquals(PositionStatus.CHECKMATE, p.status())
    }

    @Test
    fun `stalemate with no legal moves and not in check`() {
        val p = ChessPosition.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
        assertEquals(PositionStatus.STALEMATE, p.status())
    }

    @Test
    fun `checkmate status does not throw with pinned defenders`() {
        val p = ChessPosition.fromFen("6k1/5ppp/8/8/8/8/5PPP/6K1 w - - 0 1")
        assertEquals(PositionStatus.NORMAL, p.status())
    }
}