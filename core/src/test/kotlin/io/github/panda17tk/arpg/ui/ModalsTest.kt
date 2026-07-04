package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Mirrors TouchLayoutTest's style: feed a screen size, assert the pure geometry. */
class ModalsTest {
    private val w = 360f
    private val h = 800f

    private fun onScreen(b: UiButton) =
        b.x >= 0f && b.y >= 0f && b.x + b.w <= w && b.y + b.h <= h

    private fun disjoint(a: UiButton, b: UiButton): Boolean =
        a.x + a.w <= b.x || b.x + b.w <= a.x || a.y + a.h <= b.y || b.y + b.h <= a.y

    private fun nonOverlapping(rects: List<UiButton>): Boolean {
        for (i in rects.indices) for (j in i + 1 until rects.size)
            if (!disjoint(rects[i], rects[j])) return false
        return true
    }

    @Test fun `three upgrade cards fit on screen and do not overlap`() {
        val cards = Modals.upgradeCards(w, h, 3)
        assertEquals(3, cards.size)
        assertTrue(cards.all(::onScreen), "cards off screen: $cards")
        assertTrue(nonOverlapping(cards), "cards overlap: $cards")
    }

    @Test fun `upgrade cards are horizontally centered`() {
        val cards = Modals.upgradeCards(w, h, 3)
        cards.forEach { assertEquals(w / 2f, it.centerX, 0.01f) }
    }

    @Test fun `zero upgrade cards yields an empty list`() {
        assertTrue(Modals.upgradeCards(w, h, 0).isEmpty())
    }

    @Test fun `game over shows one restart button on screen`() {
        val b = Modals.gameOverButtons(w, h)
        assertEquals(1, b.size)
        assertTrue(onScreen(b[0]))
    }

    @Test fun `pause button sits in the top-right corner on screen`() {
        val b = Modals.pauseButton(w, h)
        assertTrue(onScreen(b))
        assertTrue(b.centerX > w * 0.5f, "not in right half: $b")
        assertTrue(b.centerY > h * 0.5f, "not in top half: $b")
    }

    @Test fun `pause overlay has four buttons that fit and do not overlap`() {
        val b = Modals.pauseButtons(w, h)
        assertEquals(5, b.size) // v2.53: +旧式戦闘訓練
        assertEquals("旧式戦闘訓練", b[3].label)
        assertEquals("宇宙の記憶を消す", b[4].label)
        assertTrue(b.all(::onScreen), "buttons off screen: $b")
        assertTrue(nonOverlapping(b), "buttons overlap: $b")
    }

    @Test fun `the surface pause fits five buttons without overlap`() {
        val b = Modals.pauseButtons(w, h, includeMemory = true)
        assertEquals(6, b.size) // v2.53: +旧式戦闘訓練
        assertEquals("この星の記憶", b[3].label)
        assertEquals("旧式戦闘訓練", b[4].label)
        assertEquals("宇宙の記憶を消す", b[5].label)
        assertTrue(b.all(::onScreen), "buttons off screen: $b")
        assertTrue(nonOverlapping(b), "buttons overlap: $b")
    }

    @Test fun `the forget confirmation has two buttons that fit`() {
        val b = Modals.forgetButtons(w, h)
        assertEquals(2, b.size)
        assertEquals("消す", b[0].label)
        assertTrue(b.all(::onScreen) && nonOverlapping(b), "forget buttons broken: $b")
    }

    @Test fun `help overlay has one back button on screen`() {
        val b = Modals.helpButtons(w, h)
        assertEquals(1, b.size)
        assertTrue(onScreen(b[0]))
    }

    @Test fun `hitModal returns the index of the rect under the point`() {
        val cards = Modals.upgradeCards(w, h, 3)
        cards.forEachIndexed { i, c ->
            assertEquals(i, Modals.hitModal(cards, c.centerX, c.centerY))
        }
    }

    @Test fun `hitModal returns null when no rect is hit`() {
        val cards = Modals.upgradeCards(w, h, 3)
        assertNull(Modals.hitModal(cards, 0f, 0f))
    }

    @Test fun `hitModal returns the first matching rect when rects overlap`() {
        val a = UiButton(0f, 0f, 100f, 100f)
        val b = UiButton(50f, 50f, 100f, 100f)
        assertEquals(0, Modals.hitModal(listOf(a, b), 75f, 75f))
    }
}
