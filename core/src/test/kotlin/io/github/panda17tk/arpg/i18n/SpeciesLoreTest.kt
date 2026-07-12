package io.github.panda17tk.arpg.i18n

import io.github.panda17tk.arpg.config.GameConfig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.182 図録: the curated codex — every lore-bearing kind carries complete English, and every
 *  English key still points at a real roster kind that carries the Japanese source. */
class SpeciesLoreTest {
    private val lored = GameConfig().enemies.filterValues { it.lore.isNotBlank() }

    @Test fun `the codex is stocked`() {
        assertTrue(lored.size >= 15, "a codex worth reading names the memorable kinds (got ${lored.size})")
    }

    @Test fun `every lore-bearing kind has complete English`() {
        for ((id, def) in lored) {
            val en = SpeciesLoreEn.textOf(id)
            assertTrue(en != null && en.isNotBlank(), "no English codex line for $id (${def.name})")
            assertFalse(
                en!!.any { it.code in 0x2E80..0x9FFF || it.code in 0x3040..0x30FF },
                "CJK left in EN for $id: $en",
            )
        }
    }

    @Test fun `no English codex line is an orphan`() {
        val roster = GameConfig().enemies
        for (id in SpeciesLoreEn.ids()) {
            assertTrue(id in roster.keys, "$id is not a roster kind")
            assertTrue(roster.getValue(id).lore.isNotBlank(), "$id carries EN but no JA source")
        }
    }
}
