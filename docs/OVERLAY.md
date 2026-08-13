# TRX-CHESS — OVERLAY

## Design

The overlay is a floating analysis window above other apps. Architecture
rules:

1. The overlay **renders only** — it never owns engine logic.
2. Data flows through the process-wide `OverlayController` (StateFlow bridge):
   the app layer publishes coalesced snapshots; the service subscribes.
3. `OverlayPublisher` throttles engine output to 250 ms snapshots and skips
   no-op updates.

## Enable flow

1. Settings → Overlay → enable toggle
2. If `Settings.canDrawOverlays` is false, the host activity opens
   `ACTION_MANAGE_OVERLAY_PERMISSION`
3. On return, permission is re-checked; the setting is persisted and the
   service is started (`startForegroundService` on O+, plain start below)
4. `OverlayService` re-checks permission in `onCreate` and no-ops without it

## Service

- Foreground service (`dataSync` type) with its own notification channel and
  an ongoing notification while the overlay is up
- Window: `TYPE_APPLICATION_OVERLAY` (O+) / `TYPE_PHONE` (legacy),
  `FLAG_NOT_FOCUSABLE` — it cannot steal input from the app below
- Panel: glass card (compact/expanded), drag + edge snap, opacity from
  settings, 30 Hz max render rate

## Data model

`OverlayData` carries evaluation, best move, depth, nodes, nps, multiPV list,
engine active/ready flags and a timestamp. `OverlayPrefs` (compact, opacity)
is pushed from settings through the controller.

## Security

The overlay declares the minimal permission surface (SYSTEM_ALERT_WINDOW,
FGS dataSync) and renders only app-published data. No automation or
stealth behavior is implemented.