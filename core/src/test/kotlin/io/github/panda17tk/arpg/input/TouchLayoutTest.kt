package io.github.panda17tk.arpg.input

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TouchLayoutTest {
    private val l = TouchLayout(1000f, 600f)

    @Test fun `left half is the move-stick zone, right half is not`() {
        assertTrue(l.isInStickZone(100f, 300f))
        assertFalse(l.isInStickZone(900f, 300f))
    }

    @Test fun `each button center hit-tests back to itself`() {
        for (b in l.all()) assertEquals(b, l.button(l.centerX(b), l.centerY(b)))
    }

    @Test fun `a point in the stick zone matches no button`() {
        assertNull(l.button(100f, 300f))
    }

    @Test fun `the aim guide sits in the right half and on screen`() {
        assertTrue(l.aimGuideCx > l.screenW * 0.5f)
        assertTrue(l.aimGuideCx - l.aimGuideRadius >= 0f && l.aimGuideCx + l.aimGuideRadius <= l.screenW)
        assertTrue(l.aimGuideCy - l.aimGuideRadius >= 0f && l.aimGuideCy + l.aimGuideRadius <= l.screenH)
    }

    @Test fun `every button stays fully on screen — even on a narrow phone`() {
        // v2.54: the old fraction layout clipped ダッシュ/持物 off the right edge on tall phones.
        for (layout in listOf(TouchLayout(1000f, 600f), TouchLayout(360f, 800f), TouchLayout(411f, 914f))) {
            for (b in layout.all()) {
                val r = layout.radiusOf(b)
                assertTrue(layout.centerX(b) - r >= 0f && layout.centerX(b) + r <= layout.screenW, "$b clips horizontally")
                assertTrue(layout.centerY(b) - r >= 0f && layout.centerY(b) + r <= layout.screenH, "$b clips vertically")
            }
        }
    }

    @Test fun `grid buttons never overlap each other`() {
        for (layout in listOf(TouchLayout(1000f, 600f), TouchLayout(360f, 800f))) {
            val bs = layout.all()
            for (i in bs.indices) for (j in i + 1 until bs.size) {
                val a = bs[i]; val b = bs[j]
                val dist = kotlin.math.hypot(layout.centerX(a) - layout.centerX(b), layout.centerY(a) - layout.centerY(b))
                assertTrue(dist >= layout.radiusOf(a) + layout.radiusOf(b), "$a overlaps $b (dist $dist)")
            }
        }
    }

    @Test fun `the aim guide clears every action button`() {
        for (b in l.all()) {
            val dx = l.centerX(b) - l.aimGuideCx
            val dy = l.centerY(b) - l.aimGuideCy
            val dist = kotlin.math.hypot(dx, dy)
            assertTrue(dist > l.radiusOf(b) + l.aimGuideRadius, "aim guide overlaps $b")
        }
    }

    // ── v2.65 左利き配置 ────────────────────────────────────────────────

    private fun mirroredOf(w: Float, h: Float) = TouchLayout(w, h).apply { mirrored = true }

    @Test fun `mirrored flips the stick to the right and the cluster to the left`() {
        val m = mirroredOf(1000f, 600f)
        assertTrue(m.isInStickZone(900f, 300f))
        assertFalse(m.isInStickZone(100f, 300f))
        assertTrue(m.stickCx > m.screenW * 0.5f)
        assertTrue(m.centerX(TouchButton.DASH) < m.screenW * 0.5f, "hub belongs bottom-left")
        assertTrue(m.centerX(TouchButton.INV) < m.screenW * 0.5f, "持物 docks top-left")
        assertTrue(m.aimGuideCx < m.screenW * 0.5f)
    }

    @Test fun `mirrored geometry is the exact reflection of the right-handed one`() {
        val r = TouchLayout(411f, 914f)
        val m = mirroredOf(411f, 914f)
        for (b in r.all()) {
            assertEquals(r.screenW - r.centerX(b), m.centerX(b), 0.001f, "$b x should mirror")
            assertEquals(r.centerY(b), m.centerY(b), 0.001f, "$b y should not move")
            assertEquals(r.radiusOf(b), m.radiusOf(b), 0.001f, "$b size should not change")
        }
    }

    @Test fun `mirrored buttons stay on screen, apart, and hit-test back to themselves`() {
        for (layout in listOf(mirroredOf(1000f, 600f), mirroredOf(360f, 800f), mirroredOf(411f, 914f))) {
            for (b in layout.all()) {
                val r = layout.radiusOf(b)
                assertTrue(layout.centerX(b) - r >= 0f && layout.centerX(b) + r <= layout.screenW, "$b clips horizontally")
                assertTrue(layout.centerY(b) - r >= 0f && layout.centerY(b) + r <= layout.screenH, "$b clips vertically")
                assertEquals(b, layout.button(layout.centerX(b), layout.centerY(b)))
            }
            val bs = layout.all()
            for (i in bs.indices) for (j in i + 1 until bs.size) {
                val a = bs[i]; val b = bs[j]
                val dist = kotlin.math.hypot(layout.centerX(a) - layout.centerX(b), layout.centerY(a) - layout.centerY(b))
                assertTrue(dist >= layout.radiusOf(a) + layout.radiusOf(b), "$a overlaps $b (dist $dist)")
            }
        }
    }

    @Test fun `mirrored tweaks clamp into the left (non-stick) zone`() {
        val m = mirroredOf(1000f, 600f)
        m.tweaks = mapOf(TouchButton.MELEE to ButtonTweak(0.95f, 0.5f, 1f)) // dragged into the stick side
        val r = m.radiusOf(TouchButton.MELEE)
        assertTrue(m.centerX(TouchButton.MELEE) + r <= m.screenW * 0.54f, "must stay out of the stick zone")
    }
}
