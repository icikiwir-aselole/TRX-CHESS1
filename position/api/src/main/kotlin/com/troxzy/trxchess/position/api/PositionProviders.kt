package com.troxzy.trxchess.position.api
import com.troxzy.trxchess.chess.ChessPosition
interface PositionProvider{val id:String;suspend fun currentPosition():ChessPosition?}
object ManualPositionProvider:PositionProvider{override val id="manual";private var value=ChessPosition.start();override suspend fun currentPosition()=value;fun set(p:ChessPosition){value=p}}
