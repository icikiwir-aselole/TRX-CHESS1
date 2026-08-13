package com.troxzy.trxchess.chess

object Notation {
    fun findMove(p:ChessPosition, uci:String):Move? = p.legalMoves().firstOrNull{it.toString()==uci.trim().lowercase()}
    fun applyUci(p:ChessPosition, uci:String):ChessPosition = p.legalMoves().firstOrNull{it.toString()==uci.trim().lowercase()}?.let(p::apply) ?: error("Illegal move: $uci")
}
