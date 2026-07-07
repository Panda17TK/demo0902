package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.input.TouchButton
import io.github.panda17tk.arpg.input.TouchButtons
import io.github.panda17tk.arpg.input.TouchLayout
import io.github.panda17tk.arpg.save.TuneMode
import io.github.panda17tk.arpg.ui.TuningPanel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.98 調整モード: the passcode door, the tune button's dock, and the live knobs. */
class TuningModeTest {
    @AfterEach fun reset() { TuneMode.active = false }

    @Test fun `only 2938 opens the door`() {
        assertFalse(TuneMode.tryUnlock("1234"))
        assertFalse(TuneMode.active)
        assertFalse(TuneMode.tryUnlock(""))
        assertTrue(TuneMode.tryUnlock("2938"))
        assertTrue(TuneMode.active)
    }

    @Test fun `the tune button exists only in tune mode and docks left of 持物`() {
        val plain = TouchButtons.visible(blocks = 1, mag = 3, magSize = 16, canLand = false)
        assertFalse(TouchButton.TUNE in plain)
        val tuned = TouchButtons.visible(blocks = 1, mag = 3, magSize = 16, canLand = false, tuneMode = true)
        assertTrue(TouchButton.TUNE in tuned)
        // portrait AND landscape: 調整 docks beside 持物 (left, at most a short reach up)
        for (l in listOf(TouchLayout(360f, 800f), TouchLayout(1000f, 600f))) {
            assertTrue(l.centerX(TouchButton.TUNE) < l.centerX(TouchButton.INV), "left of 持物")
            assertTrue(kotlin.math.abs(l.centerY(TouchButton.INV) - l.centerY(TouchButton.TUNE)) < 70f, "beside 持物")
        }
    }

    @Test fun `the catalog reaches player and weapon knobs and a nudge really lands`() {
        val config = GameConfig()
        val params = TuningCatalog.paramsFor(config)
        assertTrue(params.size >= 25, "a real catalog (got ${params.size})")
        val hp = params.first { it.name == "最大HP" }
        val before = config.player.hpMax
        hp.nudge(+1)
        assertEquals(before + 10f, config.player.hpMax, 1e-3f, "the knob turns the shared config")
        val pistol = config.weapons.first { it.id == "pistol" }
        val dmg = params.first { it.name == "${pistol.name} 威力" }
        val d0 = pistol.dmg
        dmg.nudge(+1)
        assertEquals(d0 + 1f, pistol.dmg, 1e-3f)
        val mag = params.first { it.name == "${pistol.name} 装弾数" }
        val m0 = pistol.magSize ?: 0
        mag.nudge(+1)
        assertEquals(m0 + 1, pistol.magSize)
    }

    @Test fun `nudging is clamped at both rims`() {
        val config = GameConfig()
        val params = TuningCatalog.paramsFor(config)
        val hp = params.first { it.name == "最大HP" }
        repeat(200) { hp.nudge(-1) }
        assertEquals(10f, hp.get(), 1e-3f, "floors at min")
        repeat(500) { hp.nudge(+1) }
        assertTrue(hp.get() <= 999f, "ceils at max")
    }

    @Test fun `the panel pages the catalog seven rows at a time`() {
        assertEquals(1, TuningPanel.pageCount(3))
        assertEquals(1, TuningPanel.pageCount(7))
        assertEquals(2, TuningPanel.pageCount(8))
        val rows = TuningPanel.rows(360f, 800f)
        assertEquals(TuningPanel.ROWS, rows.size)
        for (row in rows) {
            val minus = TuningPanel.minus(row); val plus = TuningPanel.plus(row)
            assertTrue(minus.x >= row.x && minus.x + minus.w < plus.x, "[−] left of [＋], both inside the row")
            assertTrue(plus.x + plus.w <= row.x + row.w + 0.01f)
        }
        val footer = TuningPanel.footer(360f, 800f)
        assertEquals(listOf(TuningPanel.PREV, TuningPanel.NEXT, TuningPanel.CLOSE), footer.map { it.label })
    }
}
