# Phase 3 — Combat: Weapons, Projectiles, Melee, Ammo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** The player can fight: 5 weapons (Pistol/Shotgun/MG/Beam/Grenade) with finite ammo and reload, melee swings with stamina tiers, and projectiles + melee + explosions that destroy walls.

**Architecture:** Combat math is pure, unit-tested functions (`combat/*`: spread angles, beam ray, reload, melee tiers, ballistic step, explosion falloff). Projectiles are Fleks entities (`Bullet`, `Grenade`) updated by thin systems. **There are no enemies yet (Phase 5)** — bullets/melee/grenades affect only WALLS for now; the `hurtMob` hooks are marked where they will be added. **FX (muzzle/beam/slash/blast visuals) are Phase 7** — Phase 3 uses minimal rendering.

**Tech Stack:** Kotlin 2.0.21, libGDX 1.13.1, Fleks 2.8, JUnit 5. Builds on Phase 2 (TileMap, Collision, Tiles.damageTile, FlowField, ECS, loop).

---

## Source-of-truth values (legacy `legacy-web-v1.1.1`, `config.js`/`combat.js`/`projectiles.js`/`melee.js`)

**Weapons** (`CONFIG.weapons`): `bulletSpeed=360`, `grenadeSpeed=280`, `grenadeFuse=1.0`, bullet `life=0.9`.

| id | name | dmg | fireRate | magSize | spread | pellets | ammoType |
|---|---|---|---|---|---|---|---|
| pistol | Pistol | 24 | 0.22 | 12 | 0.05 | 1 | ammo9 |
| shotgun | Shotgun | 16 | 0.60 | 6 | 0.25 | 6 | ammo12 |
| mg | MG | 12 | 0.08 | 40 | 0.12 | 1 | ammo9 |
| beam | Beam | 80 | 0.60 | null (no mag) | 0 | 1 | ammoBeam |
| grenade | Grenade | 0 | 0.90 | 1 | 0 | 1 | ammoNade |

**Melee:** `meleeDmg=22`, `meleeReach=51`, `meleeCD=0.32`, `meleeKB=240`, `meleeSlashDmg=8`, `meleeStaWeakBelow=0.40`, `meleeStaSwordMin=0.20`, `meleeWeakMul=0.6`, `fistDmg=8`, arc=π (180°), front 3×3 wall break at offset 22px. Stamina tiers: ratio ≤0.20 → fist (dmg 8, no slash); 0.20<ratio<0.40 → weak sword (dmg 22×0.6, slash 8×0.6); ≥0.40 → full sword (dmg 22, slash 8).
**Explosion:** `explodeRadius=70`, `explodeDmg=110`, `explodeSelfDmg=25`; wall damage = `120*(1-d/r)` for `#` tiles within r (border `D`/∞ unaffected). Self damage uses falloff within `r*0.7`.
**Reload/ammo:** `autoReloadDelay=0.8`; reload pulls `min(magSize-mag, reserve[ammoType])`. Beam has no mag → consumes `ammoBeam` directly, no reload. Initial reserves (legacy `inv`): `ammo9=96, ammo12=24, ammoBeam=6, ammoNade=3`. Bullet vs wall: `damageTile(b.dmg)` then despawn. Beam: ray-march step 6, maxL 700, stops at solid tile.

## File structure (this phase)

```
core/src/main/kotlin/io/github/panda17tk/arpg/
├─ sim/Tuning.kt                     # MODIFY: combat constants
├─ combat/WeaponDef.kt               # weapon data class
├─ combat/Weapons.kt                 # the 5 weapon defs
├─ combat/Firing.kt                  # pure: bulletAngles(aim, spread, pellets, rng)
├─ combat/BeamRay.kt                 # pure: cast(map, x, y, dx, dy) -> end + reach
├─ combat/Reload.kt                  # pure: reload(magSize, mag, reserve)
├─ combat/MeleeResolve.kt            # pure: resolve(staRatio) -> dmg/slash/isFist
├─ combat/Ballistics.kt             # pure: stepBullet(map, x,y,vx,vy,life,dt)
├─ combat/Explosion.kt               # pure falloff + apply wall damage in radius
├─ ecs/components/{Arsenal,Ammo,Cooldowns,Bullet,Grenade}.kt
├─ ecs/systems/{WeaponSwitchSystem,FireSystem,ReloadSystem,MeleeSystem,ProjectileSystem}.kt
├─ ecs/world/WorldFactory.kt        # MODIFY: arsenal/ammo/cooldowns + new systems
├─ input/InputState.kt              # MODIFY: fire/melee/reload/weaponSlot
├─ input/KeyboardInput.kt           # MODIFY: poll K/J/R/1-5
└─ screens/GameScreen.kt            # MODIFY: render bullets/grenades + weapon HUD
core/src/test/kotlin/io/github/panda17tk/arpg/
├─ combat/{FiringTest,BeamRayTest,ReloadTest,MeleeResolveTest,BallisticsTest,ExplosionTest}.kt
└─ ecs/world/WorldCombatTest.kt
```

> **Fleks 2.8 notes:** spawning entities inside a system (`world.entity { it += ... }`) and removing the current entity (`world -= entity`) are used in Fire/Projectile systems. If those exact calls don't resolve, check the Fleks wiki and adjust only the systems (e.g. `entity.remove()` / `world.remove(entity)`). Keep pure `combat/*` logic untouched.

---

## Task 1: Tuning constants + WeaponDef + Weapons

**Files:** MODIFY `sim/Tuning.kt`; Create `combat/WeaponDef.kt`, `combat/Weapons.kt`.

- [ ] **Step 1: Append to the `Tuning` object** (`sim/Tuning.kt`, inside the object after the existing constants)
```kotlin
    // --- Combat (legacy CONFIG.player / weapons) ---
    const val BULLET_SPEED = 360f
    const val BULLET_LIFE = 0.9f
    const val MUZZLE_OFFSET = 14f
    const val GRENADE_SPEED = 280f
    const val GRENADE_FUSE = 1.0f
    const val AUTO_RELOAD_DELAY = 0.8f
    const val MELEE_DMG = 22f
    const val MELEE_REACH = 51f
    const val MELEE_CD = 0.32f
    const val MELEE_SLASH_DMG = 8f
    const val MELEE_STA_WEAK_BELOW = 0.40f
    const val MELEE_STA_SWORD_MIN = 0.20f
    const val MELEE_WEAK_MUL = 0.6f
    const val FIST_DMG = 8f
    const val MELEE_WALL_OFFSET = 22f
    const val EXPLODE_RADIUS = 70f
    const val EXPLODE_DMG = 110f
    const val EXPLODE_SELF_DMG = 25f
    const val EXPLODE_WALL_DMG = 120f
    const val START_AMMO9 = 96
    const val START_AMMO12 = 24
    const val START_AMMO_BEAM = 6
    const val START_AMMO_NADE = 3
```

- [ ] **Step 2: Create `combat/WeaponDef.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

/** Static weapon stats. magSize null = no magazine (Beam consumes reserve directly). */
data class WeaponDef(
    val id: String,
    val name: String,
    val dmg: Float,
    val fireRate: Float,
    val magSize: Int?,
    val spread: Float,
    val pellets: Int,
    val ammoType: String,
)
```

- [ ] **Step 3: Create `combat/Weapons.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

object Weapons {
    val ALL: List<WeaponDef> = listOf(
        WeaponDef("pistol", "Pistol", 24f, 0.22f, 12, 0.05f, 1, "ammo9"),
        WeaponDef("shotgun", "Shotgun", 16f, 0.60f, 6, 0.25f, 6, "ammo12"),
        WeaponDef("mg", "MG", 12f, 0.08f, 40, 0.12f, 1, "ammo9"),
        WeaponDef("beam", "Beam", 80f, 0.60f, null, 0f, 1, "ammoBeam"),
        WeaponDef("grenade", "Grenade", 0f, 0.90f, 1, 0f, 1, "ammoNade"),
    )
}
```

- [ ] **Step 4: Compile check + commit**
Run: `cd "V:/src/demo0902" && ./gradlew :core:compileKotlin`
```bash
git add core/src/main/kotlin/io/github/panda17tk/arpg/sim/Tuning.kt core/src/main/kotlin/io/github/panda17tk/arpg/combat/WeaponDef.kt core/src/main/kotlin/io/github/panda17tk/arpg/combat/Weapons.kt
git commit -m "feat(core): add combat tuning constants and weapon definitions"
```

---

## Task 2: Pure ranged logic — Firing, BeamRay, Reload (TDD)

**Files:** Create `combat/Firing.kt`, `combat/BeamRay.kt`, `combat/Reload.kt`; Tests `combat/FiringTest.kt`, `combat/BeamRayTest.kt`, `combat/ReloadTest.kt`.

- [ ] **Step 1: Write failing tests**

`combat/FiringTest.kt`:
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.math.Rng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class FiringTest {
    @Test fun `single pellet with zero spread fires exactly at the aim angle`() {
        val angles = Firing.bulletAngles(aim = 1.0f, spread = 0f, pellets = 1, rng = Rng(1))
        assertEquals(1, angles.size); assertEquals(1.0f, angles[0], 1e-5f)
    }
    @Test fun `pellet count matches weapon pellets`() {
        val angles = Firing.bulletAngles(aim = 0f, spread = 0.25f, pellets = 6, rng = Rng(1))
        assertEquals(6, angles.size)
    }
    @Test fun `spread keeps angles within plus or minus spread of the aim`() {
        val angles = Firing.bulletAngles(aim = 0f, spread = 0.25f, pellets = 50, rng = Rng(7))
        angles.forEach { assertTrue(abs(it) <= 0.25f + 1e-4f, "angle $it outside spread") }
    }
}
```

`combat/BeamRayTest.kt`:
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BeamRayTest {
    @Test fun `beam stops at the first solid tile`() {
        // open row 1 from x=16; wall column at tile x=4 (world 128..160)
        val m = TileMap.fromRows(listOf(".....#....", ".....#....", ".....#...."))
        val hit = BeamRay.cast(m, x = 16f, y = 48f, dirX = 1f, dirY = 0f, maxLen = 700f)
        assertTrue(hit.reach in 100f..130f, "reach ${hit.reach} should be near the wall at x=160 (tile 5)")
        assertTrue(hit.endX < 160f, "beam end ${hit.endX} must be before the wall face")
    }
}
```

`combat/ReloadTest.kt`:
```kotlin
package io.github.panda17tk.arpg.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReloadTest {
    @Test fun `reload tops up the magazine from the reserve`() {
        val r = Reload.reload(magSize = 12, mag = 3, reserve = 100)
        assertEquals(12, r.newMag); assertEquals(91, r.newReserve); assertEquals(9, r.taken)
    }
    @Test fun `reload is limited by the reserve`() {
        val r = Reload.reload(magSize = 12, mag = 3, reserve = 4)
        assertEquals(7, r.newMag); assertEquals(0, r.newReserve); assertEquals(4, r.taken)
    }
    @Test fun `full magazine takes nothing`() {
        val r = Reload.reload(magSize = 12, mag = 12, reserve = 100)
        assertEquals(12, r.newMag); assertEquals(100, r.newReserve); assertEquals(0, r.taken)
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.combat.FiringTest" --tests "io.github.panda17tk.arpg.combat.BeamRayTest" --tests "io.github.panda17tk.arpg.combat.ReloadTest"`

- [ ] **Step 3: Implement `combat/Firing.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.math.Rng

/** Pure spread resolver (legacy combat.js shooting). Each pellet jitters within ±spread of aim. */
object Firing {
    fun bulletAngles(aim: Float, spread: Float, pellets: Int, rng: Rng): FloatArray =
        FloatArray(pellets) { aim + (rng.nextFloat() - 0.5f) * spread * 2f }
}
```

- [ ] **Step 4: Implement `combat/BeamRay.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

data class BeamHit(val endX: Float, val endY: Float, val reach: Float)

/** Ray-march to the first solid tile (legacy beam). step=6, walks until a wall or maxLen. */
object BeamRay {
    private const val STEP = 6f
    fun cast(map: TileMap, x: Float, y: Float, dirX: Float, dirY: Float, maxLen: Float): BeamHit {
        var cx = x; var cy = y; var ex = x; var ey = y; var reach = maxLen
        var t = 0f
        while (t < maxLen) {
            cx += dirX * STEP; cy += dirY * STEP
            val tx = floor(cx / Tuning.TILE).toInt(); val ty = floor(cy / Tuning.TILE).toInt()
            if (map.solidAt(tx, ty)) { reach = t; break }
            ex = cx; ey = cy
            t += STEP
        }
        return BeamHit(ex, ey, reach)
    }
}
```

- [ ] **Step 5: Implement `combat/Reload.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

data class ReloadResult(val newMag: Int, val newReserve: Int, val taken: Int)

/** Pure reload math (legacy combat.js reload). Pulls min(need, reserve) from the reserve. */
object Reload {
    fun reload(magSize: Int, mag: Int, reserve: Int): ReloadResult {
        val need = magSize - mag
        if (need <= 0) return ReloadResult(mag, reserve, 0)
        val take = minOf(need, reserve)
        return ReloadResult(mag + take, reserve - take, take)
    }
}
```

- [ ] **Step 6: Run, verify PASS** (3 + 1 + 3 tests)
Run: same command as Step 2.

- [ ] **Step 7: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/combat/Firing.kt core/src/main/kotlin/io/github/panda17tk/arpg/combat/BeamRay.kt core/src/main/kotlin/io/github/panda17tk/arpg/combat/Reload.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/FiringTest.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/BeamRayTest.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/ReloadTest.kt && git commit -m "feat(core): pure firing spread, beam ray, and reload logic (TDD)"
```

---

## Task 3: Pure melee + ballistics + explosion logic (TDD)

**Files:** Create `combat/MeleeResolve.kt`, `combat/Ballistics.kt`, `combat/Explosion.kt`; Tests `combat/MeleeResolveTest.kt`, `combat/BallisticsTest.kt`, `combat/ExplosionTest.kt`.

- [ ] **Step 1: Write failing tests**

`combat/MeleeResolveTest.kt`:
```kotlin
package io.github.panda17tk.arpg.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeleeResolveTest {
    @Test fun `full stamina swings a full sword`() {
        val o = MeleeResolve.resolve(staRatio = 1.0f)
        assertEquals(22f, o.dmg, 1e-3f); assertEquals(8f, o.slashDmg, 1e-3f); assertFalse(o.isFist)
    }
    @Test fun `low-ish stamina is a weakened sword`() {
        val o = MeleeResolve.resolve(staRatio = 0.30f)
        assertEquals(22f * 0.6f, o.dmg, 1e-3f); assertEquals(8f * 0.6f, o.slashDmg, 1e-3f); assertFalse(o.isFist)
    }
    @Test fun `very low stamina is a fist with no slash`() {
        val o = MeleeResolve.resolve(staRatio = 0.10f)
        assertEquals(8f, o.dmg, 1e-3f); assertEquals(0f, o.slashDmg, 1e-3f); assertTrue(o.isFist)
    }
}
```

`combat/BallisticsTest.kt`:
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.TileMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BallisticsTest {
    @Test fun `bullet moves and survives over open floor`() {
        val m = TileMap.fromRows(listOf("..........", "..........", ".........."))
        val s = Ballistics.stepBullet(m, x = 16f, y = 48f, vx = 360f, vy = 0f, life = 0.9f, dt = 0.1f)
        assertEquals(52f, s.x, 1e-3f); assertNull(s.wallTile); assertFalse(s.expired)
    }
    @Test fun `bullet entering a wall reports the wall tile`() {
        val m = TileMap.fromRows(listOf("..#.......", "..#.......", "..#......."))
        val s = Ballistics.stepBullet(m, x = 60f, y = 48f, vx = 360f, vy = 0f, life = 0.9f, dt = 0.05f)
        assertNotNull(s.wallTile) // crossed into tile x=2 (world 64..96)
        assertEquals(2, s.wallTile!!.first)
    }
    @Test fun `bullet expires when life runs out`() {
        val m = TileMap.fromRows(listOf("..........", "..........", ".........."))
        val s = Ballistics.stepBullet(m, x = 16f, y = 48f, vx = 0f, vy = 0f, life = 0.05f, dt = 0.1f)
        assertTrue(s.expired)
    }
}
```

`combat/ExplosionTest.kt`:
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.MapLoader
import io.github.panda17tk.arpg.map.Stages
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.sim.Tuning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExplosionTest {
    @Test fun `falloff is full at center and zero at the radius edge`() {
        assertEquals(1f, Explosion.falloff(0f, 70f), 1e-4f)
        assertEquals(0f, Explosion.falloff(70f, 70f), 1e-4f)
        assertTrue(Explosion.falloff(35f, 70f) in 0.4f..0.6f)
    }
    @Test fun `explosion breaks a nearby destructible wall`() {
        val m = MapLoader.load(Stages.byId("arena1")).tileMap
        // find an internal destructible wall and explode on it with full damage
        var wx = -1; var wy = -1
        loop@ for (ty in 1 until m.height - 1) for (tx in 1 until m.width - 1) {
            if (m.tileAt(tx, ty) == Tile.WALL && m.hp[m.index(tx, ty)].isFinite()) { wx = tx; wy = ty; break@loop }
        }
        Explosion.applyWallDamage(m, wx * Tuning.TILE + 16f, wy * Tuning.TILE + 16f)
        assertEquals(Tile.FLOOR, m.tileAt(wx, wy)) // 120 dmg at center > 90 HP -> broken
    }
}
```

- [ ] **Step 2: Run, verify FAIL**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test --tests "io.github.panda17tk.arpg.combat.MeleeResolveTest" --tests "io.github.panda17tk.arpg.combat.BallisticsTest" --tests "io.github.panda17tk.arpg.combat.ExplosionTest"`

- [ ] **Step 3: Implement `combat/MeleeResolve.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.sim.Tuning

/** Resolved melee strength for a stamina ratio (legacy melee.js tiers). */
data class MeleeOutcome(val dmg: Float, val slashDmg: Float, val isFist: Boolean)

object MeleeResolve {
    fun resolve(staRatio: Float): MeleeOutcome = when {
        staRatio <= Tuning.MELEE_STA_SWORD_MIN -> MeleeOutcome(Tuning.FIST_DMG, 0f, true)
        staRatio < Tuning.MELEE_STA_WEAK_BELOW -> MeleeOutcome(
            Tuning.MELEE_DMG * Tuning.MELEE_WEAK_MUL, Tuning.MELEE_SLASH_DMG * Tuning.MELEE_WEAK_MUL, false,
        )
        else -> MeleeOutcome(Tuning.MELEE_DMG, Tuning.MELEE_SLASH_DMG, false)
    }
}
```

- [ ] **Step 4: Implement `combat/Ballistics.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/** Result of advancing a bullet one step. wallTile != null means it hit a wall (caller damages it). */
data class BulletStep(val x: Float, val y: Float, val life: Float, val wallTile: Pair<Int, Int>?, val expired: Boolean)

object Ballistics {
    fun stepBullet(map: TileMap, x: Float, y: Float, vx: Float, vy: Float, life: Float, dt: Float): BulletStep {
        val nx = x + vx * dt
        val ny = y + vy * dt
        val nLife = life - dt
        val tx = floor(nx / Tuning.TILE).toInt()
        val ty = floor(ny / Tuning.TILE).toInt()
        if (map.solidAt(tx, ty)) return BulletStep(nx, ny, nLife, tx to ty, true)
        return BulletStep(nx, ny, nLife, null, nLife <= 0f)
    }
}
```

- [ ] **Step 5: Implement `combat/Explosion.kt`**
```kotlin
package io.github.panda17tk.arpg.combat

import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor
import kotlin.math.hypot

/** Explosion math + wall application (legacy projectiles.js explode). Mob/self damage handled by callers. */
object Explosion {
    /** Linear falloff: 1 at center, 0 at [radius]. */
    fun falloff(dist: Float, radius: Float): Float = (1f - dist / radius).coerceIn(0f, 1f)

    /** Damage destructible WALL tiles within EXPLODE_RADIUS of (x,y) with 120*(1-d/r) (legacy). */
    fun applyWallDamage(map: TileMap, x: Float, y: Float) {
        val r = Tuning.EXPLODE_RADIUS
        val tx0 = maxOf(1, floor((x - r) / Tuning.TILE).toInt())
        val ty0 = maxOf(1, floor((y - r) / Tuning.TILE).toInt())
        val tx1 = minOf(map.width - 2, floor((x + r) / Tuning.TILE).toInt())
        val ty1 = minOf(map.height - 2, floor((y + r) / Tuning.TILE).toInt())
        for (ty in ty0..ty1) for (tx in tx0..tx1) {
            if (map.tileAt(tx, ty) != Tile.WALL) continue
            val cx = tx * Tuning.TILE + Tuning.TILE / 2f
            val cy = ty * Tuning.TILE + Tuning.TILE / 2f
            val d = hypot(cx - x, cy - y)
            if (d <= r) Tiles.damageTile(map, tx, ty, Tuning.EXPLODE_WALL_DMG * (1f - d / r))
        }
    }
}
```

- [ ] **Step 6: Run, verify PASS** (3 + 3 + 2 tests)
Run: same command as Step 2.

- [ ] **Step 7: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/combat/MeleeResolve.kt core/src/main/kotlin/io/github/panda17tk/arpg/combat/Ballistics.kt core/src/main/kotlin/io/github/panda17tk/arpg/combat/Explosion.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/MeleeResolveTest.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/BallisticsTest.kt core/src/test/kotlin/io/github/panda17tk/arpg/combat/ExplosionTest.kt && git commit -m "feat(core): pure melee tiers, ballistics step, and explosion logic (TDD)"
```

---

## Task 4: Combat ECS components

**Files:** Create `ecs/components/Arsenal.kt`, `Ammo.kt`, `Cooldowns.kt`, `Bullet.kt`, `Grenade.kt`.

- [ ] **Step 1: `ecs/components/Arsenal.kt`** (weapons + current index + per-weapon mag/auto-reload timer)
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.combat.WeaponDef

/** Runtime weapon: its definition, current magazine, and time since last shot (auto-reload). */
class WeaponRuntime(val def: WeaponDef, var mag: Int, var autoReloadTimer: Float = 0f)

class Arsenal(val weapons: List<WeaponRuntime>, var curW: Int = 0) : Component<Arsenal> {
    val current: WeaponRuntime get() = weapons[curW]
    override fun type() = Arsenal
    companion object : ComponentType<Arsenal>()
}
```

- [ ] **Step 2: `ecs/components/Ammo.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import io.github.panda17tk.arpg.sim.Tuning

/** Reserve ammo by pool (legacy player.inv). */
class Ammo(
    var ammo9: Int = Tuning.START_AMMO9,
    var ammo12: Int = Tuning.START_AMMO12,
    var ammoBeam: Int = Tuning.START_AMMO_BEAM,
    var ammoNade: Int = Tuning.START_AMMO_NADE,
) : Component<Ammo> {
    fun get(pool: String): Int = when (pool) {
        "ammo9" -> ammo9; "ammo12" -> ammo12; "ammoBeam" -> ammoBeam; "ammoNade" -> ammoNade; else -> 0
    }
    fun set(pool: String, value: Int) { when (pool) {
        "ammo9" -> ammo9 = value; "ammo12" -> ammo12 = value; "ammoBeam" -> ammoBeam = value; "ammoNade" -> ammoNade = value
    } }
    override fun type() = Ammo
    companion object : ComponentType<Ammo>()
}
```

- [ ] **Step 3: `ecs/components/Cooldowns.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Per-player action cooldowns (seconds remaining). */
class Cooldowns(var shoot: Float = 0f, var melee: Float = 0f) : Component<Cooldowns> {
    override fun type() = Cooldowns
    companion object : ComponentType<Cooldowns>()
}
```

- [ ] **Step 4: `ecs/components/Bullet.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Player bullet: velocity, remaining life (s), damage. (Mob hits wire in Phase 5.) */
class Bullet(var vx: Float, var vy: Float, var life: Float, var dmg: Float) : Component<Bullet> {
    override fun type() = Bullet
    companion object : ComponentType<Bullet>()
}
```

- [ ] **Step 5: `ecs/components/Grenade.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/** Thrown grenade: velocity + fuse (s). Explodes on solid tile or fuse end. */
class Grenade(var vx: Float, var vy: Float, var fuse: Float) : Component<Grenade> {
    override fun type() = Grenade
    companion object : ComponentType<Grenade>()
}
```

- [ ] **Step 6: Compile + commit**
Run: `cd "V:/src/demo0902" && ./gradlew :core:compileKotlin`
```bash
git add core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/Arsenal.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/Ammo.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/Cooldowns.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/Bullet.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/components/Grenade.kt
git commit -m "feat(core): combat ECS components (Arsenal/Ammo/Cooldowns/Bullet/Grenade)"
```

---

## Task 5: Input + combat systems (Fire/Reload/Melee/WeaponSwitch)

**Files:** MODIFY `input/InputState.kt`, `input/KeyboardInput.kt`; Create `ecs/systems/WeaponSwitchSystem.kt`, `FireSystem.kt`, `ReloadSystem.kt`, `MeleeSystem.kt`.

- [ ] **Step 1: MODIFY `input/InputState.kt`** — add combat inputs (after `placeWall`)
```kotlin
    var placeWall = false
    var fire = false           // held (K)
    var melee = false          // edge (J)
    var reload = false         // edge (R)
    var weaponSlot = -1        // 0..4 on the frame 1-5 is pressed, else -1
```

- [ ] **Step 2: MODIFY `input/KeyboardInput.kt`** — poll K (held) / J,R edges / number keys. Add prev-state flags alongside `prevF`:
```kotlin
    private var prevF = false
    private var prevJ = false
    private var prevR = false
```
and in `poll`, after the existing F handling:
```kotlin
        state.fire = k.isKeyPressed(Keys.K)
        val j = k.isKeyPressed(Keys.J); state.melee = j && !prevJ; prevJ = j
        val r = k.isKeyPressed(Keys.R); state.reload = r && !prevR; prevR = r
        state.weaponSlot = when {
            k.isKeyPressed(Keys.NUM_1) -> 0
            k.isKeyPressed(Keys.NUM_2) -> 1
            k.isKeyPressed(Keys.NUM_3) -> 2
            k.isKeyPressed(Keys.NUM_4) -> 3
            k.isKeyPressed(Keys.NUM_5) -> 4
            else -> -1
        }
```

- [ ] **Step 3: `ecs/systems/WeaponSwitchSystem.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.input.InputState

class WeaponSwitchSystem : IteratingSystem(family { all(PlayerTag, Arsenal) }) {
    private val input: InputState = world.inject()
    override fun onTickEntity(entity: Entity) {
        val slot = input.weaponSlot
        if (slot < 0) return
        val arsenal = entity[Arsenal]
        if (slot < arsenal.weapons.size) arsenal.curW = slot
    }
}
```

- [ ] **Step 4: `ecs/systems/FireSystem.kt`** (spawns bullet/grenade entities; beam ray + ammo; cooldown)
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.BeamRay
import io.github.panda17tk.arpg.combat.Firing
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.math.Rng
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class FireSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Arsenal, Ammo, Cooldowns) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()
    private val rng: Rng = world.inject()

    override fun onTickEntity(entity: Entity) {
        val cd = entity[Cooldowns]
        if (cd.shoot > 0f) cd.shoot -= deltaTime
        if (!input.fire || cd.shoot > 0f) return

        val t = entity[Transform]; val f = entity[Facing]
        val arsenal = entity[Arsenal]; val ammo = entity[Ammo]
        val w = arsenal.current; val def = w.def
        val aim = atan2(f.y, f.x)
        val dirX = cos(aim); val dirY = sin(aim)

        when (def.id) {
            "beam" -> {
                if (ammo.ammoBeam <= 0) return
                ammo.ammoBeam--
                cd.shoot = def.fireRate
                BeamRay.cast(map, t.x, t.y, dirX, dirY, 700f) // mob hits land here in Phase 5
            }
            "grenade" -> {
                if (w.mag <= 0) return
                w.mag--; cd.shoot = def.fireRate
                world.entity {
                    it += Transform(x = t.x + dirX * Tuning.MUZZLE_OFFSET, y = t.y + dirY * Tuning.MUZZLE_OFFSET)
                    it += Grenade(dirX * Tuning.GRENADE_SPEED, dirY * Tuning.GRENADE_SPEED, Tuning.GRENADE_FUSE)
                }
            }
            else -> {
                if (w.mag <= 0) return
                w.mag--; cd.shoot = def.fireRate
                val angles = Firing.bulletAngles(aim, def.spread, def.pellets, rng)
                for (a in angles) {
                    val vx = cos(a) * Tuning.BULLET_SPEED; val vy = sin(a) * Tuning.BULLET_SPEED
                    world.entity {
                        it += Transform(x = t.x + cos(a) * Tuning.MUZZLE_OFFSET, y = t.y + sin(a) * Tuning.MUZZLE_OFFSET)
                        it += Bullet(vx, vy, Tuning.BULLET_LIFE, def.dmg)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 5: `ecs/systems/ReloadSystem.kt`** (manual R + auto-reload after delay)
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.Reload
import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.sim.Tuning

class ReloadSystem : IteratingSystem(family { all(PlayerTag, Arsenal, Ammo) }) {
    private val input: InputState = world.inject()
    override fun onTickEntity(entity: Entity) {
        val arsenal = entity[Arsenal]; val ammo = entity[Ammo]
        val w = arsenal.current; val size = w.def.magSize ?: return // beam: no reload
        fun doReload() {
            val r = Reload.reload(size, w.mag, ammo.get(w.def.ammoType))
            w.mag = r.newMag; ammo.set(w.def.ammoType, r.newReserve); w.autoReloadTimer = 0f
        }
        if (input.reload) { doReload(); return }
        // auto-reload: when not firing and the mag isn't full, count up to the delay
        if (w.mag < size && !input.fire) {
            w.autoReloadTimer += deltaTime
            if (w.autoReloadTimer >= Tuning.AUTO_RELOAD_DELAY) doReload()
        } else {
            w.autoReloadTimer = 0f
        }
    }
}
```

- [ ] **Step 6: `ecs/systems/MeleeSystem.kt`** (swing on J: cooldown, stamina tiers, front 3×3 wall break)
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.MeleeResolve
import io.github.panda17tk.arpg.ecs.components.Cooldowns
import io.github.panda17tk.arpg.ecs.components.Facing
import io.github.panda17tk.arpg.ecs.components.PlayerTag
import io.github.panda17tk.arpg.ecs.components.Stamina
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.input.InputState
import io.github.panda17tk.arpg.map.Tile
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

class MeleeSystem : IteratingSystem(family { all(PlayerTag, Transform, Facing, Stamina, Cooldowns) }) {
    private val input: InputState = world.inject()
    private val map: TileMap = world.inject()

    override fun onTickEntity(entity: Entity) {
        val cd = entity[Cooldowns]
        if (cd.melee > 0f) cd.melee -= deltaTime
        if (!input.melee || cd.melee > 0f) return

        val t = entity[Transform]; val f = entity[Facing]; val s = entity[Stamina]
        cd.melee = Tuning.MELEE_CD
        val outcome = MeleeResolve.resolve(if (s.max > 0f) s.value / s.max else 1f) // mob hits: Phase 5

        // break destructible walls in the front 3x3 (legacy melee.js)
        val ftx = floor((t.x + f.x * Tuning.MELEE_WALL_OFFSET) / Tuning.TILE).toInt()
        val fty = floor((t.y + f.y * Tuning.MELEE_WALL_OFFSET) / Tuning.TILE).toInt()
        for (oy in -1..1) for (ox in -1..1) {
            val tx = ftx + ox; val ty = fty + oy
            if (map.tileAt(tx, ty) == Tile.WALL) Tiles.damageTile(map, tx, ty, outcome.dmg)
        }
    }
}
```

- [ ] **Step 7: Compile + commit** (systems wired into the world in Task 6)
Run: `cd "V:/src/demo0902" && ./gradlew :core:compileKotlin`
```bash
git add core/src/main/kotlin/io/github/panda17tk/arpg/input/InputState.kt core/src/main/kotlin/io/github/panda17tk/arpg/input/KeyboardInput.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/WeaponSwitchSystem.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/FireSystem.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/ReloadSystem.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/MeleeSystem.kt
git commit -m "feat(core): combat input + fire/reload/melee/switch systems"
```

---

## Task 6: ProjectileSystem + world wiring (TDD)

**Files:** Create `ecs/systems/ProjectileSystem.kt`; MODIFY `ecs/world/WorldFactory.kt`; Test `ecs/world/WorldCombatTest.kt`.

- [ ] **Step 1: `ecs/systems/ProjectileSystem.kt`** (advance bullets & grenades; wall break; explode)
```kotlin
package io.github.panda17tk.arpg.ecs.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World.Companion.family
import io.github.panda17tk.arpg.combat.Ballistics
import io.github.panda17tk.arpg.combat.Explosion
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.ecs.components.Grenade
import io.github.panda17tk.arpg.ecs.components.Transform
import io.github.panda17tk.arpg.map.TileMap
import io.github.panda17tk.arpg.map.Tiles
import io.github.panda17tk.arpg.pathfinding.FlowField
import io.github.panda17tk.arpg.sim.Tuning
import kotlin.math.floor

/** Advances player bullets and grenades; breaks walls; explodes grenades. Mob damage: Phase 5. */
class ProjectileSystem : IteratingSystem(family { any(Bullet, Grenade) }) {
    private val map: TileMap = world.inject()
    private val flow: FlowField = world.inject()

    override fun onTickEntity(entity: Entity) {
        val t = entity[Transform]
        if (entity has Bullet) {
            val b = entity[Bullet]
            val step = Ballistics.stepBullet(map, t.x, t.y, b.vx, b.vy, b.life, deltaTime)
            t.x = step.x; t.y = step.y; b.life = step.life
            val wall = step.wallTile
            if (wall != null) {
                val broke = Tiles.damageTile(map, wall.first, wall.second, b.dmg).broke
                if (broke) flow.rebuild(map, playerTileX(), playerTileY())
                world -= entity
                return
            }
            if (step.expired) world -= entity
        } else {
            val g = entity[Grenade]
            t.x += g.vx * deltaTime; t.y += g.vy * deltaTime; g.fuse -= deltaTime
            val tx = floor(t.x / Tuning.TILE).toInt(); val ty = floor(t.y / Tuning.TILE).toInt()
            if (map.solidAt(tx, ty) || g.fuse <= 0f) {
                Explosion.applyWallDamage(map, t.x, t.y) // player self-damage + mob damage: Phase 5
                flow.rebuild(map, playerTileX(), playerTileY())
                world -= entity
            }
        }
    }

    // The player's current tile, for flow-field rebuilds after terrain changes.
    private var pPlayerX = 0
    private var pPlayerY = 0
    private fun playerTileX(): Int = pPlayerX
    private fun playerTileY(): Int = pPlayerY

    override fun onTick() {
        // cache the player tile once per frame so wall-break rebuilds use it
        val players = world.family { all(io.github.panda17tk.arpg.ecs.components.PlayerTag, Transform) }
        players.forEach { e -> val t = e[Transform]; pPlayerX = floor(t.x / Tuning.TILE).toInt(); pPlayerY = floor(t.y / Tuning.TILE).toInt() }
        super.onTick()
    }
}
```
NOTE: if `entity has Bullet` / `world.family { }` / `world -= entity` don't resolve in Fleks 2.8, use the documented equivalents (e.g. `Bullet in entity`, `world.family { ... }.forEach`, `entity.remove()`); adjust only this system.

- [ ] **Step 2: MODIFY `ecs/world/WorldFactory.kt`** — inject `Rng`, add player combat components, register the combat systems in order. Add to the `injectables { }` block: `add(Rng(seed))` (a second Rng instance for combat spread — distinct from stage selection). Add to the player entity: `it += Arsenal(Weapons.ALL.map { d -> WeaponRuntime(d, d.magSize ?: 0) })`, `it += Ammo()`, `it += Cooldowns()`. Register systems after `BuildSystem` in this order:
```kotlin
                add(SnapshotSystem())
                add(MovementSystem())
                add(BuildSystem())
                add(WeaponSwitchSystem())
                add(MeleeSystem())
                add(FireSystem())
                add(ReloadSystem())
                add(ProjectileSystem())
```
Add the needed imports (`Arsenal`, `WeaponRuntime`, `Ammo`, `Cooldowns`, `Weapons`, the 5 systems, `Rng`). Keep the existing stage-selection `Rng(seed)` use; create a separate `Rng(seed xor 0x9E3779B9L)` for the combat injectable so spread and stage selection don't share a stream.

- [ ] **Step 3: Write `ecs/world/WorldCombatTest.kt`**
```kotlin
package io.github.panda17tk.arpg.ecs.world

import io.github.panda17tk.arpg.ecs.components.Ammo
import io.github.panda17tk.arpg.ecs.components.Arsenal
import io.github.panda17tk.arpg.ecs.components.Bullet
import io.github.panda17tk.arpg.input.InputState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldCombatTest {
    @Test fun `firing the pistol spawns a bullet and consumes a magazine round`() {
        val input = InputState().apply { fire = true }
        val gw = WorldFactory.create(input, seed = 1L)
        val magBefore = with(gw.world) { gw.player[Arsenal].current.mag }
        gw.world.update(1f / 60f)
        val magAfter = with(gw.world) { gw.player[Arsenal].current.mag }
        assertEquals(magBefore - 1, magAfter)
        val bullets = gw.world.family { all(Bullet) }.numEntities
        assertTrue(bullets >= 1, "expected a bullet entity, got $bullets")
    }

    @Test fun `reload refills the magazine from reserves`() {
        val input = InputState()
        val gw = WorldFactory.create(input, seed = 1L)
        // drain a few rounds
        input.fire = true
        repeat(3) { gw.world.update(0.3f) } // fireRate 0.22 -> 3 shots
        input.fire = false
        val magLow = with(gw.world) { gw.player[Arsenal].current.mag }
        input.reload = true
        gw.world.update(1f / 60f)
        input.reload = false
        val magFull = with(gw.world) { gw.player[Arsenal].current.mag }
        assertTrue(magFull > magLow, "reload should refill: $magLow -> $magFull")
    }
}
```

- [ ] **Step 4: Run, verify PASS + full suite**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test`
Expected: all pass (Phases 1–3). If a Fleks API symbol fails (entity spawn/remove/family-count `numEntities`), adjust the systems/test per the Fleks 2.8 wiki (thin wrappers only).

- [ ] **Step 5: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/ecs/systems/ProjectileSystem.kt core/src/main/kotlin/io/github/panda17tk/arpg/ecs/world/WorldFactory.kt core/src/test/kotlin/io/github/panda17tk/arpg/ecs/world/WorldCombatTest.kt && git commit -m "feat(core): projectile system (bullets/grenades + wall break) and world wiring (TDD)"
```

---

## Task 7: Render projectiles + weapon HUD (integration; build gate + manual)

**Files:** MODIFY `screens/GameScreen.kt`.

- [ ] **Step 1: MODIFY `screens/GameScreen.kt`** — after drawing the player (still inside the `ShapeType.Filled` block) draw bullets and grenades; update the HUD line with the weapon name and ammo. Add imports for `Bullet`, `Grenade`, `Arsenal`, `Ammo`. Inside the filled-shapes block, after the player circle:
```kotlin
        // bullets (yellow) + grenades (red)
        with(gw.world) {
            gw.world.family { all(Bullet, Transform) }.forEach { e ->
                val bt = e[Transform]; shapes.color = Color(1f, 0.95f, 0.5f, 1f); shapes.circle(bt.x, bt.y, 3f, 8)
            }
            gw.world.family { all(Grenade, Transform) }.forEach { e ->
                val gt = e[Transform]; shapes.color = Color(1f, 0.5f, 0.4f, 1f); shapes.circle(gt.x, gt.y, 5f, 10)
            }
        }
```
And replace the HUD `font.draw` line with one that includes the current weapon + ammo:
```kotlin
        val arsenal = with(gw.world) { gw.player[Arsenal] }
        val ammo = with(gw.world) { gw.player[Ammo] }
        val w = arsenal.current
        val magStr = w.def.magSize?.let { "${w.mag}/$it" } ?: "∞"
        val reserve = ammo.get(w.def.ammoType)
        font.draw(batch, "${w.def.name} $magStr (res $reserve)  STA ${sta.toInt()}  blk $blocks  [WASD/J/K/R/1-5/F]", 16f, 28f)
```
(Keep the stamina-bar block and camera/loop logic unchanged.)

- [ ] **Step 2: Build gate**
Run: `cd "V:/src/demo0902" && ./gradlew :core:test :desktop:build`
Expected: all tests pass, desktop compiles. Do NOT run `:desktop:run`.

- [ ] **Step 3: Android target still builds**
Run: `cd "V:/src/demo0902" && ./gradlew :android:assembleDebug`
Expected: BUILD SUCCESSFUL; APK produced.

- [ ] **Step 4: Commit**
```bash
cd "V:/src/demo0902" && git add core/src/main/kotlin/io/github/panda17tk/arpg/screens/GameScreen.kt && git commit -m "feat(core): render bullets/grenades and weapon/ammo HUD"
```

- [ ] **Step 5 (MANUAL — human): Visual verification**
Run: `cd "V:/src/demo0902" && ./gradlew :desktop:run`
Expected: **K** fires the current weapon (bullets fly, mag drops, auto-reloads after a pause); **1-5** switch weapons (Shotgun fires a spread, MG fires fast, Beam/Grenade work); bullets and melee (**J**) and grenades **destroy walls**; **R** reloads; the HUD shows weapon + ammo. Close to end.

---

## Self-Review

**1. Spec coverage (Phase 3 row):** weapons/projectiles/melee/combat-core → Tasks 1–6. Finite ammo → `Ammo` + mag in `Arsenal`. Reload → `Reload` + `ReloadSystem` (manual + auto). Wall break wired → bullets (`ProjectileSystem`), melee (`MeleeSystem` 3×3), grenade (`Explosion.applyWallDamage`). 5 weapons + switch → `Weapons` + `WeaponSwitchSystem`. ✓ Deferred by design (noted in code): mob damage / beam-mob / slash fan / enemy bullets → Phase 5; FX visuals → Phase 7.

**2. Placeholder scan:** No TBD/TODO. The MeleeSystem snippet intentionally flags a non-valid stray line with an explicit "delete this" note (a guardrail, not a placeholder). Manual run is an explicit human step. Fleks-API fallbacks are concrete. ✓

**3. Type consistency:** `WeaponDef`/`WeaponRuntime`/`Arsenal.current` consistent across Fire/Reload/Switch/HUD. `Ammo.get/set(pool)` consistent. `Cooldowns.shoot/melee` consistent. `Bullet(vx,vy,life,dmg)`/`Grenade(vx,vy,fuse)` consistent across spawn (FireSystem), update (ProjectileSystem), render (GameScreen). `Tuning.*` new constants used consistently. `Ballistics.stepBullet`/`Explosion.applyWallDamage`/`Reload.reload`/`MeleeResolve.resolve`/`Firing.bulletAngles`/`BeamRay.cast` signatures match their tests + callers. ✓

**Risks flagged:** (a) Fleks entity spawn-in-system / removal / `family().numEntities` / `entity has Comp` — isolated to FireSystem/ProjectileSystem/test with concrete fallbacks. (b) Only **one** `Rng` is injected (combat spread); stage selection in `WorldFactory.create` uses a **local** `Rng(seed)` that is NOT added to `injectables`, so `FireSystem`'s `world.inject()` for `Rng` is unambiguous — no named injectable needed. (c) Determinism: combat spread uses the injected `Rng` so tests are stable.

---

## Execution Handoff

**Plan complete.** Options: **Subagent-Driven (recommended)** — group as (Tasks 1–3 pure logic) / (Tasks 4–5 components+input+systems) / (Tasks 6–7 projectile+world+render); or **Inline**. After Phase 3, **Phase 4** (data/config: `GameConfig` + `ConfigStore`, move Tuning/Weapons into editable JSON) is next.
