# TRX-CHESS — ENGINE

## Boundaries

`engine/api` defines the contracts the rest of the app depends on:

- `ChessEngine` — initialize / startAnalysis / stopAnalysis / shutdown /
  observeState / observeAnalysis; exposes `version` for identity checks
- `EngineConfig` — threads, hash MB, multiPV, move overhead; `configHash()`
  fingerprints the config for stale-result checks
- `AnalysisRequest` — position, `SearchLimit` (Depth/TimeMs/Nodes), multiPV,
  priority, and an `analysisId` the engine echoes back as the result's
  `requestId` (blank → engine generates one)
- `EngineState` — Uninitialized, Initializing, Ready, Analyzing, Stopping,
  Crashed, Failed, Shutdown
- `Evaluation` — Centipawn, Mate, LowerBound, UpperBound
- `EngineLine` — multiPV index, evaluation, depth, nodes, nps, PV (List<String> UCI)
- `EngineAnalysis` — request/position identity + lines + timestamp
- `EngineResult` — Ok / Error

## Implementations

- `engine/uci` — strict UCI protocol parser (`UciParser`) with `isReady`
  handshake and bounded error handling. This is the production-boundary path
  for a real engine binary.
- `engine/native` — self-contained C++ JNI baseline evaluator. Deliberately
  small and deterministic; used for integration testing and architecture
  validation, not tournament strength. Production strength plugs in behind
  the same `ChessEngine` interface.

## Ownership

`AnalysisCoordinator` is the single owner of engine interaction in the app
process:

- `engineState: StateFlow<EngineState>` — mirrored continuously from the engine
- `analysis: StateFlow<EngineAnalysis?>` — analysis results for the current position
- `analyze(position, limit, multiPv)` — stops any prior search, tags results
  with a full identity (analysisId + positionHash + engineVersion + configHash
  + active session), and collects engine output
- stale-result protection: an engine result is applied only when every
  identity component matches the current search; late results from a
  superseded search, a re-initialized engine, a changed configuration or an
  ended session are dropped (timestamps are never used for identity)
- `stop()` / `shutdown()` — explicit lifecycle, cancel-and-join semantics

The UI never touches the engine directly; it observes coordinator flows and
calls its suspend API from ViewModels.

## Overlay interaction

The analysis layer publishes throttled snapshots (250 ms default) through
`OverlayController`; the overlay window renders those snapshots only.

## Current Verification

```text
assembleDebug       PASS
assembleRelease     PASS
unitTest             PASS (48 tests)
lint                 PASS (0 errors)
instrumentedTest     NOT RUN (no device)
benchmark            NOT RUN (no device)
```
