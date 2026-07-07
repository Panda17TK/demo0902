package io.github.panda17tk.arpg.save

import io.github.panda17tk.arpg.config.WorkshopBoons
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Mods
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.world.WorldFactory
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.ui.WorkshopPanel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.104 周回の印: the sixth craft, paced by completed syncs, and its all-stat mark on a run. */
class NewGamePlusTest {
    @Test fun `the core craft exists and its ranks are paced by clears`() {
        val core = WorkshopCatalog.byId("core")!!
        assertEquals("管制核の欠片", core.title)
        assertEquals(0, WorkshopCatalog.rankCap(core, 0), "no sync yet — the craft stays sealed")
        assertEquals(2, WorkshopCatalog.rankCap(core, 2), "one rank per completed sync")
        assertEquals(core.maxRank, WorkshopCatalog.rankCap(core, 99), "capped at its own maxRank")
        val hull = WorkshopCatalog.byId("hull")!!
        assertEquals(hull.maxRank, WorkshopCatalog.rankCap(hull, 0), "every other craft ignores clears")
    }

    @Test fun `core ranks fold into the all-stat multiplier`() {
        assertEquals(1f, WorkshopCatalog.boonsFor(emptyMap()).allMul, 1e-4f)
        assertEquals(1.06f, WorkshopCatalog.boonsFor(mapOf("core" to 3)).allMul, 1e-4f)
    }

    @Test fun `the mark reaches the run — hp, stamina, damage and stride all scale`() {
        val plain = WorldFactory.create(InputState(), seed = 3L)
        val marked = WorldFactory.create(InputState(), seed = 3L, boons = WorkshopBoons(allMul = 1.06f))
        val plainHp = with(plain.world) { plain.player[Health].hpMax }
        val markedHp = with(marked.world) { marked.player[Health].hpMax }
        assertEquals(plainHp * 1.06f, markedHp, 1e-2f)
        val plainSta = with(plain.world) { plain.player[Stamina].max }
        val markedSta = with(marked.world) { marked.player[Stamina].max }
        assertEquals(plainSta * 1.06f, markedSta, 1e-2f)
        with(marked.world) {
            val mods = marked.player[Mods]
            assertEquals(1.06f, mods.gunMul, 1e-4f)
            assertEquals(1.06f, mods.meleeMul, 1e-4f)
            assertEquals(1.06f, mods.moveMul, 1e-4f)
        }
    }

    @Test fun `six crafts still fit a small screen with the close button clear`() {
        for ((w, h) in listOf(320f to 640f, 360f to 780f, 420f to 900f)) {
            val btns = WorkshopPanel.buttons(w, h)
            assertEquals(WorkshopCatalog.ITEMS.size + 1, btns.size)
            val close = btns.last()
            val lastRow = btns[btns.size - 2]
            assertTrue(close.y + close.h <= lastRow.y, "閉じる sits clear below the rows at $w x $h")
            for (b in btns) assertTrue(b.y >= 0f && b.y + b.h <= h, "off screen: $b at $w x $h")
        }
    }
}
