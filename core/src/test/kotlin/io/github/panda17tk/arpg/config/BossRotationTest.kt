package io.github.panda17tk.arpg.config

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.177 濃い外縁: the boss rotation doubled — the pool must hold, and stay append-only. */
class BossRotationTest {
    @Test fun `the surge draws from at least six crowned heads`() {
        val bosses = GameConfig().enemies.filterValues { it.tier == "boss" }
        assertTrue(bosses.size >= 6, "boss pool: ${'$'}{bosses.keys}")
        // every attack a new boss carries must be a type the handlers already speak
        val known = setOf("slam", "enrage", "ring_gap", "summon", "barrage", "page_wall", "shockwave", "heal")
        for (id in listOf("rust_sovereign", "grand_archivist", "tide_sovereign")) {
            val def = GameConfig().enemies[id]
            assertTrue(def != null && def.tier == "boss", "${'$'}id joined the rotation")
            def!!.attacks.forEach { assertTrue(it.type in known, "${'$'}id uses handled attack ${'$'}{it.type}") }
        }
    }
}
