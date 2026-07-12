package io.github.panda17tk.arpg.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RunSaveCodecTest {
    private fun sample() = RunSaveDto(
        mode = "SURFACE", biome = "ICE",
        spaceSeed = 1L, surfSeed = 103L, worldSeed = 103L,
        landedPlanetId = 42L, returnX = 12f, returnY = 34f,
        wave = 7, px = 640f, py = 320f,
        lootedWrecks = listOf(0, 2), survivorRescued = true, cometSwept = true, // v2.169
        upgradeSeed = 7L, // v2.174
        hp = 55f, hpMax = 120f, stamina = 80f,
        ammo9 = 60, ammo12 = 12, ammoBeam = 3, ammoNade = 2, blocks = 9, dust = 77, shards = 2,
        mags = listOf(12, 6, 40, 0, 1),
        gunMul = 1.2f, fireMul = 0.9f, meleeMul = 1.1f, moveMul = 1.08f,
        ammoMul = 1f, healOnKill = 2f, wallHp = 10f,
        loadout = mapOf("THRUSTER" to "thruster_oc", "RANGED" to "gun_mg"),
        backpack = listOf("gun_pistol", "acc_boots", "acc_boots"),
        curWeapon = 2,
        society = PlanetMemoryDto(childKilled = true, hostility = 0.8f),
    )

    @Test fun `round-trips through JSON`() {
        val back = RunSaveCodec.fromJson(RunSaveCodec.toJson(sample()))
        assertEquals(sample(), back)
    }

    @Test fun `broken JSON reads as null`() {
        assertNull(RunSaveCodec.fromJson("{"))
        assertNull(RunSaveCodec.fromJson("not json at all"))
    }

    @Test fun `unknown keys and missing fields are tolerated`() {
        val dto = RunSaveCodec.fromJson("""{"version":1,"mode":"SPACE","future_field":true}""")
        assertEquals("SPACE", dto?.mode)
        assertEquals(1, dto?.wave) // defaulted
        assertNull(dto?.society)
    }

    @Test fun `the in-memory store exercises the same codec path`() {
        val store = InMemoryRunSaveStore()
        assertNull(store.load())
        store.save(sample())
        assertEquals(sample(), store.load())
        store.clear()
        assertNull(store.load())
    }
}
