# TRX-CHESS

Production-oriented Android chess analysis architecture by Troxzy.

- Min API: 24
- Target API: 36
- Compile API: 37
- Offline-first core
- Modular Clean Architecture
- Native JNI engine boundary
- UCI parsing and explicit engine lifecycle
- Room + DataStore persistence
- Overlay service with throttling-ready boundary
- Automation policy with fair-play gating

## Build

Use JDK 17 and Android SDK platforms 37 + build tools 36.0.0. The project intentionally uses fixed toolchain versions; avoid dynamic dependencies.

```bash
./gradlew :app:assembleDebug
./gradlew test
```

## Engine backend

`engine/native` contains a self-contained C++ JNI UCI-style baseline evaluator. It is deliberately small and deterministic, suitable for integration testing and architecture validation rather than tournament-strength play. Production strength can be supplied behind the same `ChessEngine` interface.

## Security

Secrets are stored through Android Keystore-backed AES-GCM. Release configuration contains no secret values. Imported PGN/FEN must be treated as untrusted input.
