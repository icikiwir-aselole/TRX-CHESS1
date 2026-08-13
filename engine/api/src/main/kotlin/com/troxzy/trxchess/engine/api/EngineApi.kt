package com.troxzy.trxchess.engine.api

import com.troxzy.trxchess.chess.ChessPosition
import kotlinx.coroutines.flow.Flow

data class EngineConfig(val threads:Int=1,val hashMb:Int=64,val multiPv:Int=1,val moveOverheadMs:Int=30)
data class AnalysisRequest(val position:ChessPosition,val limit:SearchLimit=SearchLimit.Depth(14),val multiPv:Int=1,val priority:Priority=Priority.INTERACTIVE)
sealed interface SearchLimit { data class Depth(val value:Int):SearchLimit; data class TimeMs(val value:Long):SearchLimit; data class Nodes(val value:Long):SearchLimit }
enum class Priority { CRITICAL, INTERACTIVE, BACKGROUND, LOW }
sealed interface EngineState { data object Uninitialized:EngineState; data object Initializing:EngineState; data object Ready:EngineState; data object Analyzing:EngineState; data object Stopping:EngineState; data object Crashed:EngineState; data object Failed:EngineState; data object Shutdown:EngineState }
sealed interface Evaluation { data class Centipawn(val value:Int):Evaluation; data class Mate(val plies:Int):Evaluation; data class LowerBound(val score:Evaluation):Evaluation; data class UpperBound(val score:Evaluation):Evaluation }
data class EngineLine(val multiPv:Int,val evaluation:Evaluation,val depth:Int,val nodes:Long,val nps:Long,val pv:List<String>)
data class EngineAnalysis(val requestId:String,val positionKey:String,val lines:List<EngineLine>,val timestampMs:Long)
sealed interface EngineResult { data object Ok:EngineResult; data class Error(val message:String,val cause:Throwable?=null):EngineResult }
interface ChessEngine { suspend fun initialize(config:EngineConfig):EngineResult; suspend fun setPosition(position:ChessPosition):EngineResult; suspend fun startAnalysis(request:AnalysisRequest); suspend fun stopAnalysis(); suspend fun shutdown(); fun observeState():Flow<EngineState>; fun observeAnalysis():Flow<EngineAnalysis>; }
