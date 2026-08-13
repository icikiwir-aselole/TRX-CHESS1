# TRX-CHESS

Production-oriented Android chess analysis app by Troxzy — cyber-anime dark
UI with a native engine boundary and an adaptive visual system.

- Min API: 24 · Target API: 36 · Compile API: 37
- Offline-first core, modular Clean Architecture
- Design system (color/type/shape/motion tokens) + adaptive visual quality
- Animated board (moves, captures, selection, legal moves, check, flip)
- Analysis screen: eval bar, eval graph, engine status, multiPV, PV line
- Board editor (FEN load/clear/place pieces) → analysis hand-off
- Floating overlay with permission flow and throttled snapshots
- Room (session history) + DataStore (settings)
- Keystore AES-GCM secrets; strict FEN validation; fair-play-gated automation
- Real telemetry only (frame times, jank, thermal, storage, network, keystore)

## Build

Pinned toolchain (Gradle 9.4.0 system, AGP 9.1.1, Kotlin/KSP 2.3.10,
JDK 21 with JVM target 17, SDK 36 + build-tools 36.0.0). See
[BUILD.md](BUILD.md) before bumping anything.

```bash
export JAVA_HOME=/home/codespace/java/21.0.10-ms
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest
./gradlew :app:lintDebug
```

Status: assembleDebug / assembleRelease / unit tests / lint all PASS
(instrumented tests require a device — see [TESTING.md](TESTING.md)).

## Engine backend

`engine/native` contains a self-contained C++ JNI UCI-style baseline
evaluator — small and deterministic, for integration testing and
architecture validation. Production strength plugs in behind the same
`ChessEngine` interface (`engine/api`). See [ENGINE.md](ENGINE.md).

## More docs

- [ARCHITECTURE.md](ARCHITECTURE.md) — dependency flow and key decisions
- [OVERLAY.md](OVERLAY.md) — floating analysis window
- [PERFORMANCE.md](PERFORMANCE.md) — adaptive quality and render budget
- [SECURITY.md](SECURITY.md) — permissions, storage, input handling
- [COMPATIBILITY.md](COMPATIBILITY.md) — API-level gating

## Current Verification

```text
assembleDebug       PASS
assembleRelease     PASS
unitTest             PASS (48 tests)
lint                 PASS (0 errors)
instrumentedTest     NOT RUN (no device)
benchmark            NOT RUN (no device)
```
