# Engine

The UCI boundary is modeled in `engine/api` and `engine/uci`. The native implementation is in `engine/native` and currently exposes a JNI baseline evaluator.

A future production engine process can implement the same `ChessEngine` contract, including watchdog, crash restart, backoff, command serialization, and stdout streaming.
