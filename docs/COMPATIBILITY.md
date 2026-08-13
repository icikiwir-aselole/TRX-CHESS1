# TRX-CHESS — COMPATIBILITY

## API levels

| Surface | Min | Notes |
|---|---|---|
| App | 24 | SDK_INT guards used where needed (O+ FGS, O+ overlay window type) |
| Target | 36 | |
| Compile | 37 | `platforms;android-37` unavailable in SDK repo — AGP auto-installs during build; CI must use 36 explicitly |

## Feature gating

- Overlay window type: `TYPE_APPLICATION_OVERLAY` (O+) / `TYPE_PHONE` (legacy)
- Foreground service start: `startForegroundService` (O+) / `startService`
- Thermal status: `PowerManager.currentThermalStatus` (Q+), else NONE
- Splash: Android 12+ system splash attributes in `values-v31`,
  `windowLightNavigationBar` in `values-v27`
- Reduced motion: system animator scale (O+) detected and honored

## Design compatibility

- All UI strings live in `res/values/strings.xml` (single locale, RTL
  supported via `supportsRtl`)
- Views are programmatic; no layout XML to break across densities
- Board/hero scale from density-independent units

## Known limitations

- Instrumented tests require a device; not run in this environment.
- The baseline native engine is integration-grade, not tournament strength.