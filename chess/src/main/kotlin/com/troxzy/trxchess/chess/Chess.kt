package com.troxzy.trxchess.chess

enum class Side { WHITE, BLACK; fun other() = if (this == WHITE) BLACK else WHITE }
enum class PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
enum class PositionStatus { NORMAL, CHECK, CHECKMATE, STALEMATE }
data class Piece(val side: Side, val type: PieceType)
data class Square(val file: Int, val rank: Int) { init { require(file in 0..7 && rank in 0..7) }; override fun toString() = "${('a'.code+file).toChar()}${rank+1}"; companion object { fun parse(s:String)=Square(s[0]-'a',s[1]-'1') } }
data class Move(val from: Square, val to: Square, val promotion: PieceType? = null, val isEnPassant: Boolean = false, val isCastle: Boolean = false) { override fun toString() = from.toString()+to.toString()+(promotion?.let{it.name.lowercase().first()} ?: "") }
data class ChessPosition(val board: Map<Square,Piece>, val sideToMove: Side, val castling: Set<String>, val enPassant: Square?, val halfmove: Int, val fullmove: Int) {
    fun pieceAt(s:Square)=board[s]
    fun isSquareAttacked(target:Square, by:Side):Boolean {
        for ((s,p) in board) if (p.side==by) {
            val df=target.file-s.file; val dr=target.rank-s.rank
            val adf=kotlin.math.abs(df); val adr=kotlin.math.abs(dr)
            when(p.type){
                PieceType.PAWN -> if (dr==(if(by==Side.WHITE)1 else -1) && adf==1) return true
                PieceType.KNIGHT -> if ((adf==1&&adr==2)||(adf==2&&adr==1)) return true
                PieceType.KING -> if (adf<=1&&adr<=1) return true
                PieceType.BISHOP,PieceType.ROOK,PieceType.QUEEN -> {
                    val ok = when(p.type){
                        PieceType.BISHOP -> adf==adr && adf>0
                        PieceType.ROOK -> (df==0 || dr==0) && (df!=0 || dr!=0)
                        else -> (adf==adr && adf>0) || ((df==0 || dr==0) && (df!=0 || dr!=0))
                    }
                    if(ok){ val sf=Integer.signum(df); val sr=Integer.signum(dr); var f=s.file+sf; var r=s.rank+sr; var blocked=false; while(f!=target.file||r!=target.rank){ if(board[Square(f,r)]!=null){blocked=true;break}; f+=sf;r+=sr }; if(!blocked) return true }
                }
            }
        }
        return false
    }
    fun inCheck(side:Side)=board.entries.firstOrNull{it.value.side==side&&it.value.type==PieceType.KING}?.key?.let{isSquareAttacked(it,side.other())} ?: true

    /** Terminal/status classification derived purely from rules. */
    fun status(): PositionStatus {
        val hasLegal = legalMoves().isNotEmpty()
        return when {
            inCheck(sideToMove) -> if (hasLegal) PositionStatus.CHECK else PositionStatus.CHECKMATE
            !hasLegal -> PositionStatus.STALEMATE
            else -> PositionStatus.NORMAL
        }
    }

    fun legalMoves():List<Move> = pseudoMoves().filter {
        val capturesKing = board[it.to]?.type == PieceType.KING
        !capturesKing && apply(it).let { !it.inCheck(sideToMove) }
    }
    fun apply(move:Move):ChessPosition {
        val mutable=board.toMutableMap(); val moving=mutable.remove(move.from) ?: return this; mutable.remove(move.to)
        if(move.isEnPassant){ val capRank=move.to.rank+(if(moving.side==Side.WHITE)-1 else 1); mutable.remove(Square(move.to.file,capRank)) }
        var piece=moving; if(move.promotion!=null) piece=Piece(moving.side,move.promotion); mutable[move.to]=piece
        if(move.isCastle){ val kingSide=move.to.file>move.from.file; val rf=if(kingSide)7 else 0; val rt=if(kingSide)5 else 3; val rr=move.from.rank; val rookFrom=Square(rf,rr); val rookTo=Square(rt,rr); mutable[rookTo]=mutable.remove(rookFrom)?:Piece(moving.side,PieceType.ROOK) }
        val newCast=castling.toMutableSet(); when(moving.type){ PieceType.KING->{newCast.remove(if(moving.side==Side.WHITE)"K" else "k");newCast.remove(if(moving.side==Side.WHITE)"Q" else "q")}; PieceType.ROOK->{ if(move.from==Square(0,0))newCast.remove("Q");if(move.from==Square(7,0))newCast.remove("K");if(move.from==Square(0,7))newCast.remove("q");if(move.from==Square(7,7))newCast.remove("k")}; else->{} }
        board[move.to]?.let { if(it.type==PieceType.ROOK){ if(move.to==Square(0,0))newCast.remove("Q");if(move.to==Square(7,0))newCast.remove("K");if(move.to==Square(0,7))newCast.remove("q");if(move.to==Square(7,7))newCast.remove("k") } }
        val ep = if(moving.type==PieceType.PAWN && kotlin.math.abs(move.to.rank-move.from.rank)==2) Square(move.from.file,(move.to.rank+move.from.rank)/2) else null
        return copy(board=mutable.toMap(), sideToMove=sideToMove.other(), castling=newCast, enPassant=ep, halfmove=if(moving.type==PieceType.PAWN||board[move.to]!=null)0 else halfmove+1, fullmove=fullmove+(if(sideToMove==Side.BLACK)1 else 0))
    }
    private fun pseudoMoves():List<Move>{
        val out=mutableListOf<Move>()
        for((s,p) in board) if(p.side==sideToMove) when(p.type){
            PieceType.PAWN->{ val d=if(p.side==Side.WHITE)1 else -1; val start=if(p.side==Side.WHITE)1 else 6; val promo=if(p.side==Side.WHITE)7 else 0; val oneRank=s.rank+d; if(oneRank !in 0..7) continue; val one=Square(s.file,oneRank); if(board[one]==null){ if(one.rank==promo) listOf(PieceType.QUEEN,PieceType.ROOK,PieceType.BISHOP,PieceType.KNIGHT).forEach{out+=Move(s,one,it)} else out+=Move(s,one); if(s.rank==start){val two=Square(s.file,s.rank+2*d); if(board[two]==null)out+=Move(s,two)}}; for(df in intArrayOf(-1,1)){val tf=s.file+df; val tr=s.rank+d; if(tf !in 0..7 || tr !in 0..7) continue; val t=Square(tf,tr); if(t in board){ if(board[t]?.side!=p.side){if(t.rank==promo)listOf(PieceType.QUEEN,PieceType.ROOK,PieceType.BISHOP,PieceType.KNIGHT).forEach{out+=Move(s,t,it)} else out+=Move(s,t)} } else if(t==enPassant) out+=Move(s,t,isEnPassant=true)} }
            PieceType.KNIGHT-> addLeapers(s,p,intArrayOf(1,2,2,1,-1,-2,-2,-1),intArrayOf(2,1,-1,-2,-2,-1,1,2),out)
            PieceType.KING->{ addLeapers(s,p,intArrayOf(-1,0,1,-1,1,-1,0,1),intArrayOf(-1,-1,-1,0,0,1,1,1),out); val rank=if(p.side==Side.WHITE)0 else 7; val ks=if(p.side==Side.WHITE)"K" else "k"; val qs=if(p.side==Side.WHITE)"Q" else "q"; if(s==Square(4,rank)&&ks in castling && board[Square(5,rank)]==null&&board[Square(6,rank)]==null&&!inCheck(p.side)&&!isSquareAttacked(Square(5,rank),p.side.other())&&!isSquareAttacked(Square(6,rank),p.side.other()))out+=Move(s,Square(6,rank),isCastle=true); if(s==Square(4,rank)&&qs in castling && board[Square(1,rank)]==null&&board[Square(2,rank)]==null&&board[Square(3,rank)]==null&&!inCheck(p.side)&&!isSquareAttacked(Square(3,rank),p.side.other())&&!isSquareAttacked(Square(2,rank),p.side.other()))out+=Move(s,Square(2,rank),isCastle=true) }
            PieceType.BISHOP-> addSliders(s,p,intArrayOf(1,1,-1,-1),intArrayOf(1,-1,1,-1),out)
            PieceType.ROOK-> addSliders(s,p,intArrayOf(1,-1,0,0),intArrayOf(0,0,1,-1),out)
            PieceType.QUEEN-> addSliders(s,p,intArrayOf(1,1,-1,-1,1,-1,0,0),intArrayOf(1,-1,1,-1,0,0,1,-1),out)
        }
        return out
    }
    private fun addLeapers(s:Square,p:Piece,dfs:IntArray,drs:IntArray,out:MutableList<Move>){for(i in dfs.indices){val f=s.file+dfs[i];val r=s.rank+drs[i];if(f !in 0..7||r !in 0..7)continue;val q=board[Square(f,r)];if(q==null||q.side!=p.side)out+=Move(s,Square(f,r))}}
    private fun addSliders(s:Square,p:Piece,dfs:IntArray,drs:IntArray,out:MutableList<Move>){for(i in dfs.indices){var f=s.file+dfs[i];var r=s.rank+drs[i];while(f in 0..7&&r in 0..7){val sq=Square(f,r);val q=board[sq];if(q==null)out+=Move(s,sq) else {if(q.side!=p.side)out+=Move(s,sq);break};f+=dfs[i];r+=drs[i]}}}
    companion object {
        fun start()=fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        fun fromFen(fen:String):ChessPosition=when(val r=Fen.parseStrict(fen)){is Fen.FenResult.Ok->r.position;is Fen.FenResult.Err->error("Invalid FEN: ${r.error}")}
    }
}
