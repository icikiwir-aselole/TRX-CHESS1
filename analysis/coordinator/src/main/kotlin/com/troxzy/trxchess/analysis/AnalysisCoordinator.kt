package com.troxzy.trxchess.analysis
import com.troxzy.trxchess.chess.*
import com.troxzy.trxchess.core.common.*
import com.troxzy.trxchess.engine.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
class AnalysisCoordinator(private val engine:ChessEngine,private val dispatchers:AppDispatchers=AppDispatchers()){
 private val _state=MutableStateFlow<CoordinatorState>(CoordinatorState.Idle);val state=_state.asStateFlow();private val _analysis=MutableStateFlow<EngineAnalysis?>(null);val analysis=_analysis.asStateFlow();private var collector:Job?=null;private var activeHash:String?=null
 suspend fun initialize(c:EngineConfig)=engine.initialize(c)
 suspend fun analyze(p:ChessPosition,limit:SearchLimit,multiPv:Int=1,priority:Priority=Priority.INTERACTIVE){stop();val hash=Fen.serialize(p).hashCode().toString(16);activeHash=hash;_state.value=CoordinatorState.Analyzing(UUID.randomUUID().toString());collector=CoroutineScope(SupervisorJob()+dispatchers.engine).launch{engine.observeAnalysis().collectLatest{r->if(activeHash==hash){_analysis.value=r;_state.value=CoordinatorState.Ready}}};engine.startAnalysis(AnalysisRequest(p,limit,multiPv,priority))}
 suspend fun stop(){collector?.cancelAndJoin();collector=null;engine.stopAnalysis();if(_state.value !is CoordinatorState.Error)_state.value=CoordinatorState.Ready}
 suspend fun shutdown(){stop();engine.shutdown();_state.value=CoordinatorState.Stopped}
}
sealed interface CoordinatorState{data object Idle:CoordinatorState;data class Analyzing(val id:String):CoordinatorState;data object Ready:CoordinatorState;data object Stopped:CoordinatorState;data class Error(val reason:String):CoordinatorState}
