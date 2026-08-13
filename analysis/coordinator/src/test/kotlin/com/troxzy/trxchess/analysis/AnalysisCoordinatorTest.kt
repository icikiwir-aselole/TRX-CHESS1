package com.troxzy.trxchess.analysis

import com.troxzy.trxchess.chess.ChessPosition
import com.troxzy.trxchess.chess.Fen
import com.troxzy.trxchess.core.common.AppDispatchers
import com.troxzy.trxchess.engine.api.AnalysisRequest
import com.troxzy.trxchess.engine.api.ChessEngine
import com.troxzy.trxchess.engine.api.EngineAnalysis
import com.troxzy.trxchess.engine.api.EngineConfig
import com.troxzy.trxchess.engine.api.EngineLine
import com.troxzy.trxchess.engine.api.EngineResult
import com.troxzy.trxchess.engine.api.EngineState
import com.troxzy.trxchess.engine.api.Evaluation
import com.troxzy.trxchess.engine.api.SearchLimit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Stale-result protection: a result is applied only when analysisId,
 * positionHash, engineVersion, configHash and session all match.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisCoordinatorTest {

    private class FakeEngine(
        override var version: String = "fake-1.0.0",
    ) : ChessEngine {
        val analysis = MutableSharedFlow<EngineAnalysis>(extraBufferCapacity = 64)
        private val state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
        val lastRequest = MutableStateFlow<AnalysisRequest?>(null)

        override suspend fun initialize(config: EngineConfig): EngineResult {
            state.value = EngineState.Ready
            return EngineResult.Ok
        }

        override suspend fun setPosition(position: ChessPosition): EngineResult = EngineResult.Ok
        override suspend fun startAnalysis(request: AnalysisRequest) {
            lastRequest.value = request
            state.value = EngineState.Analyzing
        }

        override suspend fun stopAnalysis() {
            if (state.value == EngineState.Analyzing) state.value = EngineState.Ready
        }

        override suspend fun shutdown() {
            state.value = EngineState.Shutdown
        }

        override fun observeState(): Flow<EngineState> = state
        override fun observeAnalysis(): Flow<EngineAnalysis> = analysis
    }

    private fun line(depth: Int = 8) = EngineLine(
        multiPv = 1,
        evaluation = Evaluation.Centipawn(30),
        depth = depth,
        nodes = 1000,
        nps = 100_000,
        pv = listOf("e2e4"),
    )

    private fun result(requestId: String, p: ChessPosition, ts: Long) = EngineAnalysis(
        requestId = requestId,
        positionKey = Fen.serialize(p).hashCode().toString(16),
        lines = listOf(line()),
        timestampMs = ts,
    )

    private val dispatcher = StandardTestDispatcher()

    private fun dispatchers(d: CoroutineDispatcher) = AppDispatchers(
        main = d,
        default = d,
        io = d,
        engine = d,
    )

    @Test
    fun `result with matching identity is applied`() = runTest(dispatcher) {
        val engine = FakeEngine()
        val coordinator = AnalysisCoordinator(engine, dispatchers(dispatcher))
        coordinator.initialize(EngineConfig())
        val start = ChessPosition.start()
        coordinator.analyze(start, SearchLimit.Depth(8))
        dispatcher.scheduler.advanceUntilIdle()

        val request = engine.lastRequest.value!!
        engine.analysis.emit(result(request.analysisId, start, 1))
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(coordinator.analysis.value)
        assertEquals(CoordinatorState.Ready, coordinator.state.value)
        coordinator.shutdown()
    }

    @Test
    fun `late result from previous search is rejected`() = runTest(dispatcher) {
        val engine = FakeEngine()
        val coordinator = AnalysisCoordinator(engine, dispatchers(dispatcher))
        coordinator.initialize(EngineConfig())

        val posA = ChessPosition.start()
        coordinator.analyze(posA, SearchLimit.Depth(8))
        dispatcher.scheduler.advanceUntilIdle()
        val reqA = engine.lastRequest.value!!

        val posB = ChessPosition.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
        coordinator.analyze(posB, SearchLimit.Depth(8))
        dispatcher.scheduler.advanceUntilIdle()

        engine.analysis.emit(result(reqA.analysisId, posA, 2))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(coordinator.analysis.value)
        coordinator.shutdown()
    }

    @Test
    fun `result with wrong position hash is rejected`() = runTest(dispatcher) {
        val engine = FakeEngine()
        val coordinator = AnalysisCoordinator(engine, dispatchers(dispatcher))
        coordinator.initialize(EngineConfig())
        val start = ChessPosition.start()
        coordinator.analyze(start, SearchLimit.Depth(8))
        dispatcher.scheduler.advanceUntilIdle()

        val request = engine.lastRequest.value!!
        val other = ChessPosition.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
        engine.analysis.emit(result(request.analysisId, other, 3))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(coordinator.analysis.value)
        coordinator.shutdown()
    }

    @Test
    fun `result after session change is rejected`() = runTest(dispatcher) {
        val engine = FakeEngine()
        val coordinator = AnalysisCoordinator(engine, dispatchers(dispatcher))
        coordinator.initialize(EngineConfig())
        val start = ChessPosition.start()
        coordinator.analyze(start, SearchLimit.Depth(8))
        dispatcher.scheduler.advanceUntilIdle()
        val request = engine.lastRequest.value!!

        coordinator.beginSession("session-2")
        engine.analysis.emit(result(request.analysisId, start, 4))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(coordinator.analysis.value)
        coordinator.shutdown()
    }

    @Test
    fun `result after engine version change is rejected`() = runTest(dispatcher) {
        val engine = FakeEngine()
        val coordinator = AnalysisCoordinator(engine, dispatchers(dispatcher))
        coordinator.initialize(EngineConfig())
        val start = ChessPosition.start()
        coordinator.analyze(start, SearchLimit.Depth(8))
        dispatcher.scheduler.advanceUntilIdle()
        val request = engine.lastRequest.value!!

        engine.version = "fake-2.0.0"
        engine.analysis.emit(result(request.analysisId, start, 5))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(coordinator.analysis.value)
        coordinator.shutdown()
    }

    @Test
    fun `result after config change is rejected`() = runTest(dispatcher) {
        val engine = FakeEngine()
        val coordinator = AnalysisCoordinator(engine, dispatchers(dispatcher))
        coordinator.initialize(EngineConfig(threads = 1, hashMb = 64))
        val start = ChessPosition.start()
        coordinator.analyze(start, SearchLimit.Depth(8))
        dispatcher.scheduler.advanceUntilIdle()
        val request = engine.lastRequest.value!!

        coordinator.initialize(EngineConfig(threads = 4, hashMb = 256))
        engine.analysis.emit(result(request.analysisId, start, 6))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(coordinator.analysis.value)
        coordinator.shutdown()
    }

    @Test
    fun `request carries the coordinator analysis id`() = runTest(dispatcher) {
        val engine = FakeEngine()
        val coordinator = AnalysisCoordinator(engine, dispatchers(dispatcher))
        coordinator.initialize(EngineConfig())
        coordinator.analyze(ChessPosition.start(), SearchLimit.Depth(8))
        dispatcher.scheduler.advanceUntilIdle()

        val request = engine.lastRequest.value!!
        val analyzing = coordinator.state.value as CoordinatorState.Analyzing
        assertEquals(analyzing.id, request.analysisId)
        coordinator.shutdown()
    }
}