package com.troxzy.trxchess.ui.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionTokensTest {

    @Test
    fun `scaled applies scale to base duration`() {
        assertEquals(140L, MotionTokens.scaled(MotionTokens.MicroMs, 1f))
        assertEquals(70L, MotionTokens.scaled(MotionTokens.MicroMs, 0.5f))
        assertEquals(0L, MotionTokens.scaled(MotionTokens.StandardMs, 0f))
    }

    @Test
    fun `scaled never goes below floor`() {
        assertEquals(90L, MotionTokens.scaled(MotionTokens.EmphasisMs, 0.1f, floorMs = 90L))
    }

    @Test
    fun `scaled clamps scale to unit range`() {
        assertEquals(MotionTokens.CinematicMs, MotionTokens.scaled(MotionTokens.CinematicMs, 2f))
        assertEquals(0L, MotionTokens.scaled(MotionTokens.CinematicMs, -1f))
    }

    @Test
    fun `category durations follow the motion budget`() {
        assertEquals(MotionTokens.MicroMs, AnimationCategory.MICRO.durationMs())
        assertEquals(MotionTokens.ShortMs, AnimationCategory.SHORT.durationMs())
        assertEquals(MotionTokens.StandardMs, AnimationCategory.STANDARD.durationMs())
        assertEquals(MotionTokens.EmphasisMs, AnimationCategory.EMPHASIS.durationMs())
        assertEquals(MotionTokens.CinematicMs, AnimationCategory.CINEMATIC.durationMs())
        assert(MotionTokens.MicroMs <= MotionTokens.ShortMs)
        assert(MotionTokens.ShortMs <= MotionTokens.StandardMs)
        assert(MotionTokens.StandardMs <= MotionTokens.EmphasisMs)
        assert(MotionTokens.EmphasisMs <= MotionTokens.CinematicMs)
    }
}
