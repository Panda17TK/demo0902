package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.71 タイトル演出: the sky's dramas are pure functions of time — deterministic and bounded. */
class TitleFxTest {
    @Test fun `the same moment always shows the same sky`() {
        for (t in listOf(0.7f, 12.3f, 55.5f, 300.1f)) {
            assertEquals(TitleFx.meteorAt(t), TitleFx.meteorAt(t))
            assertEquals(TitleFx.twinkle(5, t), TitleFx.twinkle(5, t))
        }
    }

    @Test fun `meteors fall sometimes, and rest between falls`() {
        var seen = 0; var quiet = 0
        var t = 0f
        while (t < 90f) {
            if (TitleFx.meteorAt(t) != null) seen++ else quiet++
            t += 0.05f
        }
        assertTrue(seen > 0, "no meteor ever fell in 90 seconds")
        assertTrue(quiet > seen, "the sky should be quiet most of the time")
    }

    @Test fun `a falling meteor stays near the screen and always moves down-left`() {
        var t = 0f
        while (t < 90f) {
            TitleFx.meteorAt(t)?.let { m ->
                assertTrue(m.fx in -0.2f..1.2f && m.fy in -0.2f..1.2f, "meteor strayed: $m")
                assertTrue(m.dirX < 0f && m.dirY < 0f, "meteors fall down-left: $m")
                assertTrue(m.p in 0f..1f)
            }
            t += 0.05f
        }
    }

    @Test fun `twinkle and glow stay inside their quiet bands`() {
        var t = 0f
        while (t < 30f) {
            for (i in 0 until 12) {
                val tw = TitleFx.twinkle(i, t)
                assertTrue(tw in 0.3f..1.01f, "twinkle out of band: $tw")
            }
            val g = TitleFx.glow(t)
            assertTrue(g in 0.03f..0.17f, "glow out of band: $g")
            t += 0.1f
        }
    }

    @Test fun `the nebula bank is stable and sane`() {
        assertEquals(4, TitleFx.CLOUDS.size)
        assertEquals(TitleFx.CLOUDS, TitleFx.CLOUDS) // same list every read (computed once)
        for (c in TitleFx.CLOUDS) {
            assertTrue(c.fx in 0f..1f && c.fy in 0f..1f)
            assertTrue(c.speed > 0f && c.fr in 0.1f..0.4f)
        }
    }
}
