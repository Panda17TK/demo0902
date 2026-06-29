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

    @Test fun `the aim guide clears every action button`() {
        for (b in l.all()) {
            val dx = l.centerX(b) - l.aimGuideCx
            val dy = l.centerY(b) - l.aimGuideCy
            val dist = kotlin.math.hypot(dx, dy)
            assertTrue(dist > l.radiusOf(b) + l.aimGuideRadius, "aim guide overlaps $b")
        }
    }
}
