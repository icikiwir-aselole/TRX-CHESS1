package com.troxzy.trxchess.chess

object Fen {
    fun serialize(p: ChessPosition): String {
        val rows=(7 downTo 0).joinToString("/"){r-> var empty=0; buildString { for(f in 0..7){val pc=p.board[Square(f,r)]; if(pc==null) empty++ else {if(empty>0){append(empty);empty=0};append(symbol(pc))} };if(empty>0)append(empty)}}
        return listOf(rows,if(p.sideToMove==Side.WHITE)"w" else "b",if(p.castling.isEmpty())"-" else p.castling.joinToString(""),p.enPassant?.toString()?:"-",p.halfmove,p.fullmove).joinToString(" ")
    }
    private fun symbol(p:Piece):Char{val c=when(p.type){PieceType.PAWN->'p';PieceType.KNIGHT->'n';PieceType.BISHOP->'b';PieceType.ROOK->'r';PieceType.QUEEN->'q';PieceType.KING->'k'};return if(p.side==Side.WHITE)c.uppercaseChar() else c}
}
