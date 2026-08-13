package com.troxzy.trxchess.chess

import org.junit.Assert.*
import org.junit.Test
class ChessTest {
  @Test fun startHasTwentyMoves(){ assertEquals(20, ChessPosition.start().legalMoves().size) }
  @Test fun fenRoundTrip(){ val p=ChessPosition.start(); assertEquals(Fen.serialize(p), Fen.serialize(ChessPosition.fromFen(Fen.serialize(p)))) }
  @Test fun e4IsLegal(){ val p=ChessPosition.start(); assertNotNull(Notation.findMove(p,"e2e4")) }
}
