package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.config.FamilyRole
import io.github.panda17tk.arpg.config.GameConfig
import io.github.panda17tk.arpg.config.LifeKind
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Health
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** v2.147 巨体の手応え: broadphase margins cover whale-class hulls — no dead zone at the flank. */
class GiantHitboxTest {
    private val dt = 1f / 60f

    @Test fun `the broadphase margin covers the widest hull in the roster`() {
        val defs = GameConfig().enemies.values
        val maxHalf = defs.maxOf { maxOf(it.w, it.h) } / 2f
        assertTrue(maxHalf <= Tuning.MAX_BODY_HALF, "MAX_BODY_HALF must cover $maxHalf")
    }

    @Test fun `a bullet lands on the whale's flank, not only its centre`() {
        val gw = WorldFactory.create(InputState(), seed = 7L)
        val whaleDef = GameConfig().enemies.getValue("isle_whale")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val whale = MobFactory.spawn(gw.world, whaleDef, px + 200f, py)
        val hp0 = with(gw.world) { whale[Health].hp }
        // 55px off-centre: inside the 60px half-width, outside the old 24px query margin
        gw.world.entity {
            it += Transform(x = px + 200f - 55f, y = py, prevX = px + 200f - 55f, prevY = py)
            it += Bullet(vx = 0f, vy = 0f, life = 0.5f, dmg = 10f)
        }
        repeat(3) { gw.world.update(dt) }
        assertTrue(with(gw.world) { whale[Health].hp } < hp0, "the flank shot lands")
    }

    @Test fun `a grenade's fuse feels the tyrant's hull, not just its centre`() {
        val gw = WorldFactory.create(InputState(), seed = 7L)
        val tyrantDef = GameConfig().enemies.getValue("tyrant_shark")
        val (px, py) = with(gw.world) { gw.player[Transform].let { it.x to it.y } }
        val shark = MobFactory.spawn(gw.world, tyrantDef, px + 200f, py + 120f)
        val hp0 = with(gw.world) { shark[Health].hp }
        val half = (tyrantDef.w + tyrantDef.h) / 4f
        val d = Tuning.TILE + half - 4f // inside the new hull-aware fuse ring, outside the old 32px one
        assertTrue(d > Tuning.TILE + 4f, "the test point sits beyond the old centre-only ring")
        gw.world.entity {
            it += Transform(x = px + 200f - d, y = py + 120f, prevX = px + 200f - d, prevY = py + 120f)
            it += Grenade(vx = 0f, vy = 0f, fuse = 30f)
        }
        repeat(3) { gw.world.update(dt) }
        assertTrue(with(gw.world) { shark[Health].hp } < hp0, "the proximity fuse trips on the hull")
    }

    @Test fun `every non-normal tier keeps a live spawn path and the book never demands the child`() {
        val defs = GameConfig().enemies
        // mirrors DesyncSurgeSystem's midboss/boss pool predicate — a def that drifts out of it vanishes
        val surgeable = defs.filterValues { it.biome == null && it.lifeKind != LifeKind.WILDLIFE }
        for ((id, d) in defs) {
            when (d.tier) {
                // biome != null = a surface lord / vault keeper (SurfaceEcology.lordFor); null = the surge pools
                "midboss" -> assertTrue(d.biome != null || id in surgeable, "midboss $id keeps a spawn path")
                "boss" -> assertTrue(d.biome != null || id in surgeable, "boss $id keeps a spawn path")
                "rogue" -> assertEquals("rogue_drifter", id, "the rogue tier is the two drifters")
            }
        }
        // the vault picks its keeper with firstOrNull — a second midboss per biome would be shadowed forever
        val byBiome = defs.values.filter { it.tier == "midboss" && it.biome != null }.groupBy { it.biome }
        for ((b, list) in byBiome) assertEquals(1, list.size, "biome $b keeps exactly one vault keeper")
        // v2.147: BESTIARY_FULL's denominator — the one CHILD (獣の子) stays out of the demand
        assertEquals(defs.size - 1, defs.values.count { it.familyRole != FamilyRole.CHILD })
    }
}
