package io.github.panda17tk.arpg.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.88 ボスHPバー: slides in with a target, tracks its health, slides away without one. */
class BossBarTest {
    @Test fun `the bar slides in with a heavy and away without one`() {
        val b = BossBar()
        assertFalse(b.visible)
        b.update(true, "錆のカラス", 1f, 0.1f)
        assertTrue(b.visible && b.k < 1f)
        b.update(true, "錆のカラス", 1f, 1f)
        assertEquals(1f, b.k, 1e-4f)
        b.update(false, null, 0f, 1f)
        assertFalse(b.visible)
    }

    @Test fun `displayed health eases toward the real value`() {
        val b = BossBar()
        b.update(true, "空洞の騎士", 1f, 1f)
        b.update(true, "空洞の騎士", 0.4f, 0.05f)
        assertTrue(b.frac in 0.4f..0.99f, "eases down, not a snap (got ${b.frac})")
        repeat(60) { b.update(true, "空洞の騎士", 0.4f, 1f / 30f) }
        assertEquals(0.4f, b.frac, 0.01f)
    }

    @Test fun `a new heavy snaps its own health instead of inheriting the last fight`() {
        val b = BossBar()
        b.update(true, "A", 0.2f, 1f)
        b.update(true, "B", 1f, 0.001f)
        assertTrue(b.frac > 0.9f, "B starts from its own health (got ${b.frac})")
        assertEquals("B", b.name)
    }
}
