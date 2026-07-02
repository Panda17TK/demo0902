package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HudLayoutTest {
    // Portrait phone dp sizes (game is sensorPortrait); include a narrow one.
    private val sizes = listOf(320f to 640f, 360f to 780f, 420f to 900f)

    private fun onScreen(b: UiButton, w: Float, h: Float) =
        b.x >= 0f && b.y >= 0f && b.x + b.w <= w && b.y + b.h <= h

    private fun disjoint(a: UiButton, b: UiButton): Boolean =
        a.x + a.w <= b.x || b.x + b.w <= a.x || a.y + a.h <= b.y || b.y + b.h <= a.y

    @Test fun `every region has a positive size`() {
        for ((w, h) in sizes) {
            val l = HudLayout.of(w, h)
            for (r in listOf(l.wave, l.hp, l.stamina, l.ammo, l.stats)) {
                assertTrue(r.w > 0f && r.h > 0f, "non-positive region $r at $w x $h")
            }
        }
    }

    @Test fun `every region fits on screen`() {
        for ((w, h) in sizes) {
            val l = HudLayout.of(w, h)
            for (r in listOf(l.wave, l.hp, l.stamina, l.ammo, l.stats)) {
                assertTrue(onScreen(r, w, h), "region off screen $r at $w x $h")
            }
        }
    }

    @Test fun `the four primary regions never overlap`() {
        for ((w, h) in sizes) {
            val l = HudLayout.of(w, h)
            val regions = listOf(l.wave, l.hp, l.stamina, l.ammo)
            for (i in regions.indices) for (j in i + 1 until regions.size) {
                assertTrue(disjoint(regions[i], regions[j]), "overlap ${regions[i]} / ${regions[j]} at $w x $h")
            }
        }
    }

    @Test fun `the ammo panel clears the P1 pause button`() {
        for ((w, h) in sizes) {
            val l = HudLayout.of(w, h)
            assertTrue(disjoint(l.ammo, Modals.pauseButton(w, h)), "ammo collides with pause button at $w x $h")
        }
    }

    @Test fun `HP sits above stamina`() {
        for ((w, h) in sizes) {
            val l = HudLayout.of(w, h)
            assertTrue(l.hp.y > l.stamina.y, "HP not above stamina at $w x $h")
        }
    }

    @Test fun `the planet scan card fits on screen at every size and line count`() {
        for ((w, h) in sizes + (384f to 700f)) { // include the minimum ExtendViewport width
            for (lines in 2..4) {
                val card = HudLayout.planetCard(w, h, lines)
                assertTrue(onScreen(card, w, h), "scan card off screen at $w x $h with $lines lines: $card")
            }
        }
    }

    @Test fun `the planet scan card sits below the wave badge`() {
        for ((w, h) in sizes) {
            val l = HudLayout.of(w, h)
            val card = HudLayout.planetCard(w, h, 4)
            assertTrue(card.y + card.h < l.wave.y, "scan card overlaps the wave badge at $w x $h")
        }
    }
}

class SegmentsTest {
    @Test fun `a full bar lights every segment`() {
        assertEquals(10, filledSegments(10f, 10f, 10))
    }

    @Test fun `an empty bar lights none`() {
        assertEquals(0, filledSegments(0f, 10f, 10))
    }

    @Test fun `a half bar lights half the segments`() {
        assertEquals(5, filledSegments(5f, 10f, 10))
    }

    @Test fun `value above max clamps to the full count`() {
        assertEquals(10, filledSegments(15f, 10f, 10))
    }

    @Test fun `a non-positive max lights none`() {
        assertEquals(0, filledSegments(5f, 0f, 10))
    }
}
