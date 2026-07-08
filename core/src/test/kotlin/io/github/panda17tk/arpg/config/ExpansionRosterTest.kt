package io.github.panda17tk.arpg.config

import io.github.panda17tk.arpg.sim.SocietySpeechLines
import io.github.panda17tk.arpg.sim.SpeechLines
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.82 大増員: 50 new species and 100 new lines — counted, wired, and in the right pools. */
class ExpansionRosterTest {
    private val enemies = GameConfig().enemies

    @Test fun `the roster grew by fifty, to 123 species — plus the rogue`() {
        assertEquals(127, enemies.size, "73 old + 50 new (v2.82) + rogue_drifter (v2.83) + 3 void fish (v2.130)")
        assertEquals("rogue", enemies.getValue("rogue_drifter").tier, "the rogue stays out of every pool")
    }

    @Test fun `the new space kin sit at the surge pool's tail — old waves stay byte-identical`() {
        val normals = enemies.filterValues { it.tier == "normal" && it.biome == null }.keys.toList()
        // the first 17 keys are the historic pool, in their historic order
        assertEquals("zombie", normals.first())
        assertTrue(normals.indexOf("drift_leech") > normals.indexOf("quarantine"), "new normals must follow the old pool")
        val mids = enemies.filterValues { it.tier == "midboss" && it.biome == null }.keys.toList()
        assertTrue(mids.size >= 10, "4 old + 6 new midbosses (got ${mids.size})")
        assertTrue(mids.indexOf("archivist") > mids.indexOf("brute"), "new midbosses rotate in after the old")
    }

    @Test fun `the new species really vary — size, speed, smarts and voice`() {
        val newKeys = listOf(
            "drift_leech", "glass_mite", "relay_husk", "cinder_drone", "archive_moth", "null_hound",
            "patch_crab", "echo_wisp", "ballast_golem", "sweeper_scarab", "cipher_ray", "seal_moth",
            "tally_keeper", "grief_shell", "vector_imp",
            "archivist", "tide_warden", "hollow_knight", "chorus_node", "rust_titan", "lantern_bearer",
            "thorn_sentinel", "seed_keeper", "canopy_archer", "forge_priest", "slag_brute", "ember_dancer",
            "glacier_monk", "shard_lancer", "aurora_seer", "wind_cantor", "storm_herald", "pressure_warden",
            "bone_scribe", "grave_keeper", "ash_pilgrim", "waymark_tender", "echo_hermit", "stray_knight",
            "river_otter", "thorn_tortoise", "bramble_lynx", "ashwing", "snow_owl", "rime_elk",
            "rime_wolf", "sky_grazer", "crypt_beetle", "tomb_stalker", "dust_skipper",
        )
        assertEquals(50, newKeys.size)
        for (k in newKeys) assertTrue(k in enemies, "missing new species: $k")
        val defs = newKeys.map { enemies.getValue(it) }
        assertTrue(defs.minOf { it.hp } < 25f && defs.maxOf { it.hp } > 800f, "hp must span mites to titans")
        assertTrue(defs.minOf { it.speed } < 30f && defs.maxOf { it.speed } > 130f, "speed must span")
        assertTrue(defs.count { it.intelligence >= 0.7f } >= 8, "plenty of clever ones")
        assertTrue(defs.count { it.canSpeak } >= 20, "the societies talk")
        val styles = defs.map { it.speechStyle }.filter { it.isNotBlank() }.toSet()
        assertEquals(setOf("mechanical", "savage", "archaic", "polite"), styles, "all four registers in use")
        // every attack references only types the AI actually interprets
        val known = setOf(
            "melee", "lunge", "shot", "blink", "charge_melee", "burst", "spray", "spiral", "nova",
            "mine", "homing", "slam", "shockwave", "summon", "heal", "guard", "enrage", "barrage",
            "charge", "twin_shot",
            "ring_gap", "cutoff_volley", "page_wall", // v2.94 固有ボス技
        )
        for (k in newKeys) for (a in enemies.getValue(k).attacks) {
            assertTrue(a.type in known, "$k uses unknown attack type ${a.type}")
        }
        // summoned minions must exist
        for (k in newKeys) for (a in enemies.getValue(k).attacks.filter { it.type == "summon" }) {
            assertTrue(a.minion in enemies, "$k summons unknown minion ${a.minion}")
        }
    }

    @Test fun `the game gained at least a hundred new lines`() {
        // v2.82 以前: SpeechLines 47 + SocietySpeechLines 34 = 81 lines in total.
        val total = SpeechLines.lineCount() + SocietySpeechLines.lineCount()
        assertTrue(total >= 181, "expected 81 + 100 new lines, got $total")
    }

    @Test fun `each register speaks with its own voice, and plain speakers still speak`() {
        val salt = 0
        val styles = listOf("mechanical", "savage", "archaic", "polite")
        val voices = styles.map { SpeechLines.pick(SpeechLines.Trigger.Warn, salt, it) }
        assertEquals(voices.size, voices.toSet().size, "the four registers must not sound alike at salt 0")
        for (s in styles) for (trig in SpeechLines.Trigger.entries) {
            // styled speakers always have a line for every trigger (styled + plain fallback)
            assertTrue(SpeechLines.pick(trig, 3, s) != null, "$s went mute on $trig")
        }
        assertTrue(SpeechLines.pick(SpeechLines.Trigger.Warn, salt) != null, "plain voice intact")
        assertTrue(SpeechLines.pick(SpeechLines.Trigger.Warn, salt, "no_such_style") != null, "unknown style falls back")
    }
}
