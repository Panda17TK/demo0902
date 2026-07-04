package io.github.panda17tk.arpg.sim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.51 遭難ログ: deterministic per wreck, in the distress-log register. */
class WreckLogTest {
    @Test fun `a wreck's broadcast is deterministic`() {
        assertEquals(WreckLog.lineFor(5L, 0), WreckLog.lineFor(5L, 0))
        assertTrue(WreckLog.lineFor(5L, 0).startsWith("遭難記録: "))
    }

    @Test fun `different wrecks tell different stories`() {
        val lines = (0L..20L).flatMap { seed -> (0..2).map { WreckLog.lineFor(seed, it) } }.toSet()
        assertTrue(lines.size > 3, "expected variety, got ${lines.size}")
    }
}
