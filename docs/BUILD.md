# TRX-CHESS — BUILD

## Toolchain (pinned — do not bump without verification)

| Component | Version | Notes |
|---|---|---|
| JDK | 21 (compatible, JVM target 17) | Set `JAVA_HOME` before building |
| Gradle | 9.4.0 (system) | `gradlew` is a stub delegating to `gradle` on PATH |
| AGP | 9.1.1 | Gradle 9.5+ / AGP 9.3.x are NOT downloadable (404); do not attempt |
| Kotlin / KSP | 2.3.10 | Kotlin android plugin 2.3.11 does not exist on Maven Central |
| Android SDK | platform-tools, platforms;android-36, build-tools;36.0.0 | `platforms;android-37` is unavailable in the SDK repo; AGP auto-installs missing components when licenses are accepted |

`local.properties` must contain `sdk.dir=/opt/android-sdk` (or your SDK path).

## Commands

```bash
export JAVA_HOME=/home/codespace/java/21.0.10-ms
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # release APK (R8 minified)
./gradlew testDebugUnitTest       # unit tests (all modules)
./gradlew :app:lintDebug          # lint (0 errors)
```

Filter noisy deprecation output with `grep -vE "^w: file"`.

## Module layout

- `app` — screens, navigation, ViewModels, container, resources
- `ui` — design system, components, brand, board, analysis widgets
- `chess` — pure position logic (FEN, legal moves, apply)
- `engine/{api,uci,native}` — engine contracts, UCI parser, JNI baseline engine
- `analysis/coordinator` — analysis orchestration over the engine
- `overlay` — floating overlay window service + controller
- `data` — Room (sessions) + DataStore (settings) keys
- `security`, `automation`, `network/api`, `diagnostics`, `benchmark`, `core/common`, `position/api`

## Known constraints

- The repository's `gradlew` is a thin stub; CI must install Gradle 9.4 on PATH first.
- `ci.yml` references `platforms;android-37` — change to `android-36` (see open TODO).
- Instrumented tests require an emulator/device; `connectedCheck` reports NOT RUN otherwise.
