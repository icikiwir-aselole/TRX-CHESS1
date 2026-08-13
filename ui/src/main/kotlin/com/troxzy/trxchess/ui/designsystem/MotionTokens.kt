package com.troxzy.trxchess.ui.designsystem

import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator

/**
 * Centralized motion language.
 *
 * Categories follow the TRX-CHESS budget:
 * micro 120-160ms, short 160-220ms, standard 220-320ms,
 * emphasis 320-450ms, cinematic 450-650ms.
 *
 * All durations and easings come from here; screens never hard-code values.
 */
object MotionTokens {

    const val MicroMs = 140L
    const val ShortMs = 200L
    const val StandardMs = 280L
    const val EmphasisMs = 400L
    const val CinematicMs = 560L

    val StandardEasing: Interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
    val EmphasizedEasing: Interpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)
    val DecelerateEasing: Interpolator = DecelerateInterpolator(2f)
    val LinearEasing: Interpolator = PathInterpolator(0f, 0f, 1f, 1f)

    /** Applies the reduced-motion scale to a duration; never below floor. */
    fun scaled(durationMs: Long, scale: Float, floorMs: Long = 0L): Long =
        (durationMs * scale.coerceIn(0f, 1f)).toLong().coerceAtLeast(floorMs)
}

enum class AnimationCategory { MICRO, SHORT, STANDARD, EMPHASIS, CINEMATIC }

fun AnimationCategory.durationMs(): Long = when (this) {
    AnimationCategory.MICRO -> MotionTokens.MicroMs
    AnimationCategory.SHORT -> MotionTokens.ShortMs
    AnimationCategory.STANDARD -> MotionTokens.StandardMs
    AnimationCategory.EMPHASIS -> MotionTokens.EmphasisMs
    AnimationCategory.CINEMATIC -> MotionTokens.CinematicMs
}
