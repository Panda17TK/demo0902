package io.github.panda17tk.arpg.i18n

import io.github.panda17tk.arpg.planet.PlanetBiome
import io.github.panda17tk.arpg.sim.Endgame
import io.github.panda17tk.arpg.sim.MemoryCoreLog
import io.github.panda17tk.arpg.sim.WaveEvent
import io.github.panda17tk.arpg.sim.WaveEvents
import io.github.panda17tk.arpg.sim.WreckLog
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.162 英語化第4弾(後半・その1): the story's spine — cores, wrecks, the ending, the banner. */
class LangNarrativeTest {
    private fun assertEnglish(s: String) {
        val t = Lang.tr(s)
        assertTrue(t.none { it.code >= 0x2E80 && it != '　' }, "still CJK: 「$s」 -> 「$t」")
    }

    @Test fun `every memory core line speaks English at every depth`() {
        Lang.en = true
        try {
            val biomes: List<PlanetBiome?> = PlanetBiome.entries + null
            for (system in 1..4) for (b in biomes) for (pid in 1L..30L) {
                assertEnglish(MemoryCoreLog.lineFor(pid, b, system))
            }
        } finally { Lang.en = false }
    }

    @Test fun `every wreck broadcast speaks English`() {
        Lang.en = true
        try {
            for (seed in 1L..20L) for (i in 0..3) assertEnglish(WreckLog.lineFor(seed, i))
        } finally { Lang.en = false }
    }

    @Test fun `the whole ending reads in English`() {
        Lang.en = true
        try {
            Endgame.PAGES.flatten().forEach(::assertEnglish)
            Endgame.EPILOGUE.forEach(::assertEnglish)
            assertEnglish(Endgame.DRIFT_LINE)
            assertEnglish(Endgame.CHOICE_SLEEP)
            assertEnglish(Endgame.CHOICE_DRIFT)
            assertEnglish("タップで続ける")
            assertEnglish("タップで記録を閉じる")
        } finally { Lang.en = false }
    }

    @Test fun `the wave calls on the banner speak English`() {
        Lang.en = true
        try {
            // BOUNTY carries a proper name (kept in its own script) — the frame is tested composed
            for (e in WaveEvent.entries) if (e != WaveEvent.BOUNTY && e != WaveEvent.NONE) {
                WaveEvents.announceFor(e, null)?.let(::assertEnglish)
            }
            assertEnglish("⚠ 強大な気配が近づく　大群が接近している") // the boss line joins the event line
            assertEnglish("強敵の気配")
            assertEnglish("漂流者を救助した — 礼にと星屑40を分けてくれた")
        } finally { Lang.en = false }
    }
}
