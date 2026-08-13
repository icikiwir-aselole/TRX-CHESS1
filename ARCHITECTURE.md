# TRX-CHESS Architecture

Dependency direction is `app -> ui/policy/coordinator -> interfaces -> implementations`. The chess rules module has no Android UI dependency.

Critical state machines are explicit, notably `EngineState` and `CoordinatorState`. Engine commands have a single controller owner; analysis results carry a position identity to prevent stale-result races.

The native engine is behind `ChessEngine`, so replacing the baseline native backend with a stronger UCI implementation does not require changing the UI or domain model.
