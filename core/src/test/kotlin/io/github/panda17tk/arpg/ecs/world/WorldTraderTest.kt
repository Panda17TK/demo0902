package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.item.ItemKind
import io.github.panda17tk.arpg.item.Trader
import io.github.panda17tk.arpg.item.TraderGoodKind
import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.WorldMode
import io.github.panda17tk.arpg.ui.TraderPanel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.100 行商船: a friendly vessel in SOME skies, with deterministic shelves. */
class WorldTraderTest {
    @Test fun `some systems host the vessel, some don't, and the spot is deterministic`() {
        var hosted = 0
        for (seed in 1L..30L) {
            val a = WorldFactory.create(InputState(), seed = seed)
            val b = WorldFactory.create(InputState(), seed = seed)
            assertEquals(a.worldState.trader, b.worldState.trader, "seed $seed drifts the same sky twice")
            if (a.worldState.trader != null) hosted++
        }
        assertTrue(hosted in 1..29, "the vessel visits some skies but not all (got $hosted/30)")
    }

    @Test fun `surfaces never host the vessel`() {
        val gw = WorldFactory.create(InputState(), seed = 3L, mode = WorldMode.SURFACE, biome = PlanetBiome.NATURE)
        assertNull(gw.worldState.trader)
    }

    @Test fun `the shelves hold supplies, one piece of gear, and sometimes a shard`() {
        for (seed in 1L..30L) {
            val stock = Trader.stockFor(seed)
            assertEquals(stock, Trader.stockFor(seed), "same seed, same shelves")
            assertTrue(stock.size in 3..4, "3..4 slots (got ${stock.size})")
            assertEquals(TraderGoodKind.MED, stock[0].kind)
            assertEquals(TraderGoodKind.AMMO, stock[1].kind)
            assertEquals(TraderGoodKind.GEAR, stock[2].kind)
            val gear = stock[2].item
            assertTrue(gear != null && gear.kind != ItemKind.LORE, "stories are found, not sold")
            assertTrue(stock.all { it.price >= 1 }, "everything costs something")
            stock.getOrNull(3)?.let { shard ->
                assertEquals(TraderGoodKind.SHARD, shard.kind)
                assertTrue(shard.price >= 100, "the shortcut is priced steep (got ${shard.price})")
            }
        }
        // the shard visits some shelves but never all of them
        val withShard = (1L..40L).count { Trader.stockFor(it).size == 4 }
        assertTrue(withShard in 1..39, "got $withShard/40 shard shelves")
    }

    @Test fun `the shop panel stacks its plates above the leave button`() {
        val rows = TraderPanel.rows(360f, 800f, 4)
        assertEquals(4, rows.size)
        for (i in 0 until rows.size - 1) {
            assertTrue(rows[i].y > rows[i + 1].y, "plates stack top-down")
        }
        val close = TraderPanel.closeButton(360f, 800f)
        assertTrue(close.y + close.h < rows.last().y, "[離れる] sits clear below the shelves")
        assertEquals(TraderPanel.CLOSE, close.label)
    }
}
