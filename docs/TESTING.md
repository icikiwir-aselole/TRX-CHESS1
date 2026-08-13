# TRX-CHESS — TESTING

## Unit tests

Run: `./gradlew testDebugUnitTest` (or `./gradlew test` for all variants)

| Area | Location | Coverage |
|---|---|---|
| Chess rules | `chess/src/test` | FEN parse/serialize, legal moves, check detection |
| UCI protocol | `engine/uci/src/test` | parser acceptance and rejection cases |
| Visual policy | `ui/src/test/.../VisualQualityPolicyTest` | quality tiers, reduced motion, thermal/battery capping, device class |
| Motion tokens | `ui/src/test/.../MotionTokensTest` | duration scaling, floor, clamping, category budget ordering |

The UI unit tests run with `unitTests.isReturnDefaultValues = true` because
motion tokens instantiate framework interpolators.

## Instrumented tests

`connectedCheck` requires an emulator or device. In the current
environment there is no device — instrumented tests are reported NOT RUN
rather than fabricated.

## Lint

`./gradlew :app:lintDebug` — currently 0 errors. Remaining warnings are
documented classes:
- `UnusedResources` — strings/colors reserved for future screens (error states,
  overlay strings, status labels)
- `ViewConstructor` — screens are created programmatically, not from layouts
- `GradleDependency`/`NewerVersionAvailable` — versions are intentionally pinned
  (see BUILD.md)
- `CustomSplashScreen` — the branded splash is deliberate; Android 12+ shows
  the system splash first (values-v31 style), then the brand reveal
- `SetTextI18n` — numeric stat labels are locale-neutral integers

## Verification matrix (current)

| Check | Result |
|---|---|
| `:app:assembleDebug` | PASS |
| `:app:assembleRelease` (R8) | PASS |
| `testDebugUnitTest` | PASS (18 tests) |
| `:app:lintDebug` | PASS (0 errors) |
| `connectedCheck` | NOT RUN (no device) |