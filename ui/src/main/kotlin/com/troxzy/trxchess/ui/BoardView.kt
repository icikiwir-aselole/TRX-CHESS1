package com.troxzy.trxchess.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.troxzy.trxchess.chess.*

class BoardView(context:android.content.Context):View(context){
 private val light=Paint().apply{color=0xffeee0c0.toInt()};private val dark=Paint().apply{color=0xff7a654b.toInt()};private val text=Paint().apply{color=0xff101418.toInt();textAlign=Paint.Align.CENTER;textSize=42f};var position=ChessPosition.start();set(v){field=v;invalidate()}
 override fun onDraw(c:Canvas){super.onDraw(c);val s=width.coerceAtMost(height)/8f;for(r in 0..7)for(f in 0..7){c.drawRect(f*s,r*s,(f+1)*s,(r+1)*s,if((r+f)%2==0)light else dark);val p=position.board[Square(f,7-r)] ?: continue;text.color=if(p.side==Side.WHITE)0xfff8f8f8.toInt() else 0xff171717.toInt();text.setShadowLayer(3f,1f,1f,0x88000000.toInt());c.drawText(symbol(p),f*s+s/2,r*s+s*.67f,text);text.clearShadowLayer()}}
 private fun symbol(p:Piece)=when(p.type){PieceType.PAWN->"♟";PieceType.KNIGHT->"♞";PieceType.BISHOP->"♝";PieceType.ROOK->"♜";PieceType.QUEEN->"♛";PieceType.KING->"♚"}
}
