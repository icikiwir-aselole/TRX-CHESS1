package com.troxzy.trxchess.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FenParserTest {

    private val start = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    @Test
    fun `valid start position parses`() {
        val r = Fen.parseStrict(start)
        assertTrue(r is Fen.FenResult.Ok)
        assertEquals(20, (r as Fen.FenResult.Ok).position.legalMoves().size)
    }

    @Test
    fun `valid custom position with all fields parses`() {
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq e6 2 7"
        val r = Fen.parseStrict(fen)
        assertTrue(r is Fen.FenResult.Ok)
        val p = (r as Fen.FenResult.Ok).position
        assertEquals(Side.WHITE, p.sideToMove)
        assertEquals(2, p.halfmove)
        assertEquals(7, p.fullmove)
        assertEquals(Square(4, 5), p.enPassant)
        assertEquals(setOf("K", "Q", "k", "q"), p.castling)
    }

    @Test
    fun `optional clocks default safely`() {
        assertTrue(Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -") is Fen.FenResult.Ok)
        assertTrue(Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - -") is Fen.FenResult.Ok)
    }

    @Test
    fun `too many fields rejected`() {
        val r = Fen.parseStrict("$start extra")
        assertEquals(Fen.FenError.INVALID_FIELD_COUNT, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `too few fields rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w")
        assertEquals(Fen.FenError.INVALID_FIELD_COUNT, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `missing ranks rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1")
        assertEquals(Fen.FenError.INVALID_BOARD, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `rank not summing to eight rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(Fen.FenError.INVALID_BOARD, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `zero digit rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPP0PPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(Fen.FenError.INVALID_BOARD, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `invalid piece rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQXBNR w KQkq - 0 1")
        assertEquals(Fen.FenError.INVALID_PIECE, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `missing kings rejected as illegal board`() {
        val r = Fen.parseStrict("rnbq1bnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1")
        assertEquals(Fen.FenError.INVALID_BOARD, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `invalid side to move rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1")
        assertEquals(Fen.FenError.INVALID_SIDE_TO_MOVE, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `invalid castling rights rejected`() {
        assertEquals(
            Fen.FenError.INVALID_CASTLING_RIGHTS,
            (Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w X - 0 1") as Fen.FenResult.Err).error,
        )
        assertEquals(
            Fen.FenError.INVALID_CASTLING_RIGHTS,
            (Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KK - 0 1") as Fen.FenResult.Err).error,
        )
    }

    @Test
    fun `invalid en passant rejected`() {
        assertEquals(
            Fen.FenError.INVALID_EN_PASSANT,
            (Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e9 0 1") as Fen.FenResult.Err).error,
        )
        assertEquals(
            Fen.FenError.INVALID_EN_PASSANT,
            (Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e 0 1") as Fen.FenResult.Err).error,
        )
    }

    @Test
    fun `negative halfmove rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - -1 1")
        assertEquals(Fen.FenError.INVALID_HALFMOVE_CLOCK, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `non numeric halfmove rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - x 1")
        assertEquals(Fen.FenError.INVALID_HALFMOVE_CLOCK, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `zero fullmove rejected`() {
        val r = Fen.parseStrict("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 0")
        assertEquals(Fen.FenError.INVALID_FULLMOVE_NUMBER, (r as Fen.FenResult.Err).error)
    }

    @Test
    fun `leading and trailing whitespace tolerated`() {
        assertTrue(Fen.parseStrict("  $start  ") is Fen.FenResult.Ok)
    }

    @Test
    fun `empty string rejected as field count`() {
        assertEquals(Fen.FenError.INVALID_FIELD_COUNT, (Fen.parseStrict("") as Fen.FenResult.Err).error)
    }
}