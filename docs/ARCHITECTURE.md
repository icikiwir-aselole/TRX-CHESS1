# TRX-CHESS — ARCHITECTURE

## Dependency flow

```
app ──▶ ui ──▶ analysis/coordinator ──▶ engine/api ◀── engine/{uci,native}
 │      │                    │
 │      ├── chess            └──▶ interfaces (core/common)
 │      ├── overlay  ───────────▶ OverlayController (process bridge)
 │      ├── data (Room + DataStore)
 │      ├── security (Keystore AES-GCM)
 │      └── automation (fair-play gated), network/api, diagnostics, benchmark
```

The app module is the only composition root. `TrxApp` builds a single
`AppContainer` that owns the engine, coordinator, settings repository, design
system and overlay bridge; screens receive it through the activity.

## Key decisions

- **Classic Views + Canvas** — no Compose. Custom views draw tokens from the
  design system; consistent with the existing codebase and explicit about
  render cost.
- **Single-activity host** — `MainActivity` swaps screen views in a container
  with a custom back stack; ViewModels are activity-scoped so state survives
  navigation. Theme changes recreate the current screen view.
- **Process-wide overlay bridge** — `OverlayController` is a single
  StateFlow-based data bridge. The overlay service renders only what is
  published by the app layer (throttled by `OverlayPublisher`); it never owns
  engine logic.
- **Adaptive visual policy** — `AppContainer.visualPolicy` combines settings
  with live device signals (device class, thermal, battery, reduced motion)
  into a `VisualPolicy` pushed into `DesignSystem`; every view reads it
  instead of probing the device.
- **Engine state is explicit** — `AnalysisCoordinator` owns the engine
  lifecycle and exposes `StateFlow`s for state and analysis. Results carry a
  position identity so stale results are never applied.
- **Real telemetry only** — diagnostics (frame times, jank, thermal, storage)
  come from actual device APIs; nothing is fabricated.

## Configuration surfaces

- `app/src/main/AndroidManifest.xml` — activities, overlay FGS service,
  permissions (overlay, FGS dataSync, network state)
- `gradle/libs.versions.toml` — version catalog (pinned versions, see BUILD.md)
- `data/.../Database.kt` — `Keys` (settings) and Room entities/DAO
- `core/AppSettings.kt` — settings model + DataStore repository

## Security notes

- Secrets stored via Android Keystore AES-GCM (`security` module)
- FEN input is validated strictly (`ChessPosition.fromFen`) before use
- No secrets in release configuration

## Current Verification

```text
assembleDebug       PASS
assembleRelease     PASS
unitTest             PASS (48 tests)
lint                 PASS (0 errors)
instrumentedTest     NOT RUN (no device)
benchmark            NOT RUN (no device)
```
