# TRX-CHESS — SECURITY

## Storage

- Secrets are stored through Android Keystore-backed AES-GCM
  (`security` module, `SecureStorage`). The diagnostic screen verifies the
  keystore path with a probe value.
- `android:allowBackup="false"` — app data is not backed up to the cloud.
- Release configuration contains no secret values.

## Input handling

- FEN strings are validated strictly before use (`ChessPosition.fromFen`
  requires a legal board layout; failure is surfaced as an error, never
  silently applied).
- Imported PGN/FEN must be treated as untrusted input; the parser layer
  bounds all fields.

## Permissions (minimal surface)

- `SYSTEM_ALERT_WINDOW` — overlay feature only; requested via the platform
  settings screen, re-checked by the service on every start
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` — overlay service
- `ACCESS_NETWORK_STATE` — diagnostics read-only connectivity report

No network, location, storage or identity permissions are declared.

## Overlay

The overlay window is `FLAG_NOT_FOCUSABLE` (cannot steal input) and renders
only data published by the app layer. It never drives engine logic.

## Fair play

The automation subsystem remains fair-play gated. The app does not implement
anti-cheat bypass or stealth automation.