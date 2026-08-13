# TRX-CHESS — PERFORMANCE

## Rendering

- All screens draw via Canvas custom views; no layout inflation overhead per
  frame. `invalidate()` is issued only when state actually changes.
- The board animates with a single `ValueAnimator` diffing positions;
  capture effects are drawn from the diff, no full redraw storms.
- The evaluation graph redraws on new samples only (bounded 96-sample
  history), never forcing board redraws.

## Adaptive quality

`AppContainer.visualPolicy` (see ARCHITECTURE.md) reduces render cost under
pressure:

- Thermal SEVERE → static rendering, no particles/glow, zero animation scale
- Thermal MODERATE → particles halved, animation scale 0.8x
- Battery < 15% unplugged → medium quality
- Low-end devices (cores ≤ 2 or memory ≤ 128 MB) → medium quality
- Power saver setting → low-end classification
- Reduced motion (setting or system animator scale 0) → fully static

Every view reads `designSystem.visualPolicy` — no per-view device probing.

## Engine

- Engine config is derived from settings (threads 1–4, hash 16–256 MB,
  multiPV 1–8) via `AppContainer.engineConfig()`.
- `AnalysisCoordinator` stops any running search before a new one and tags
  results by position identity so stale work never lands.
- Stop is cancel-and-join: no orphaned engine threads after shutdown.

## Telemetry

`FrameMonitor` (Choreographer-based) measures real frame times and jank
while the diagnostics screen is visible; the metrics are real, not synthetic.

## Overlay

`OverlayPublisher` throttles engine snapshots to the overlay at 250 ms and
only writes meaningful changes; the overlay panel renders at 30 Hz max.