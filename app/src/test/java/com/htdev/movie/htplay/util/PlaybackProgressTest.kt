package com.htdev.movie.htplay.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {
    @Test
    fun missingOrInvalidDurationHasNoProgress() {
        assertEquals(0, playbackProgressPercent(null, null))
        assertEquals(0, playbackProgressPercent(60, 0))
        assertEquals(0, playbackProgressPercent(60, -1))
        assertEquals(0, playbackProgressPercent(null, 600))
    }

    @Test
    fun progressIsClampedToPlaybackRange() {
        assertEquals(0, playbackProgressPercent(-30, 600))
        assertEquals(25, playbackProgressPercent(150, 600))
        assertEquals(100, playbackProgressPercent(900, 600))
    }

    @Test
    fun longDurationsDoNotOverflowInt() {
        assertEquals(50, playbackProgressPercent(3_000_000_000, 6_000_000_000))
    }
}
