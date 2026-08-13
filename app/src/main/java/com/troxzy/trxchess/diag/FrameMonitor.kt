package com.troxzy.trxchess.diag

import android.view.Choreographer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.math.max

/**
 * Real frame-time telemetry.
 *
 * Uses Choreographer callbacks to measure the actual frame interval while a
 * diagnostics screen is visible, counting janky frames (> 16.6ms) and
 * reporting the rolling average. Stops automatically when detached.
 */
class FrameMonitor {

    private data class Window(
        var frames: Int = 0,
        var janky: Int = 0,
        var totalMs: Double = 0.0,
        var startMs: Long = 0L,
    )

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var lastFrameNs = 0L
    private var window = Window()

    var avgFrameMs: Double = 0.0
        private set

    var jankyFrames: Int = 0
        private set

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (lastFrameNs != 0L) {
                val deltaMs = (frameTimeNanos - lastFrameNs) / 1_000_000.0
                window.frames++
                window.totalMs += deltaMs
                if (deltaMs > 16.6) window.janky++
                if (SystemClock.uptimeMillis() - window.startMs >= WINDOW_MS) {
                    avgFrameMs = if (window.frames > 0) window.totalMs / window.frames else 0.0
                    jankyFrames = window.janky
                    window = Window()
                    window.startMs = SystemClock.uptimeMillis()
                }
            } else {
                window.startMs = SystemClock.uptimeMillis()
            }
            lastFrameNs = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNs = 0L
        window = Window()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    companion object {
        private const val WINDOW_MS = 2_000L
    }
}
