# TRX-CHESS — TESTING

## Unit tests

Run: `./gradlew testDebugUnitTest` (or `./gradlew test` for all variants)

| Area | Location | Coverage |
|---|---|---|
| Chess rules | `chess/src/test` | FEN strict parse (18 cases: valid, malformed, illegal board, whitespace, invalid piece/castling/en-passant/clocks), position status (check/checkmate/stalemate), legal moves, FEN round trip |
| Analysis coordinator | `analysis/coordinator/src/test` | stale-result protection: matching identity applied; late previous-search result, wrong position hash, session change, engine version change, config change all rejected; analysis-id propagation |
| UCI protocol | `engine/uci/src/test` | parser acceptance and rejection cases |
| Visual policy | `ui/src/test/.../VisualQualityPolicyTest` | quality tiers, reduced motion, thermal/battery capping, device class |
| Motion tokens | `ui/src/test/.../MotionTokensTest` | duration scaling, floor, clamping, category budget ordering |

The UI unit tests run with `unitTests.isReturnDefaultValues = true` because
motion tokens instantiate framework interpolators.

## Instrumented tests

Infrastructure is in `app/src/androidTest` and is syntactically valid and
packaged (`:app:assembleDebugAndroidTest` builds the APK). Tests are tagged
with `AndroidJUnit4` / `LargeTest` and organized by area:

| Class | Covers |
|---|---|
| `AppNavigationTest` | launch to home, home→analysis/settings/about and back, back on root finishes |
| `FenImportTest` | valid FEN (checkmate + check status), malformed FEN, invalid piece, clear button |
| `PromotionDialogTest` | promotion dialog appears on last-rank pawn move, queen pick applies move, cancel leaves board untouched, rapid taps |
| `ThemeSwitchTest` | theme dialog dark/light/system switching, screen stability after theme rebuild, cancel keeps state |
| `RecreationTest` | activity recreation on home/settings, navigation after recreation |

Run: `./gradlew connectedDebugAndroidTest` with an emulator/device (API 24+).

`connectedCheck` requires an emulator or device. In the current
environment there is no device — instrumented tests are reported NOT RUN
rather than fabricated.

## Benchmark

The `:benchmark` module exists but benchmark instrumentation requires a
device; it is reported NOT RUN in the current environment.

## Manual validation

On a device/emulator:
- promotion: move a pawn to the last rank (white and black) → chooser
  appears → pick any piece → position updates; tap outside / back → canceled
- FEN: paste into the analysis screen → Load → board updates; malformed FEN
  shows a categorized message, never a raw exception
- analysis: load position A, start analysis, load position B quickly →
  late A results never appear (stale-result protection)
- overlay: grant overlay permission → enable in settings → floating panel
  appears; disable → panel disappears
- theme: switch dark/light/system in settings → current screen rebuilds
  without losing state
- reduced motion: enable system animator scale 0 → animations are skipped

## CI validation

`.github/workflows/ci.yml` runs on JDK 21 + Gradle 9.4 + API 36:
- `testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- `:app:assembleRelease`

## Verification matrix (current)

| Check | Result |
|---|---|
| `:app:assembleDebug` | PASS |
| `:app:assembleRelease` (R8) | PASS |
| `testDebugUnitTest` | PASS (48 tests) |
| `:app:lintDebug` | PASS (0 errors, 99 documented warnings) |
| `:app:assembleDebugAndroidTest` | PASS (APK built) |
| `connectedDebugAndroidTest` | NOT RUN (no device) |
| Benchmark | NOT RUN (no device) |

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
