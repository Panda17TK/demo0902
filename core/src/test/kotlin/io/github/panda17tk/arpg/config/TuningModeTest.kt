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

    @Test fun `tuning is a real overlay now — it pauses like pause and ESC walks it back to play`() {
        // v2.111: the popup rides the Overlay register (like 持物), so every overlay-aware guard
        // (gameplay touch, HUD chips, tap routing) excludes it without its own flag.
        assertTrue(io.github.panda17tk.arpg.ui.Overlay.entries.any { it.name == "TUNING" })
        assertEquals(
            io.github.panda17tk.arpg.ui.Overlay.NONE,
            io.github.panda17tk.arpg.ui.PauseFlow.toggle(io.github.panda17tk.arpg.ui.Overlay.TUNING),
        )
    }

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
        assertTrue(params.size >= 60, "v2.99: the catalog covers everything (got ${params.size})")
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
        assertEquals(1f, hp.get(), 1e-3f, "floors at min (v2.99: debug-wide bounds)")
        repeat(2000) { hp.nudge(+1, big = true) }
        assertTrue(hp.get() <= 99999f, "ceils at max")
    }

    @Test fun `the panel pages the catalog seven rows at a time`() {
        assertEquals(1, TuningPanel.pageCount(3))
        assertEquals(1, TuningPanel.pageCount(7))
        assertEquals(2, TuningPanel.pageCount(8))
        val rows = TuningPanel.rows(360f, 800f)
        assertEquals(TuningPanel.ROWS, rows.size)
        for (row in rows) {
            val zones = listOf(TuningPanel.minusBig(row), TuningPanel.minus(row), TuningPanel.plus(row), TuningPanel.plusBig(row))
            for (i in 0 until zones.size - 1) {
                assertTrue(zones[i].x + zones[i].w <= zones[i + 1].x + 0.01f, "≪ − ＋ ≫ stay in order without overlap")
            }
            assertTrue(zones.first().x >= row.x && zones.last().x + zones.last().w <= row.x + row.w + 0.01f)
        }
        val footer = TuningPanel.footer(360f, 800f)
        assertEquals(
            listOf(TuningPanel.PREV, TuningPanel.NEXT, TuningPanel.CLOSE, TuningPanel.EXPORT, TuningPanel.RESET_ALL),
            footer.map { it.label },
        )
    }

    // ── v2.99 第2弾: 基準・×10・一括リセット・ロースター倍率・書き出し ──

    @Test fun `every knob remembers its shipped 基準 and resets to it`() {
        val config = GameConfig()
        val params = TuningCatalog.paramsFor(config)
        for (p in params) assertTrue(!p.changed(), "fresh config sits at 基準: ${p.name}")
        val hp = params.first { it.name == "最大HP" }
        assertEquals(100f, hp.def, 1e-3f, "基準 is the shipped value")
        hp.nudge(+1, big = true) // ×10 step = +100
        assertEquals(200f, hp.get(), 1e-3f, "≫ rides ten steps")
        assertTrue(hp.changed())
        params.forEach { it.reset() }
        assertEquals(100f, hp.get(), 1e-3f, "全て既定へ returns to 基準")
        assertTrue(params.none { it.changed() })
    }

    @Test fun `the roster multipliers scale every enemy and restore cleanly`() {
        val config = GameConfig()
        val params = TuningCatalog.paramsFor(config)
        val hpMul = params.first { it.name == "全敵 HP倍率" }
        val zombie0 = config.enemies.getValue("zombie").hp
        val titan0 = config.enemies.getValue("rust_titan").hp
        hpMul.set(2f)
        assertEquals(zombie0 * 2f, config.enemies.getValue("zombie").hp, 1e-2f)
        assertEquals(titan0 * 2f, config.enemies.getValue("rust_titan").hp, 1e-2f)
        assertEquals(2f, hpMul.get(), 1e-3f, "the factor is derivable from the live table")
        hpMul.reset()
        assertEquals(zombie0, config.enemies.getValue("zombie").hp, 1e-2f, "基準 restores the roster")
        val dmgMul = params.first { it.name == "全敵 攻撃力倍率" }
        val bite0 = config.enemies.getValue("zombie").attacks.first { it.dmg > 0f }.dmg
        dmgMul.set(0.5f)
        assertEquals(bite0 * 0.5f, config.enemies.getValue("zombie").attacks.first { it.dmg > 0f }.dmg, 1e-2f)
    }

    @Test fun `the export reads like a hand-off to Claude`() {
        val config = GameConfig()
        val params = TuningCatalog.paramsFor(config)
        params.first { it.name == "最大HP" }.set(250f)
        val text = TuningExport.render("drift 調整パラメータ", params)
        assertTrue(text.contains("# drift 調整パラメータ"))
        assertTrue(text.contains("Claude"), "the header says what the file is for")
        assertTrue(text.contains("* 最大HP = 250 (基準 100)"), "drifted lines carry the * flag\n$text")
        assertTrue(text.contains("  移動速度 = "), "unchanged lines stay unflagged")
        assertTrue(text.contains("変更 1 / ${params.size} 項目"))
    }
}
